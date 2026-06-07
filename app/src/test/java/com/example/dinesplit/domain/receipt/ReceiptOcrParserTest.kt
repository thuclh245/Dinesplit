package com.example.dinesplit.domain.receipt

import com.example.dinesplit.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiptOcrParserTest {
    private val categories =
        listOf(
            ReceiptCategoryOption("c_food", "Dining Out", TransactionType.EXPENSE),
            ReceiptCategoryOption("c_grocery", "Groceries", TransactionType.EXPENSE),
            ReceiptCategoryOption("c_transit", "Transit", TransactionType.EXPENSE),
            ReceiptCategoryOption("c_fun", "Entertainment", TransactionType.EXPENSE),
            ReceiptCategoryOption("c_salary", "Salary", TransactionType.INCOME),
        )

    @Test
    fun `parse prefers total line and maps cafe receipt to dining`() {
        val result =
            ReceiptOcrParser.parse(
                rawText =
                    """
                    HIGHLANDS COFFEE
                    Subtotal 45.000
                    VAT 4.500
                    Tong cong 49.500d
                    """.trimIndent(),
                categories = categories,
            )

        assertEquals(49_500.0, result.amount ?: 0.0, 0.001)
        assertEquals("c_food", result.category?.id)
        assertEquals("HIGHLANDS COFFEE", result.merchantName)
    }

    @Test
    fun `parse maps supermarket receipt to groceries`() {
        val result =
            ReceiptOcrParser.parse(
                rawText =
                    """
                    CO.OP MART
                    Milk 32,000
                    Bread 18,000
                    TOTAL 50,000 VND
                    """.trimIndent(),
                categories = categories,
            )

        assertEquals(50_000.0, result.amount ?: 0.0, 0.001)
        assertEquals("c_grocery", result.category?.id)
    }

    @Test
    fun `parse maps taxi receipt to transit`() {
        val result =
            ReceiptOcrParser.parse(
                rawText =
                    """
                    GrabCar
                    Distance 7.2 km
                    Amount due: 120000
                    """.trimIndent(),
                categories = categories,
            )

        assertEquals(120_000.0, result.amount ?: 0.0, 0.001)
        assertEquals("c_transit", result.category?.id)
    }

    @Test
    fun `parse extracts receipt line items`() {
        val result =
            ReceiptOcrParser.parse(
                rawText =
                    """
                    DINESPLIT CAFE
                    Mon an            SL    Thanh tien
                    Pho bo dac biet     2    130000 VND
                    Tra sua tran chau   2    70000 VND
                    Nuoc suoi           1    15000 VND
                    Phi dich vu         1    10000 VND
                    TONG TIEN                225000 VND
                    """.trimIndent(),
                categories = categories,
            )

        assertEquals(225_000.0, result.amount ?: 0.0, 0.001)
        assertEquals(4, result.items.size)
        assertEquals("Pho bo dac biet", result.items[0].name)
        assertEquals(130_000.0, result.items[0].amount, 0.001)
        assertEquals(2, result.items[0].quantity)
        assertEquals(65_000.0, result.items[0].unitPrice, 0.001)
        assertEquals("Tra sua tran chau", result.items[1].name)
        assertEquals(70_000.0, result.items[1].amount, 0.001)
    }

    @Test
    fun `parse extracts columnar receipt line items when ocr splits table columns`() {
        val result =
            ReceiptOcrParser.parse(
                rawText =
                    """
                    DINESPLIT CAFE
                    Mon an
                    Pho bo dac biet
                    Tra sua tran chau
                    Nuoc suoi
                    Phi dich vu
                    SL
                    2
                    2
                    1
                    1
                    Thanh tien
                    130000 VND
                    70000 VND
                    15000 VND
                    10000 VND
                    TONG TIEN
                    225000 VND
                    """.trimIndent(),
                categories = categories,
            )

        assertEquals(4, result.items.size)
        assertEquals("Pho bo dac biet", result.items[0].name)
        assertEquals(2, result.items[0].quantity)
        assertEquals(65_000.0, result.items[0].unitPrice, 0.001)
        assertEquals(130_000.0, result.items[0].amount, 0.001)
        assertEquals("Phi dich vu", result.items[3].name)
        assertEquals(10_000.0, result.items[3].amount, 0.001)
    }

    @Test
    fun `parse extracts line items when ml kit returns receipt columns separately`() {
        val result =
            ReceiptOcrParser.parse(
                rawText =
                    """
                    DINESPLIT BISTRO
                    HOA DON TEST OCR
                    Mon an
                    Banh mi bo
                    Pho bo
                    Tra sua
                    Nuoc suoi
                    TONG TIEN
                    THANH TOAN
                    SL
                    1
                    2
                    3
                    4
                    Thanh tien
                    35000 VND
                    90000 VND
                    75000 VND
                    40000 VND
                    240000 VND
                    240000 VND
                    """.trimIndent(),
                categories = categories,
            )

        assertEquals(240_000.0, result.amount ?: 0.0, 0.001)
        assertEquals(4, result.items.size)
        assertEquals("Banh mi bo", result.items[0].name)
        assertEquals(1, result.items[0].quantity)
        assertEquals(35_000.0, result.items[0].unitPrice, 0.001)
        assertEquals("Pho bo", result.items[1].name)
        assertEquals(2, result.items[1].quantity)
        assertEquals(45_000.0, result.items[1].unitPrice, 0.001)
        assertEquals("Nuoc suoi", result.items[3].name)
        assertEquals(4, result.items[3].quantity)
        assertEquals(10_000.0, result.items[3].unitPrice, 0.001)
    }

    @Test
    fun `parse extracts sparse receipt items when headers are not recognized`() {
        val result =
            ReceiptOcrParser.parse(
                rawText =
                    """
                    DINESPLIT BISTRO
                    HOA DON TEST OCR
                    Ngay: 07/06/2026
                    Ban: QTY-04
                    Banh mi bo
                    Pho bo
                    Tra sua
                    Nuoc suoi
                    1
                    2
                    3
                    4
                    35000 VND
                    90000 VND
                    75000 VND
                    40000 VND
                    TONG TIEN
                    240000 VND
                    """.trimIndent(),
                categories = categories,
            )

        assertEquals(4, result.items.size)
        assertEquals("Banh mi bo", result.items[0].name)
        assertEquals(1, result.items[0].quantity)
        assertEquals(35_000.0, result.items[0].amount, 0.001)
        assertEquals("Nuoc suoi", result.items[3].name)
        assertEquals(4, result.items[3].quantity)
        assertEquals(40_000.0, result.items[3].amount, 0.001)
    }

    @Test
    fun `parse extracts interleaved receipt items`() {
        val result =
            ReceiptOcrParser.parse(
                rawText =
                    """
                    DINESPLIT BISTRO
                    Banh mi bo
                    1
                    35000 VND
                    Pho bo
                    2
                    90000 VND
                    Tra sua
                    3
                    75000 VND
                    Nuoc suoi
                    4
                    40000 VND
                    TONG TIEN
                    240000 VND
                    """.trimIndent(),
                categories = categories,
            )

        assertEquals(4, result.items.size)
        assertEquals("Banh mi bo", result.items[0].name)
        assertEquals(1, result.items[0].quantity)
        assertEquals(35_000.0, result.items[0].unitPrice, 0.001)
        assertEquals("Nuoc suoi", result.items[3].name)
        assertEquals(4, result.items[3].quantity)
        assertEquals(10_000.0, result.items[3].unitPrice, 0.001)
    }

    @Test
    fun `parse extracts emulator ml kit raw text with mixed columns and noisy lines`() {
        val result =
            ReceiptOcrParser.parse(
                rawText =
                    """
                    DINESPLIT BISTRO
                    Ngay: 07/06/2026
                    Mon an
                    Banh mi bo
                    Pho bo
                    Tra sua
                    HOA DON TEST OCR
                    Nuoc suoi
                    TONG TIEN
                    THANH TOAN
                    SL Thanh tien
                    1 1
                    2
                    3
                    Ban: QTY-04
                    4
                    35000 \VND
                    Cam on quy khach!
                    90000 VND
                    Expected items: 4
                    Expected quantities: 1, 2, 3, 4
                    Expected total: 240000 VND
                    75000 \VND
                    240000 VND
                    40000 \VND
                    240000 VND
                    """.trimIndent(),
                categories = categories,
            )

        assertEquals(240_000.0, result.amount ?: 0.0, 0.001)
        assertEquals(4, result.items.size)
        assertEquals("Banh mi bo", result.items[0].name)
        assertEquals(1, result.items[0].quantity)
        assertEquals(35_000.0, result.items[0].amount, 0.001)
        assertEquals("Pho bo", result.items[1].name)
        assertEquals(2, result.items[1].quantity)
        assertEquals(90_000.0, result.items[1].amount, 0.001)
        assertEquals("Tra sua", result.items[2].name)
        assertEquals(3, result.items[2].quantity)
        assertEquals(75_000.0, result.items[2].amount, 0.001)
        assertEquals("Nuoc suoi", result.items[3].name)
        assertEquals(4, result.items[3].quantity)
        assertEquals(40_000.0, result.items[3].amount, 0.001)
    }
}
