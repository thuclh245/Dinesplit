package com.example.dinesplit.domain.receipt

import com.example.dinesplit.domain.model.TransactionType
import java.text.Normalizer
import java.util.Locale
import kotlin.math.ln
import kotlin.math.roundToLong

object ReceiptOcrParser {
    private const val DONG_LETTER = "\u0111"
    private const val DONG_SIGN = "\u20ab"
    private const val DONG_LETTER_CHAR = '\u0111'
    private const val DONG_SIGN_CHAR = '\u20ab'

    private val amountPattern =
        Regex(
            pattern =
                "(?i)(?:vnd|vn$DONG_LETTER|$DONG_LETTER|d|$DONG_SIGN)?\\s*" +
                    "((?:\\d{1,3}(?:[.,\\s]\\d{3})+|\\d+)(?:[.,]\\d{1,2})?)\\s*" +
                    "(?:vnd|vn$DONG_LETTER|$DONG_LETTER|d|$DONG_SIGN)?",
        )

    private val totalKeywords =
        listOf(
            "grand total",
            "total amount",
            "amount due",
            "balance due",
            "tong cong",
            "tong tien",
            "thanh tien",
            "thanh toan",
            "can tra",
            "phai tra",
            "total",
        )

    private val nonTotalKeywords =
        listOf(
            "subtotal",
            "sub total",
            "tam tinh",
            "tax",
            "vat",
            "discount",
            "giam gia",
            "change",
            "tien thua",
            "cash",
            "card",
            "service charge",
            "shipping",
            "qty",
            "quantity",
            "unit price",
        )

    private val merchantNoiseKeywords =
        listOf(
            "receipt",
            "invoice",
            "bill",
            "hoa don",
            "total",
            "date",
            "time",
            "tel",
            "phone",
            "address",
            "mst",
        )

    private val itemLineNoiseKeywords =
        listOf(
            "receipt",
            "invoice",
            "bill",
            "hoa don",
            "ngay",
            "date",
            "time",
            "tel",
            "phone",
            "address",
            "mst",
            "ban",
            "table",
            "mon an",
            "sl",
            "thanh tien",
        )

    private val categoryBuckets =
        listOf(
            CategoryKeywordBucket(
                hints = setOf("food", "dining", "restaurant", "cafe", "coffee", "meal", "drink", "c_food"),
                keywords =
                    setOf(
                        "restaurant",
                        "cafe",
                        "coffee",
                        "tea",
                        "milk tea",
                        "pizza",
                        "burger",
                        "pho",
                        "banh",
                        "com",
                        "highlands",
                        "phuc long",
                        "starbucks",
                        "kfc",
                        "lotteria",
                        "jollibee",
                        "food",
                        "dining",
                    ),
            ),
            CategoryKeywordBucket(
                hints = setOf("grocery", "groceries", "market", "supermarket", "mart", "store", "c_grocery"),
                keywords =
                    setOf(
                        "grocery",
                        "groceries",
                        "supermarket",
                        "market",
                        "mini mart",
                        "mart",
                        "winmart",
                        "coop",
                        "co op",
                        "bach hoa",
                        "lotte mart",
                        "aeon",
                        "circle k",
                        "familymart",
                        "gs25",
                        "store",
                    ),
            ),
            CategoryKeywordBucket(
                hints = setOf("transit", "transport", "taxi", "ride", "bus", "train", "fuel", "parking", "c_transit"),
                keywords =
                    setOf(
                        "taxi",
                        "grab",
                        "gojek",
                        "be",
                        "xanh sm",
                        "bus",
                        "metro",
                        "train",
                        "parking",
                        "fuel",
                        "xang",
                        "toll",
                        "transport",
                        "transit",
                    ),
            ),
            CategoryKeywordBucket(
                hints = setOf("entertainment", "movie", "cinema", "ticket", "game", "event", "fun", "c_fun"),
                keywords =
                    setOf(
                        "cinema",
                        "movie",
                        "cgv",
                        "lotte cinema",
                        "bhd",
                        "galaxy",
                        "ticket",
                        "karaoke",
                        "event",
                        "game",
                        "netflix",
                        "spotify",
                        "entertainment",
                    ),
            ),
        )

