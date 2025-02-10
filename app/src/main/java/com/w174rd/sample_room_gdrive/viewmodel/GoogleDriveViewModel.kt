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
import com.w174rd.sample_room_gdrive.utils.Attributes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileOutputStream


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

    // Upload database to Google Drive

    fun uploadDatabaseToDrive(activity: Activity) {

        onResponse.postValue(OnResponse.loading())

        val dbFile = java.io.File(activity.getDatabasePath(Attributes.database.name).absolutePath)
        val dbFileShm = java.io.File(activity.getDatabasePath("${Attributes.database.name}-shm").absolutePath)
        val dbFileWal = java.io.File(activity.getDatabasePath("${Attributes.database.name}-wal").absolutePath)

        if (!dbFile.exists()) {
            Log.e("GoogleDriveHelper", dbFile.path)
            onResponse.postValue(OnResponse.error(Meta(error = 1, code = 0, message = "dbFile no exist")))
            return
        }

        val driveService = getGoogleDriveService(activity = activity)

        val fileMetadata = File().apply {
            name = Attributes.database.name
            parents = listOf("appDataFolder")
        }

        val fileMetadataShm = File().apply {
            name = "${Attributes.database.name}-shm"
            parents = listOf("appDataFolder")
        }

        val fileMetadataWal = File().apply {
            name = "${Attributes.database.name}-wal"
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

                onResponse.postValue(OnResponse.success("Database uploaded successfully"))
            } catch (e: Exception) {
                onResponse.postValue(OnResponse.error(Meta(error = 1, code = 0, message = "Error uploading file: ${e.message}")))
            }
        }
    }

    // Upload database to Google Drive

    fun downloadDatabaseFromDrive(activity: Activity) {
        onResponse.postValue(OnResponse.loading())

        val driveService = getGoogleDriveService(activity)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Cari file di Google Drive (appDataFolder)
                val query = "name='${Attributes.database.name}' and 'appDataFolder' in parents"
                val result = driveService.files().list().setQ(query).setSpaces("appDataFolder").execute()

                if (result.files.isEmpty()) {
                    onResponse.postValue(OnResponse.error(Meta(error = 1, code = 0, message = "Database file not found in Drive")))
                    return@launch
                }

                val fileId = result.files[0].id
                val dbPath = activity.getDatabasePath(Attributes.database.name).absolutePath
                val outputStream = FileOutputStream(dbPath)

                // Unduh file dari Google Drive
                driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
                outputStream.close()

                // **Lakukan hal yang sama untuk file SHM & WAL**
                downloadFileFromDrive(driveService, activity, "${Attributes.database.name}-shm")
                downloadFileFromDrive(driveService, activity, "${Attributes.database.name}-wal")

                onResponse.postValue(OnResponse.success("Database restore successfully"))

            } catch (e: Exception) {
                onResponse.postValue(OnResponse.error(Meta(error = 1, code = 0, message = "Error restore file: ${e.message}")))
            }
        }
    }

    private fun downloadFileFromDrive(driveService: Drive, activity: Activity, fileName: String) {
        try {
            val query = "name='$fileName' and 'appDataFolder' in parents"
            val result = driveService.files().list().setQ(query).setSpaces("appDataFolder").execute()

            if (result.files.isNotEmpty()) {
                val fileId = result.files[0].id
                val filePath = activity.getDatabasePath(fileName).absolutePath
                val outputStream = FileOutputStream(filePath)

                driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
                outputStream.close()
            }
        } catch (e: Exception) {
            Log.e("GoogleDriveHelper", "Failed to download $fileName: ${e.message}")
        }
    }

}