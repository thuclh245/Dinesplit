package com.example.dinesplit.domain.repository

import com.example.dinesplit.domain.model.Group
import kotlinx.coroutines.flow.Flow

interface SplitRepository {
    fun getGroups(): Flow<List<Group>>
    suspend fun createGroup(group: Group)
    suspend fun joinGroup(inviteCode: String)
}