    fun parse(
        rawText: String,
        categories: List<ReceiptCategoryOption>,
    ): ReceiptOcrResult {
        val lines =
            rawText
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toList()

        val amount = extractTotalAmount(lines)

        return ReceiptOcrResult(
            rawText = rawText,
            amount = amount,
            category = inferCategory(rawText, categories),
            merchantName = inferMerchantName(lines),
            items = extractLineItems(lines, amount),
        )
    }

    private fun extractTotalAmount(lines: List<String>): Double? {
        val candidates =
            lines.flatMapIndexed { index, line ->
                amountPattern.findAll(line).mapNotNull { match ->
                    val token = match.groupValues[1]
                    val value = token.parseMoneyToken() ?: return@mapNotNull null
                    val normalizedLine = line.searchable()
                    val hasCurrency = match.value.hasCurrencyMarker()
                    val isTotalLine = totalKeywords.any { normalizedLine.contains(it) }
                    val isNonTotalLine = nonTotalKeywords.any { normalizedLine.contains(it) }

                    if (value <= 0.0 || value > 1_000_000_000.0) return@mapNotNull null
                    if (value < 1_000.0 && !hasCurrency && !isTotalLine) return@mapNotNull null

                    val keywordScore =
                        when {
                            isTotalLine -> 100.0
                            isNonTotalLine -> -60.0
                            else -> 0.0
                        }
                    val currencyScore = if (hasCurrency) 12.0 else 0.0
                    val positionScore = index.toDouble() / 10.0
                    val valueScore = ln(value.coerceAtLeast(1.0)) / 10.0

                    AmountCandidate(
                        value = value,
                        score = keywordScore + currencyScore + positionScore + valueScore,
                    )
                }.toList()
            }

        return candidates
            .maxWithOrNull(compareBy<AmountCandidate> { it.score }.thenBy { it.value })
            ?.value
    }

    private fun extractLineItems(
        lines: List<String>,
        totalAmount: Double?,
    ): List<ReceiptOcrItem> {
        val singleLineItems = lines.mapNotNull { line ->
            val normalizedLine = line.searchable()
            val isTotalLine = totalKeywords.any { normalizedLine.contains(it) }
            val isNonItemLine =
                nonTotalKeywords.any { normalizedLine.contains(it) } ||
                    normalizedLine.hasItemLineNoise()

            if (isTotalLine || isNonItemLine) return@mapNotNull null

            val amountMatches =
                amountPattern.findAll(line).mapNotNull { match ->
                    val value = match.groupValues[1].parseMoneyToken() ?: return@mapNotNull null
                    if (value < 1_000.0 || value > 1_000_000_000.0) return@mapNotNull null
                    if (totalAmount != null && value > totalAmount) return@mapNotNull null
                    match to value
                }.toList()

            val (amountMatch, amount) = amountMatches.maxByOrNull { (_, value) -> value } ?: return@mapNotNull null
            val (rawName, quantity) = line.substring(0, amountMatch.range.first).toReceiptItemNameAndQuantity()
            if (rawName.isBlank()) return@mapNotNull null
            if (!rawName.any { it.isLetter() }) return@mapNotNull null

            ReceiptOcrItem(
                name = rawName.take(48),
                amount = amount,
                quantity = quantity,
                unitPrice = amount / quantity,
            )
        }

        return singleLineItems
            .ifEmpty { extractColumnarLineItems(lines, totalAmount) }
            .ifEmpty { extractInterleavedLineItems(lines, totalAmount) }
            .ifEmpty { extractSparseLineItems(lines, totalAmount) }
    }

