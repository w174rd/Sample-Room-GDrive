package com.w174rd.sample_room_gdrive.viewmodel

import android.accounts.Account
import android.app.Activity
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.w174rd.sample_room_gdrive.model.Meta
import com.w174rd.sample_room_gdrive.model.OnResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GoogleDriveViewModel: ViewModel() {

    val onResponse = MutableLiveData<OnResponse<Any>>()

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

        onResponse.postValue(OnResponse.loading())

        val dbFile = java.io.File(activity.getDatabasePath("sample-db").absolutePath)
        val dbFileShm = java.io.File(activity.getDatabasePath("sample-db-shm").absolutePath)
        val dbFileWal = java.io.File(activity.getDatabasePath("sample-db-wal").absolutePath)

        if (!dbFile.exists()) {
            Log.e("GoogleDriveHelper", dbFile.path)
            onResponse.postValue(OnResponse.error(Meta(error = 1, code = 0, message = "dbFile no exist")))
            return
        }

        val driveService = getGoogleDriveService(activity = activity)

        val fileMetadata = File().apply {
            name = "sample-db"
            parents = listOf("appDataFolder")
        }

        val fileMetadataShm = File().apply {
            name = "sample-db-shm"
            parents = listOf("appDataFolder")
        }

        val fileMetadataWal = File().apply {
            name = "sample-db-wal"
            parents = listOf("appDataFolder")
        }

        val mediaContent = FileContent("", dbFile)
        val mediaContentShm = FileContent("", dbFileShm)
        val mediaContentWal = FileContent("", dbFileWal)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute()

                val fileShm = driveService.files().create(fileMetadataShm, mediaContentShm)
                    .setFields("id")
                    .execute()

                val fileWal = driveService.files().create(fileMetadataWal, mediaContentWal)
                    .setFields("id")
                    .execute()

                onResponse.postValue(OnResponse.success("File uploaded: \ndb: ${file.name} \nshm: ${fileShm.name} \nwal: ${fileWal.name}"))
            } catch (e: Exception) {
                onResponse.postValue(OnResponse.error(Meta(error = 1, code = 0, message = "Error uploading file: ${e.message}")))
            }
        }
    }
}