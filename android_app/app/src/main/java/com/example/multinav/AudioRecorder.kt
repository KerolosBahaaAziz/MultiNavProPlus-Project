package com.example.multinav

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import java.io.File
import java.io.FileInputStream

class AudioRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isRecording: Boolean = false

    @SuppressLint("MissingPermission")
    fun startRecording(): Boolean {
        return try {
            // Create a temporary file in the cache directory
            audioFile = File(context.cacheDir, "voice_message.3gp").apply {
                delete() // Ensure the file doesn't already exist
            }

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }

            isRecording = true
            Log.d("AudioRecorder", "Started recording voice message")
            true
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to start recording", e)
            isRecording = false
            audioFile?.delete()
            audioFile = null
            false
        }
    }

    fun stopRecording(): ByteArray {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            Log.d("AudioRecorder", "Stopped recording voice message")

            // Read the recorded audio as a byte array
            val audioBytes = audioFile?.let { file ->
                if (file.exists()) {
                    FileInputStream(file).use { it.readBytes() }
                } else {
                    Log.e("AudioRecorder", "Audio file does not exist: ${file.absolutePath}")
                    byteArrayOf()
                }
            } ?: byteArrayOf()

            // Delete the temporary file
            audioFile?.delete()
            audioFile = null

            audioBytes
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to stop recording", e)
            isRecording = false
            audioFile?.delete()
            audioFile = null
            byteArrayOf()
        }
    }

    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            audioFile?.delete()
            audioFile = null
            Log.d("AudioRecorder", "Canceled recording voice message")
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to cancel recording", e)
            isRecording = false
            audioFile?.delete()
            audioFile = null
        }
    }

    fun isRecording(): Boolean = isRecording

    fun release() {
        if (isRecording) {
            try {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
            } catch (e: Exception) {
                Log.w("AudioRecorder", "Error during release", e)
            }
            audioFile?.delete()
        }
        mediaRecorder = null
        isRecording = false
        audioFile = null
        Log.d("AudioRecorder", "Released AudioRecorder resources")
    }
}