    private fun extractColumnarLineItems(
        lines: List<String>,
        totalAmount: Double?,
    ): List<ReceiptOcrItem> {
        val itemHeaderIndex =
            lines.indexOfFirst { line ->
                val searchableLine = line.searchable()
                searchableLine.contains("mon an") ||
                    searchableLine.contains("item") ||
                    searchableLine.contains("description")
            }
        val startIndex = itemHeaderIndex.takeIf { it >= 0 }?.plus(1) ?: 0

        val quantityHeaderIndex =
            lines.indexOfFirst { line ->
                val searchableLine = line.searchable().trim()
                searchableLine == "sl" ||
                    searchableLine.startsWith("sl ") ||
                    searchableLine.contains(" sl ") ||
                    searchableLine == "qty" ||
                    searchableLine.contains("so luong") ||
                    searchableLine.contains("quantity")
            }
        val amountHeaderIndex =
            lines.indexOfFirst { line ->
                val searchableLine = line.searchable().trim()
                searchableLine == "thanh tien" ||
                    searchableLine.contains("thanh tien") ||
                    searchableLine == "amount" ||
                    searchableLine == "price"
            }
        if (itemHeaderIndex < 0 && quantityHeaderIndex < 0 && amountHeaderIndex < 0) {
            return emptyList()
        }
        val totalSummaryIndex =
            lines.drop(startIndex).indexOfFirst { line ->
                line.isTotalSummaryLine()
            }.takeIf { it >= 0 }?.plus(startIndex) ?: -1

        val sectionItems =
            extractColumnarSections(
                lines = lines,
                itemHeaderIndex = itemHeaderIndex,
                quantityHeaderIndex = quantityHeaderIndex,
                amountHeaderIndex = amountHeaderIndex,
                totalSummaryIndex = totalSummaryIndex,
                totalAmount = totalAmount,
            )
        if (sectionItems.isNotEmpty()) return sectionItems

        val endIndex =
            lines.drop(startIndex).indexOfFirst { line ->
                totalKeywords
                    .filterNot { keyword -> keyword == "thanh tien" }
                    .any { keyword -> line.searchable().contains(keyword) }
            }.takeIf { it >= 0 }?.plus(startIndex) ?: lines.size

        val scanLines = lines.subList(startIndex, endIndex.coerceAtLeast(startIndex))
        val names =
            scanLines.mapNotNull { line ->
                val searchableLine = line.searchable()
                val hasAmount = amountPattern.containsMatchIn(line)
                val isNoise =
                    nonTotalKeywords.any { searchableLine.contains(it) } ||
                        searchableLine.hasItemLineNoise()
                if (!hasAmount && !isNoise && searchableLine.any { it.isLetter() }) {
                    line.cleanReceiptItemName().takeIf { it.isNotBlank() }
                } else {
                    null
                }
            }
        val quantities =
            scanLines.mapNotNull { line ->
                line.trim().toIntOrNull()?.takeIf { it in 1..999 }
            }
        val amounts =
            scanLines.mapNotNull { line ->
                val searchableLine = line.searchable()
                val isNoise =
                    nonTotalKeywords.any { searchableLine.contains(it) } ||
                        searchableLine.hasItemLineNoise()
                if (isNoise) return@mapNotNull null

                amountPattern.findAll(line)
                    .mapNotNull { match -> match.groupValues[1].parseMoneyToken() }
                    .filter { value ->
                        value >= 1_000.0 &&
                            value <= 1_000_000_000.0 &&
                            (totalAmount == null || value <= totalAmount)
                    }
                    .maxOrNull()
            }

        if (names.isEmpty() || amounts.isEmpty()) return emptyList()

        return names.take(amounts.size).mapIndexed { index, name ->
            val quantity = quantities.getOrNull(index)?.coerceAtLeast(1) ?: 1
            val amount = amounts[index]
            ReceiptOcrItem(
                name = name.take(48),
                amount = amount,
                quantity = quantity,
                unitPrice = amount / quantity,
            )
        }
    }

    private fun extractColumnarSections(
        lines: List<String>,
        itemHeaderIndex: Int,
        quantityHeaderIndex: Int,
        amountHeaderIndex: Int,
        totalSummaryIndex: Int,
        totalAmount: Double?,
    ): List<ReceiptOcrItem> {
        if (itemHeaderIndex < 0 || amountHeaderIndex < 0) return emptyList()

        val nameEnd =
            listOf(quantityHeaderIndex, amountHeaderIndex, totalSummaryIndex)
                .filter { it > itemHeaderIndex }
                .minOrNull()
                ?: lines.size
        val names =
            lines.subList(itemHeaderIndex + 1, nameEnd)
                .mapNotNull { line -> line.toColumnItemNameOrNull() }

        if (names.isEmpty()) return emptyList()

        val quantities =
            if (quantityHeaderIndex >= 0) {
                extractReceiptQuantities(
                    lines = lines.subList(quantityHeaderIndex + 1, lines.size),
                    limit = names.size,
                )
            } else {
                emptyList()
            }

        val amountEnd =
            totalSummaryIndex
                .takeIf { it > amountHeaderIndex }
                ?: lines.size
        val amounts =
            lines.subList(amountHeaderIndex + 1, amountEnd)
                .flatMap { line -> line.extractReceiptAmounts(totalAmount) }
                .filterNot { value -> totalAmount != null && value.roundMoney() == totalAmount.roundMoney() }
                .take(names.size)

        if (amounts.isEmpty()) return emptyList()

        return names.take(amounts.size).mapIndexed { index, name ->
            val quantity = quantities.getOrNull(index)?.coerceAtLeast(1) ?: 1
            val amount = amounts[index]
            ReceiptOcrItem(
                name = name.take(48),
                amount = amount,
                quantity = quantity,
                unitPrice = amount / quantity,
            )
        }
    }

