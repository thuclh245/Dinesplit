package com.example.dinesplit.core.notification

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.core.firebase.FirestoreCollections
import com.google.firebase.firestore.FieldValue
import com.google.firebase.messaging.FirebaseMessaging
import java.util.UUID

object FcmManager {
    private const val PREFS_NAME = "dinesplit_fcm_prefs"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_PENDING_TOKEN = "pending_fcm_token"
    private const val KEY_DISMISSED_TIME = "dismissed_prompt_time_ms"
    private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    private const val KEY_MUTE_PERMANENTLY = "mute_permanently"
    private const val KEY_MUTE_UNTIL = "mute_until"

    private lateinit var appContext: Context

    private val prefs: SharedPreferences by lazy {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isNotificationsEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun isMutePermanently(): Boolean {
        return prefs.getBoolean(KEY_MUTE_PERMANENTLY, false)
    }

    fun setMutePermanently(mute: Boolean) {
        prefs.edit().putBoolean(KEY_MUTE_PERMANENTLY, mute).apply()
    }

    fun getMuteUntil(): Long {
        return prefs.getLong(KEY_MUTE_UNTIL, 0L)
    }

    fun setMuteUntil(timestamp: Long) {
        prefs.edit().putLong(KEY_MUTE_UNTIL, timestamp).apply()
    }

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun getDeviceId(): String {
        var id = prefs.getString(KEY_DEVICE_ID, null)
        if (id.isNullOrBlank()) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        }
        return id
    }

    fun getPendingToken(): String? {
        return prefs.getString(KEY_PENDING_TOKEN, null)
    }

    fun savePendingToken(token: String) {
        prefs.edit().putString(KEY_PENDING_TOKEN, token).apply()
    }

    fun clearPendingToken() {
        prefs.edit().remove(KEY_PENDING_TOKEN).apply()
    }

    fun saveDismissedPromptTime() {
        prefs.edit().putLong(KEY_DISMISSED_TIME, System.currentTimeMillis()).apply()
    }

    fun shouldShowSoftPrompt(): Boolean {
        // 1. Android 13+ only
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return false
        }
        
        // 2. Permission not already granted
        val hasPermission = ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            return false
        }

        // 3. User is logged in
        val uid = FirebaseProviders.auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            return false
        }

        // 4. Has not dismissed recently (within 24 hours)
        val lastDismissed = prefs.getLong(KEY_DISMISSED_TIME, 0L)
        val oneDayMillis = 24 * 60 * 60 * 1000L
        return (System.currentTimeMillis() - lastDismissed) > oneDayMillis
    }

    fun handleNewToken(token: String) {
        val uid = FirebaseProviders.auth.currentUser?.uid
        if (!uid.isNullOrBlank()) {
            registerToken(uid, token)
        } else {
            savePendingToken(token)
        }
    }

    fun uploadPendingToken(userId: String) {
        val pendingToken = getPendingToken()
        if (!pendingToken.isNullOrBlank()) {
            registerToken(userId, pendingToken) { success ->
                if (success) {
                    clearPendingToken()
                }
            }
        }
    }

    fun registerCurrentToken(userId: String) {
        // 1. Upload any pending tokens first
        uploadPendingToken(userId)

        // 2. Fetch current token and register it
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                if (!token.isNullOrBlank()) {
                    registerToken(userId, token)
                }
            } else {
                android.util.Log.w("FcmManager", "Fetching FCM registration token failed", task.exception)
            }
        }
    }

    fun registerToken(userId: String, token: String, onComplete: ((Boolean) -> Unit)? = null) {
        val deviceId = getDeviceId()
        val appVersion = try {
            appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName
        } catch (e: Exception) {
            "1.0"
        }

        val tokenData = hashMapOf(
            "token" to token,
            "platform" to "android",
            "updatedAt" to FieldValue.serverTimestamp(),
            "deviceName" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "appVersion" to appVersion
        )

        FirebaseProviders.firestore
            .collection(FirestoreCollections.USERS)
            .document(userId)
            .collection(FirestoreCollections.FCM_TOKENS)
            .document(deviceId)
            .set(tokenData)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    android.util.Log.d("FcmManager", "FCM token registered successfully for user $userId")
                    onComplete?.invoke(true)
                } else {
                    android.util.Log.e("FcmManager", "FCM token registration failed", task.exception)
                    onComplete?.invoke(false)
                }
            }
    }

    fun deleteDeviceToken() {
        val uid = FirebaseProviders.auth.currentUser?.uid ?: return
        val deviceId = getDeviceId()
        
        FirebaseProviders.firestore
            .collection(FirestoreCollections.USERS)
            .document(uid)
            .collection(FirestoreCollections.FCM_TOKENS)
            .document(deviceId)
            .delete()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    android.util.Log.d("FcmManager", "Device token deleted from Firestore successfully")
                } else {
                    android.util.Log.e("FcmManager", "Failed to delete device token from Firestore", task.exception)
                }
            }
    }
}
