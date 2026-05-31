package com.example.dinesplit.domain.usecase

import com.example.dinesplit.domain.model.Group
import com.example.dinesplit.domain.repository.SplitRepository
import kotlinx.coroutines.flow.Flow

class GetGroupsUseCase(
    private val repository: SplitRepository,
) {
    operator fun invoke(): Flow<List<Group>> = repository.getGroups()
}
