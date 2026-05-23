package com.example.dinesplit.domain.receipt

import com.example.dinesplit.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiptOcrParserTest {
    private val categories = listOf(
        ReceiptCategoryOption("c_food", "Dining Out", TransactionType.EXPENSE),
        ReceiptCategoryOption("c_grocery", "Groceries", TransactionType.EXPENSE),
        ReceiptCategoryOption("c_transit", "Transit", TransactionType.EXPENSE),
        ReceiptCategoryOption("c_fun", "Entertainment", TransactionType.EXPENSE),
        ReceiptCategoryOption("c_salary", "Salary", TransactionType.INCOME)
    )

    @Test
    fun `parse prefers total line and maps cafe receipt to dining`() {
        val result = ReceiptOcrParser.parse(
            rawText = """
                HIGHLANDS COFFEE
                Subtotal 45.000
                VAT 4.500
                Tong cong 49.500d
            """.trimIndent(),
            categories = categories
        )

        assertEquals(49_500.0, result.amount ?: 0.0, 0.001)
        assertEquals("c_food", result.category?.id)
        assertEquals("HIGHLANDS COFFEE", result.merchantName)
    }

    @Test
    fun `parse maps supermarket receipt to groceries`() {
        val result = ReceiptOcrParser.parse(
            rawText = """
                CO.OP MART
                Milk 32,000
                Bread 18,000
                TOTAL 50,000 VND
            """.trimIndent(),
            categories = categories
        )

        assertEquals(50_000.0, result.amount ?: 0.0, 0.001)
        assertEquals("c_grocery", result.category?.id)
    }

    @Test
    fun `parse maps taxi receipt to transit`() {
        val result = ReceiptOcrParser.parse(
            rawText = """
                GrabCar
                Distance 7.2 km
                Amount due: 120000
            """.trimIndent(),
            categories = categories
        )

        assertEquals(120_000.0, result.amount ?: 0.0, 0.001)
        assertEquals("c_transit", result.category?.id)
    }
}
