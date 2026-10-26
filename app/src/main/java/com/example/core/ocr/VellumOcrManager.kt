package com.example.core.ocr

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

data class OcrExtractionResult(
    val fullText: String,
    val lineCount: Int,
    val characterCount: Int,
    val blocks: List<String>
)

object VellumOcrManager {

    private const val TAG = "VellumOcrManager"

    /**
     * Extracts text from an image URI completely on-device using ML Kit Text Recognition.
     * Never uploads image or extracted content to any server or cloud service.
     */
    suspend fun extractTextFromImageUri(
        context: Context,
        imageUri: Uri
    ): Result<OcrExtractionResult> = withContext(Dispatchers.IO) {
        try {
            val inputImage = try {
                InputImage.fromFilePath(context, imageUri)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load input image from URI", e)
                return@withContext Result.failure(
                    IllegalArgumentException("Could not open selected image file: ${e.message ?: "File unreadable"}")
                )
            }

            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            suspendCancellableCoroutine { continuation ->
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val rawText = visionText.text.trim()
                        if (rawText.isBlank()) {
                            continuation.resume(
                                Result.failure(IllegalStateException("No readable text could be detected in this image."))
                            )
                        } else {
                            val blocks = visionText.textBlocks.mapNotNull { block ->
                                block.text.trim().takeIf { it.isNotEmpty() }
                            }
                            val lines = rawText.lines().filter { it.isNotBlank() }
                            val result = OcrExtractionResult(
                                fullText = rawText,
                                lineCount = lines.size,
                                characterCount = rawText.length,
                                blocks = blocks
                            )
                            continuation.resume(Result.success(result))
                        }
                    }
                    .addOnFailureListener { error ->
                        Log.e(TAG, "On-device OCR processing error", error)
                        continuation.resume(
                            Result.failure(Exception("Text recognition processing failed: ${error.message ?: "Unknown error"}"))
                        )
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during text extraction", e)
            Result.failure(e)
        }
    }
}