    private fun extractSparseLineItems(
        lines: List<String>,
        totalAmount: Double?,
    ): List<ReceiptOcrItem> {
        val totalIndex =
            lines.indexOfFirst { line -> line.isTotalSummaryLine() }
                .takeIf { it >= 0 }
                ?: lines.size
        val scanLines = lines.take(totalIndex)
        val names =
            scanLines.mapNotNull { line ->
                line.toSparseItemNameOrNull()
            }
        val quantities =
            scanLines.mapNotNull { line ->
                line.trim().toIntOrNull()?.takeIf { it in 1..999 }
            }
        val amounts =
            scanLines
                .flatMap { line -> line.extractReceiptAmounts(totalAmount) }
                .let { values ->
                    if (totalAmount != null && values.size > names.size) {
                        values.filterNot { value -> value.roundMoney() == totalAmount.roundMoney() }
                    } else {
                        values
                    }
                }
                .take(names.size)

        if (names.size < 2 || amounts.size < names.size) return emptyList()

        return names.mapIndexed { index, name ->
            val quantity = quantities.getOrNull(index)?.coerceAtLeast(1) ?: 1
            val amount = amounts[index]
            ReceiptOcrItem(
                name = name.take(48),
                amount = amount,
                quantity = quantity,
                unitPrice = amount / quantity,
            )
        }
    }

    private fun extractInterleavedLineItems(
        lines: List<String>,
        totalAmount: Double?,
    ): List<ReceiptOcrItem> {
        val totalIndex =
            lines.indexOfFirst { line -> line.isTotalSummaryLine() }
                .takeIf { it >= 0 }
                ?: lines.size
        val scanLines = lines.take(totalIndex)
        val items = mutableListOf<ReceiptOcrItem>()
        var index = 0

        while (index < scanLines.size) {
            val name = scanLines[index].toSparseItemNameOrNull()
            if (name == null) {
                index++
                continue
            }

            var quantity = 1
            var amount: Double? = null
            var consumedUntil = index
            val lookAheadEnd = minOf(scanLines.lastIndex, index + 6)
            var cursor = index + 1

            while (cursor <= lookAheadEnd) {
                val nextName = scanLines[cursor].toSparseItemNameOrNull()
                if (nextName != null && amount == null && cursor > index + 1) break

                if (quantity == 1) {
                    quantity = scanLines[cursor].trim().toIntOrNull()?.takeIf { it in 1..999 } ?: quantity
                }
                val lineAmounts =
                    scanLines[cursor]
                        .extractReceiptAmounts(totalAmount)
                        .filterNot { value -> totalAmount != null && value.roundMoney() == totalAmount.roundMoney() }
                if (lineAmounts.isNotEmpty()) {
                    amount = lineAmounts.maxOrNull()
                    consumedUntil = cursor
                    break
                }
                cursor++
            }

            if (amount != null) {
                items +=
                    ReceiptOcrItem(
                        name = name.take(48),
                        amount = amount,
                        quantity = quantity.coerceAtLeast(1),
                        unitPrice = amount / quantity.coerceAtLeast(1),
                    )
                index = consumedUntil + 1
            } else {
                index++
            }
        }

        return items.takeIf { it.size >= 2 }.orEmpty()
    }

