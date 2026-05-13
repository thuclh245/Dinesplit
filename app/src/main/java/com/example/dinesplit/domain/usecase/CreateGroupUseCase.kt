package com.example.dinesplit.domain.usecase

import com.example.dinesplit.domain.model.Group
import com.example.dinesplit.domain.repository.SplitRepository

class CreateGroupUseCase(
    private val repository: SplitRepository
) {
    suspend operator fun invoke(group: Group) = repository.createGroup(group)
}
