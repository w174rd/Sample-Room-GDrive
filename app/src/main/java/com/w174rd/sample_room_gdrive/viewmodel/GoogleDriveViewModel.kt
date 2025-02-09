package com.w174rd.sample_room_gdrive.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GoogleDriveViewModel: ViewModel() {

    // Ganti AndroidHttp dengan OkHttp untuk transport
//    val httpTransport: HttpTransport = NetHttpTransport()

    private fun getGoogleDriveService(token: String): Drive {
//        val credential = GoogleCredential().setAccessToken(token)
//
////        val transport = AndroidHttp.newCompatibleTransport()
//        val transport = NetHttpTransport()
//        val jsonFactory = JacksonFactory.getDefaultInstance()
//
//        return Drive.Builder(transport, jsonFactory, credential)
//            .setApplicationName("Sample Room GDrive")
//            .build()


//        val requestInitializer = HttpRequestInitializer { request ->
//            request.headers.authorization = "Bearer $token"
//        }
//
//        return Drive.Builder(httpTransport, GsonFactory(), requestInitializer)
//            .setApplicationName("Sample Room GDrive")
//            .build()

        val credential = GoogleCredential().apply {
            setAccessToken(token)
        }

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Sample Room GDrive")
            .build()
    }

    private fun getFirebaseToken(callback: (String?) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result?.token
                callback(token)
            } else {
                Log.e("FirebaseAuth", "Error getting token: ${task.exception}")
                callback(null)
            }
        }
    }

    fun uploadDatabaseToDrive(context: Context) {
        val dbFile = java.io.File(context.getDatabasePath("sample-db").absolutePath)

        if (!dbFile.exists()) {
            Log.e("GoogleDriveHelper", dbFile.path)
            return
        }

        getFirebaseToken { token ->
            if (token == null) {
                Log.e("GoogleDriveHelper", "Failed to get Firebase token")
                return@getFirebaseToken
            }

            val driveService = getGoogleDriveService(token)

            val fileMetadata = File().apply {
                name = "backup_room_db.sqlite"
                parents = listOf("appDataFolder") // Simpan di folder aplikasi di Google Drive
            }

            val mediaContent = FileContent("", dbFile)

            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val file = driveService.files().create(fileMetadata, mediaContent)
                        .setFields("id")
                        .execute()
                    Log.d("GoogleDriveHelper", "File uploaded: ${file.id}")
                } catch (e: Exception) {
                    Log.e("GoogleDriveHelper", "Error uploading file: ${e.message}")
                }
            }
        }
    }
}