    private fun inferCategory(
        rawText: String,
        categories: List<ReceiptCategoryOption>,
    ): ReceiptCategoryOption? {
        val searchableText = rawText.searchable()
        val expenseCategories = categories.filter { it.type == TransactionType.EXPENSE }

        return expenseCategories
            .map { category -> category to category.score(searchableText) }
            .filter { (_, score) -> score > 0 }
            .maxByOrNull { (_, score) -> score }
            ?.first
    }

    private fun ReceiptCategoryOption.score(searchableText: String): Int {
        val searchableId = id.searchable()
        val searchableName = name.searchable()
        val nameTokens =
            searchableName
                .split(Regex("""\s+"""))
                .filter { it.length >= 3 }

        var score = nameTokens.count { searchableText.contains(it) } * 2
        if (searchableName.isNotBlank() && searchableText.contains(searchableName)) {
            score += 5
        }

        categoryBuckets.forEach { bucket ->
            val matchesBucket =
                bucket.hints.any { hint ->
                    searchableId.contains(hint) || searchableName.contains(hint)
                }
            if (matchesBucket) {
                score += bucket.keywords.count { keyword -> searchableText.contains(keyword) } * 3
            }
        }

        return score
    }

    private fun inferMerchantName(lines: List<String>): String? {
        return lines.firstOrNull { line ->
            val searchableLine = line.searchable()
            val hasLetters = searchableLine.any { it.isLetter() }
            val isNoise = merchantNoiseKeywords.any { searchableLine.contains(it) }
            val looksLikeAmountOnly = amountPattern.matches(line)
            hasLetters && !isNoise && !looksLikeAmountOnly
        }?.take(48)
    }

    private fun String.parseMoneyToken(): Double? {
        val token =
            filter { it.isDigit() || it == '.' || it == ',' || it == ' ' }
                .replace(" ", "")
        if (token.isBlank() || token.none { it.isDigit() }) return null

        val dotCount = token.count { it == '.' }
        val commaCount = token.count { it == ',' }
        val normalized =
            when {
                dotCount > 0 && commaCount > 0 -> normalizeMixedSeparators(token)
                dotCount > 0 -> normalizeSingleSeparator(token, '.')
                commaCount > 0 -> normalizeSingleSeparator(token, ',')
                else -> token
            } ?: return null

        return normalized.toDoubleOrNull()
    }

    private fun String.cleanReceiptItemName(): String {
        return toReceiptItemNameAndQuantity().first
    }

    private fun String.toReceiptItemNameAndQuantity(): Pair<String, Int> {
        val cleanText =
            replace(Regex("""\s+"""), " ")
            .trim()
        val quantityPatterns =
            listOf(
                Regex("""(?i)\s+x\s*(\d{1,3})$"""),
                Regex("""(?i)\s+(\d{1,3})\s*x$"""),
                Regex("""\s+(\d{1,3})$"""),
            )

        quantityPatterns.forEach { pattern ->
            val match = pattern.find(cleanText) ?: return@forEach
            val quantity = match.groupValues[1].toIntOrNull()?.takeIf { it in 1..999 } ?: return@forEach
            val name = cleanText.removeRange(match.range).trim(' ', '-', ':', '.', ',')
            if (name.isNotBlank()) return name to quantity
        }

        return cleanText.trim(' ', '-', ':', '.', ',') to 1
    }

    private fun String.toColumnItemNameOrNull(): String? {
        val searchableLine = searchable()
        val hasAmount = amountPattern.containsMatchIn(this)
        val isNumericOnly = trim().toIntOrNull() != null
        val isNoise =
            nonTotalKeywords.any { searchableLine.contains(it) } ||
                searchableLine.hasItemLineNoise() ||
                isTotalSummaryLine()

        return if (!hasAmount && !isNumericOnly && !isNoise && searchableLine.any { it.isLetter() }) {
            cleanReceiptItemName().takeIf { it.isNotBlank() }
        } else {
            null
        }
    }

    private fun String.toSparseItemNameOrNull(): String? {
        val searchableLine = searchable()
        if (searchableLine.isSparseDocumentNoise()) return null

        return toColumnItemNameOrNull()
    }

    private fun String.extractReceiptAmounts(totalAmount: Double?): List<Double> {
        val searchableLine = searchable()
        if (searchableLine.isSparseDocumentNoise()) return emptyList()
        if (nonTotalKeywords.any { searchableLine.contains(it) }) return emptyList()

        return amountPattern.findAll(this)
            .mapNotNull { match -> match.groupValues[1].parseMoneyToken() }
            .filter { value ->
                value >= 1_000.0 &&
                    value <= 1_000_000_000.0 &&
                    (totalAmount == null || value <= totalAmount)
            }
            .toList()
    }

