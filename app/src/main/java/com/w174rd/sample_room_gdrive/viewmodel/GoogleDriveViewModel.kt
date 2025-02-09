package com.w174rd.sample_room_gdrive.viewmodel

import android.accounts.Account
import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GoogleDriveViewModel: ViewModel() {

    private fun getGoogleDriveService(activity: Activity): Drive {
        val account = GoogleSignIn.getLastSignedInAccount(activity)
//        val account = FirebaseAuth.getInstance().currentUser
        val credential = GoogleAccountCredential.usingOAuth2(
            activity, listOf(
                "https://www.googleapis.com/auth/drive.appdata",
                "https://www.googleapis.com/auth/drive.file"
            )
        )
        credential.selectedAccount = Account(account?.email ?: "", "com.google")

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Sample Room GDrive")
            .build()
    }

    fun uploadDatabaseToDrive(activity: Activity) {
        val dbFile = java.io.File(activity.getDatabasePath("sample-db").absolutePath)

        if (!dbFile.exists()) {
            Log.e("GoogleDriveHelper", dbFile.path)
            return
        }

        val driveService = getGoogleDriveService(activity = activity)

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