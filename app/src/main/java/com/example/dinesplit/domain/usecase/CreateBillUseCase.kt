package com.example.dinesplit.domain.usecase

import com.example.dinesplit.domain.model.Bill
import com.example.dinesplit.domain.model.BillSplit
import com.example.dinesplit.domain.repository.SplitRepository

class CreateBillUseCase(
    private val repository: SplitRepository
) {
    suspend operator fun invoke(bill: Bill, splits: List<BillSplit>) = 
        repository.createBill(bill, splits)
}