    private fun extractReceiptQuantities(
        lines: List<String>,
        limit: Int,
    ): List<Int> {
        val quantities = mutableListOf<Int>()
        lines.forEach { line ->
            if (quantities.size >= limit) return quantities
            val searchableLine = line.searchable()
            if (searchableLine.isSparseDocumentNoise()) return@forEach
            if (line.extractReceiptAmounts(totalAmount = null).isNotEmpty()) return@forEach

            val quantity =
                Regex("""\b\d{1,3}\b""")
                    .findAll(line)
                    .mapNotNull { match -> match.value.toIntOrNull()?.takeIf { it in 1..999 } }
                    .firstOrNull()
                    ?: return@forEach
            quantities += quantity
        }
        return quantities
    }

    private fun String.isTotalSummaryLine(): Boolean {
        val searchableLine = searchable()
        return totalKeywords
            .filterNot { keyword -> keyword == "thanh tien" }
            .any { keyword -> searchableLine.contains(keyword) }
    }

    private fun Double.roundMoney(): Long = roundToLong()

    private fun String.isSparseDocumentNoise(): Boolean {
        return contains("dinesplit") ||
            contains("hoa don") ||
            contains("ocr") ||
            contains("expected") ||
            contains("cam on") ||
            contains("ngay") ||
            contains("ban:")
    }

    private fun String.hasItemLineNoise(): Boolean {
        val normalized = trim()
        return itemLineNoiseKeywords.any { keyword ->
            if (keyword.length <= 3) {
                normalized == keyword
            } else {
                normalized.contains(keyword)
            }
        }
    }

    private fun normalizeMixedSeparators(token: String): String? {
        val decimalSeparator = if (token.lastIndexOf('.') > token.lastIndexOf(',')) '.' else ','
        val groupingSeparator = if (decimalSeparator == '.') ',' else '.'
        val decimalIndex = token.lastIndexOf(decimalSeparator)
        val fractionalLength = token.length - decimalIndex - 1

        return if (fractionalLength in 1..2) {
            val whole =
                token.substring(0, decimalIndex)
                    .split(groupingSeparator)
                    .takeIf { it.hasValidGrouping() }
                    ?.joinToString("")
                    ?: return null
            val fraction = token.substring(decimalIndex + 1)
            "$whole.$fraction"
        } else {
            val parts = token.split(decimalSeparator, groupingSeparator)
            if (!parts.hasValidGrouping()) return null
            parts.joinToString("")
        }
    }

    private fun normalizeSingleSeparator(
        token: String,
        separator: Char,
    ): String? {
        val parts = token.split(separator)
        if (parts.size <= 1) return token

        return when {
            parts.size > 2 && parts.hasValidGrouping() -> parts.joinToString("")
            parts.size > 2 -> null
            parts[1].length == 3 -> parts.joinToString("")
            parts[1].length in 1..2 -> parts.joinToString(".")
            else -> null
        }
    }

    private fun List<String>.hasValidGrouping(): Boolean {
        if (isEmpty() || first().isBlank() || first().length > 3) return false
        return drop(1).all { it.length == 3 && it.all(Char::isDigit) }
    }

    private fun String.searchable(): String {
        return Normalizer.normalize(lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("""\p{Mn}+"""), "")
            .replace(DONG_LETTER_CHAR, 'd')
            .replace(DONG_SIGN_CHAR, 'd')
    }

    private fun String.hasCurrencyMarker(): Boolean {
        val lower = lowercase(Locale.ROOT)
        return lower.contains("vnd") ||
            lower.contains("vn$DONG_LETTER") ||
            lower.contains(DONG_LETTER_CHAR) ||
            lower.contains(DONG_SIGN_CHAR) ||
            Regex("""(?i)(^|\s)d($|\s)""").containsMatchIn(this)
    }

    private data class AmountCandidate(
        val value: Double,
        val score: Double,
    )

    private data class CategoryKeywordBucket(
        val hints: Set<String>,
        val keywords: Set<String>,
    )
}
