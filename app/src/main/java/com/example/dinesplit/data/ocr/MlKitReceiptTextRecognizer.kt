package com.example.dinesplit.data.ocr

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.Closeable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MlKitReceiptTextRecognizer(
    private val context: Context,
) : Closeable {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognize(uri: Uri): String {
        val image = InputImage.fromFilePath(context, uri)
        return recognizer.process(image).awaitTask().text
    }

    override fun close() {
        recognizer.close()
    }

    private suspend fun <T> Task<T>.awaitTask(): T {
        return suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    continuation.resumeWithException(
                        task.exception ?: IllegalStateException("ML Kit text recognition failed"),
                    )
                }
            }
        }
    }
}
