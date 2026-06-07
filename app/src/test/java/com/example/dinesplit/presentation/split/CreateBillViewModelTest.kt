package com.example.dinesplit.presentation.split

import com.example.dinesplit.domain.model.SplitMethod
import com.example.dinesplit.domain.receipt.ReceiptOcrItem
import com.example.dinesplit.domain.repository.impl.FakeSplitRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CreateBillViewModelTest {
    @Test
    fun `members load and saveBill produces correct shares for equal split`() =
        runBlocking {
            val repo = FakeSplitRepository()
            val vm = CreateBillViewModel(repository = repo, groupId = "g1", autoLoadMembers = false, currentUserId = "1")

            // set members from fake repo directly for deterministic test
            vm.setMembersForTest(repo.getCurrentMembers())

            val members = vm.uiState.value.members
            assertEquals(3, members.size)

            // set up state for equal split
            vm.onTotalAmountChange("300")
            vm.onMethodSelect(SplitMethod.EQUAL)

            // call suspend save and wait
            vm.saveBillBlocking()

            // assert repository received a bill and shares are equal
            val saved = repo.lastSavedBill
            assertNotNull(saved)
            val shares = saved!!.shares
            // each of 3 members should have 100.0
            assertEquals(3, shares.size)
            shares.values.forEach { v -> assertEquals(100.0, v, 0.001) }
        }

    @Test
    fun `custom split uses provided custom amounts`() =
        runBlocking {
            val repo = FakeSplitRepository()
            val vm = CreateBillViewModel(repository = repo, groupId = "g1", autoLoadMembers = false, currentUserId = "1")

            vm.setMembersForTest(repo.getCurrentMembers())

            // set custom amounts
            vm.onTotalAmountChange("300")
            vm.onCustomAmountChange("1", "50")
            vm.onCustomAmountChange("2", "100")
            vm.onCustomAmountChange("3", "150")
            vm.onMethodSelect(SplitMethod.CUSTOM)

            vm.saveBillBlocking()

            val saved = repo.lastSavedBill
            assertNotNull(saved)
            val shares = saved!!.shares
            assertEquals(3, shares.size)
            assertEquals(50.0, shares["1"] ?: 0.0, 0.001)
            assertEquals(100.0, shares["2"] ?: 0.0, 0.001)
            assertEquals(150.0, shares["3"] ?: 0.0, 0.001)
        }

    @Test
    fun `itemized split divides items among sharers correctly`() =
        runBlocking {
            val repo = FakeSplitRepository()
            val vm = CreateBillViewModel(repository = repo, groupId = "g1", autoLoadMembers = false, currentUserId = "1")

            vm.setMembersForTest(repo.getCurrentMembers())

            // setup itemized items
            vm.billItems.clear()
            vm.billItems.add(
                com.example.dinesplit.domain.model.BillItem(name = "Item1", price = 200.0, sharedByMemberIds = listOf("1", "2")),
            )
            vm.billItems.add(com.example.dinesplit.domain.model.BillItem(name = "Item2", price = 100.0, sharedByMemberIds = listOf("3")))

            vm.onMethodSelect(SplitMethod.ITEMIZED)

            vm.saveBillBlocking()

            val saved = repo.lastSavedBill
            assertNotNull(saved)
            val shares = saved!!.shares
            assertEquals(3, shares.size)
            assertEquals(100.0, shares["1"] ?: 0.0, 0.001)
            assertEquals(100.0, shares["2"] ?: 0.0, 0.001)
            assertEquals(100.0, shares["3"] ?: 0.0, 0.001)
        }

    @Test
    fun `switching to itemized keeps entered total as first item price`() =
        runBlocking {
            val repo = FakeSplitRepository()
            val vm = CreateBillViewModel(repository = repo, groupId = "g1", autoLoadMembers = false, currentUserId = "1")

            vm.onTotalAmountChange("250000")
            vm.onMethodSelect(SplitMethod.ITEMIZED)

            assertEquals(250000.0, vm.billItems.first().price, 0.001)
        }

    @Test
    fun `receipt ocr populates itemized items and defaults empty item sharers to all members`() =
        runBlocking {
            val repo = FakeSplitRepository()
            val vm = CreateBillViewModel(repository = repo, groupId = "g1", autoLoadMembers = false, currentUserId = "1")

            vm.setMembersForTest(repo.getCurrentMembers())
            vm.applyReceiptOcr(
                amount = 225_000.0,
                merchantName = "DINESPLIT CAFE",
                items =
                    listOf(
                        ReceiptOcrItem(name = "Pho bo dac biet", amount = 130_000.0, quantity = 2, unitPrice = 65_000.0),
                        ReceiptOcrItem(name = "Tra sua tran chau", amount = 70_000.0, quantity = 2, unitPrice = 35_000.0),
                        ReceiptOcrItem(name = "Nuoc suoi", amount = 15_000.0),
                        ReceiptOcrItem(name = "Phi dich vu", amount = 10_000.0),
                    ),
            )

            assertEquals(SplitMethod.ITEMIZED, vm.uiState.value.selectedMethod)
            assertEquals(4, vm.billItems.size)
            assertEquals("DINESPLIT CAFE", vm.uiState.value.billName)
            assertEquals(225000.0, vm.billItems.sumOf { it.price }, 0.001)

            vm.saveBillBlocking()

            val saved = repo.lastSavedBill
            assertNotNull(saved)
            assertEquals(4, saved!!.items.size)
            assertEquals(2, saved.items.first().quantity)
            assertEquals(65_000.0, saved.items.first().unitPrice, 0.001)
            saved.items.forEach { item ->
                assertEquals(listOf("1", "2", "3"), item.sharedByMemberIds)
            }
            assertEquals(75_000.0, saved.shares["1"] ?: 0.0, 0.001)
            assertEquals(75_000.0, saved.shares["2"] ?: 0.0, 0.001)
            assertEquals(75_000.0, saved.shares["3"] ?: 0.0, 0.001)
        }

    @Test
    fun `custom split rejects amounts that do not match total`() =
        runBlocking {
            val repo = FakeSplitRepository()
            val vm = CreateBillViewModel(repository = repo, groupId = "g1", autoLoadMembers = false, currentUserId = "1")

            vm.setMembersForTest(repo.getCurrentMembers())
            vm.onTotalAmountChange("300")
            vm.onCustomAmountChange("1", "50")
            vm.onCustomAmountChange("2", "100")
            vm.onCustomAmountChange("3", "100")
            vm.onMethodSelect(SplitMethod.CUSTOM)

            val result = vm.saveBillBlocking()

            assertEquals(true, result.isFailure)
            assertEquals(null, repo.lastSavedBill)
        }

    @Test
    fun `equal split distributes remainder as whole dong`() =
        runBlocking {
            val repo = FakeSplitRepository()
            val vm = CreateBillViewModel(repository = repo, groupId = "g1", autoLoadMembers = false, currentUserId = "1")

            vm.setMembersForTest(repo.getCurrentMembers())
            vm.onTotalAmountChange("100")
            vm.onMethodSelect(SplitMethod.EQUAL)

            vm.saveBillBlocking()

            val saved = repo.lastSavedBill
            assertNotNull(saved)
            assertEquals(100.0, saved!!.shares.values.sum(), 0.001)
            assertEquals(listOf(34.0, 33.0, 33.0), saved.shares.values.toList())
        }
}
