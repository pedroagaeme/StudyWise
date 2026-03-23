package com.example.studywise.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException

fun Context.uriToFile(uri: Uri): File {
    val inputStream = contentResolver.openInputStream(uri)
        ?: throw IOException("Could not open input stream for $uri")

    val tempFile = File(cacheDir, "upload_${System.currentTimeMillis()}.tmp")

    tempFile.outputStream().use { output ->
        inputStream.use { input ->
            input.copyTo(output)
        }
    }
    return tempFile
}