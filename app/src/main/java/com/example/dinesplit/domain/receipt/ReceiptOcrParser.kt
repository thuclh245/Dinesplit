package com.example.dinesplit.domain.receipt

import com.example.dinesplit.domain.model.TransactionType
import java.text.Normalizer
import java.util.Locale
import kotlin.math.ln

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

        return ReceiptOcrResult(
            rawText = rawText,
            amount = extractTotalAmount(lines),
            category = inferCategory(rawText, categories),
            merchantName = inferMerchantName(lines),
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
