package com.example.dinesplit.presentation.personal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.core.firebase.FirebaseErrorMapper
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.domain.model.ReminderType
import com.example.dinesplit.domain.model.SpendingReminder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for spending reminder management.
 * Tuần 3: Local reminder flow + trigger checking.
 * Tuần 4: Consider Firebase sync if needed.
 */
class SpendingReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferences = application.getSharedPreferences("spending_reminders", 0)

    private val _reminders = MutableStateFlow<List<SpendingReminder>>(emptyList())
    val reminders: StateFlow<List<SpendingReminder>> = _reminders.asStateFlow()

    private val _triggeredReminders = MutableStateFlow<List<SpendingReminder>>(emptyList())
    val triggeredReminders: StateFlow<List<SpendingReminder>> = _triggeredReminders.asStateFlow()

    private val _uiState = MutableStateFlow(SpendingReminderUiState())
    val uiState: StateFlow<SpendingReminderUiState> = _uiState.asStateFlow()

    init {
        loadLocalReminders()
    }

    fun addReminder(
        categoryId: String?,
        categoryName: String,
        budgetAmount: Double,
        threshold: Float = 0.8f,
        reminderType: ReminderType = ReminderType.MONTHLY
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val reminder = SpendingReminder(
                    id = UUID.randomUUID().toString(),
                    userId = currentUserId(),
                    categoryId = categoryId,
                    categoryName = categoryName,
                    budgetAmount = budgetAmount,
                    threshold = threshold,
                    reminderType = reminderType
                )
                saveReminderLocally(reminder)
                loadLocalReminders()
                _uiState.value = _uiState.value.copy(successMessage = "Reminder created")
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = FirebaseErrorMapper.toUserMessage(throwable)
                )
            }
        }
    }

    fun deleteReminder(reminderId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                deleteReminderLocally(reminderId)
                loadLocalReminders()
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = FirebaseErrorMapper.toUserMessage(throwable)
                )
            }
        }
    }

    fun checkRemainders(currentSpentByCategory: Map<String, Double>) {
        viewModelScope.launch(Dispatchers.Default) {
            val triggered = _reminders.value.filter { reminder ->
                val spent = currentSpentByCategory[reminder.categoryId ?: "overall"] ?: 0.0
                val thresholdAmount = reminder.budgetAmount * reminder.threshold
                spent >= thresholdAmount && (reminder.lastAlertedAt == null ||
                    System.currentTimeMillis() - (reminder.lastAlertedAt ?: 0) > 3600000) // 1 hour cooldown
            }

            _triggeredReminders.value = triggered

            // Auto-mark as alerted
            triggered.forEach { reminder ->
                val updated = reminder.copy(lastAlertedAt = System.currentTimeMillis())
                saveReminderLocally(updated)
            }
        }
    }

    private fun loadLocalReminders() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = sharedPreferences.getString("reminders_json", "[]") ?: "[]"
                val reminders = parseRemindersJson(json)
                _reminders.value = reminders
            } catch (e: Exception) {
                _reminders.value = emptyList()
            }
        }
    }

    private fun saveReminderLocally(reminder: SpendingReminder) {
        try {
            val all = _reminders.value.filterNot { it.id == reminder.id } + reminder
            val json = serializeRemindersJson(all)
            sharedPreferences.edit().putString("reminders_json", json).apply()
        } catch (e: Exception) {
            // Fallback to in-memory
        }
    }

    private fun deleteReminderLocally(reminderId: String) {
        try {
            val updated = _reminders.value.filterNot { it.id == reminderId }
            val json = serializeRemindersJson(updated)
            sharedPreferences.edit().putString("reminders_json", json).apply()
        } catch (e: Exception) {
            // Fallback
        }
    }

    private fun currentUserId(): String {
        return FirebaseProviders.auth.currentUser?.uid ?: "anonymous"
    }

    // Simple JSON serialization (week 3 - basic, can be improved week 4)
    private fun serializeRemindersJson(reminders: List<SpendingReminder>): String {
        return reminders.joinToString(",") {
            "${it.id}|${it.categoryId}|${it.categoryName}|${it.budgetAmount}|${it.threshold}|${it.reminderType.name}|${it.isEnabled}|${it.lastAlertedAt ?: 0}"
        }
    }

    private fun parseRemindersJson(json: String): List<SpendingReminder> {
        if (json.isEmpty() || json == "[]") return emptyList()
        return json.split(",").mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size >= 8) {
                try {
                    SpendingReminder(
                        id = parts[0],
                        userId = currentUserId(),
                        categoryId = parts[1].takeIf { it.isNotEmpty() },
                        categoryName = parts[2],
                        budgetAmount = parts[3].toDouble(),
                        threshold = parts[4].toFloat(),
                        reminderType = ReminderType.valueOf(parts[5]),
                        isEnabled = parts[6].toBoolean(),
                        lastAlertedAt = parts[7].toLongOrNull()
                    )
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }
    }
}

data class SpendingReminderUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

