package com.w174rd.sample_room_gdrive.viewmodel

import android.accounts.Account
import android.content.Context
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
import com.google.gson.Gson
import com.w174rd.sample_room_gdrive.db.DataBase
import com.w174rd.sample_room_gdrive.model.Entity
import com.w174rd.sample_room_gdrive.model.Meta
import com.w174rd.sample_room_gdrive.model.OnResponse
import com.w174rd.sample_room_gdrive.model.sync.DBVersino1
import com.w174rd.sample_room_gdrive.model.sync.EntityData
import com.w174rd.sample_room_gdrive.utils.Attributes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileOutputStream


class GoogleDriveViewModel: ViewModel() {

    val onResponse = MutableLiveData<OnResponse<Any>>()

    private fun getGoogleDriveService(context: Context): Drive {
        val account = GoogleSignIn.getLastSignedInAccount(context)
//        val account = FirebaseAuth.getInstance().currentUser
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(
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

    fun uploadDatabaseToDrive(context: Context, fileJson: java.io.File) {
        if (!fileJson.exists()) {
            Log.e("GoogleDriveHelper", fileJson.path)
            onResponse.postValue(OnResponse.error(Meta(error = 1, code = 0, message = "dbFile no exist")))
            return
        }

        val driveService = getGoogleDriveService(context = context)

        val fileMetadata = File().apply {
            name = Attributes.database.backup_name
            parents = listOf("appDataFolder")
        }

        val mediaContent = FileContent("", fileJson)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute()

                onResponse.postValue(OnResponse.success("Database uploaded successfully \n\n${file.id}"))
            } catch (e: Exception) {
                onResponse.postValue(OnResponse.error(Meta(error = 1, code = 0, message = "Error uploading file: ${e.message}")))
            }
        }
    }

    // Upload database to Google Drive

    fun downloadDatabaseFromDrive(db: DataBase, context: Context) {
        onResponse.postValue(OnResponse.loading())

        val driveService = getGoogleDriveService(context)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Cari file di Google Drive (appDataFolder)
                val query = "name='${Attributes.database.backup_name}' and 'appDataFolder' in parents"
                val result = driveService.files().list().setQ(query).setSpaces("appDataFolder").execute()

                if (result.files.isEmpty()) {
                    onResponse.postValue(OnResponse.error(Meta(error = 1, code = 0, message = "file db not found in Drive")))
                    return@launch
                }

                val existingFileJson = java.io.File(context.getDatabasePath(Attributes.database.backup_name).absolutePath)

                if (existingFileJson.exists()) {
                    existingFileJson.delete()
                }

                val fileId = result.files[0].id
                val dbPath = context.getDatabasePath(Attributes.database.backup_name).absolutePath
                val outputStream = FileOutputStream(dbPath)

                // Unduh file dari Google Drive
                driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
                outputStream.close()

                if (!existingFileJson.exists()) {
                    onResponse.postValue(OnResponse.error(Meta(error = 1, code = 0, message = "file db not found")))
                    return@launch
                }

                val resultdata = readAndSyncJsonFromFile(db = db, context = context)

                onResponse.postValue(OnResponse.success(resultdata))

            } catch (e: Exception) {
                onResponse.postValue(OnResponse.error(Meta(error = 1, code = 0, message = "Error restore file: ${e.message}")))
            }
        }
    }

    fun saveJsonToFile(context: Context, db: DataBase) {
        onResponse.postValue(OnResponse.loading())

        viewModelScope.launch(Dispatchers.IO) {
            val fileJson = java.io.File(context.getDatabasePath(Attributes.database.backup_name).absolutePath)
            try {
                if (fileJson.exists()) {
                    fileJson.delete()
                }

                val entityData = arrayListOf<EntityData>()
                val entityLocalData = db.entityDao().getAll()
                entityLocalData.forEach {
                    entityData.add(EntityData(id = it.id, value = it.name))
                }

                val jsonString = Gson().toJson(DBVersino1(
                    dbVersion = Attributes.database.version,
                    entityData = entityData
                ))
                fileJson.writeText(jsonString)
            } finally {
                uploadDatabaseToDrive(context = context, fileJson = fileJson)
            }
        }
    }

    fun readAndSyncJsonFromFile(db: DataBase, context: Context): String? {
        val fileJson = java.io.File(context.getDatabasePath(Attributes.database.backup_name).absolutePath)
        if (fileJson.exists()) {
            val jsonString = fileJson.readText()
            val driveData = Gson().fromJson(jsonString, DBVersino1::class.java)

            val localData = db.entityDao().getAll()

            // Sync logic
            if (driveData.dbVersion <= 1) {
                val localIds = localData.map { it.id }.toSet()
                val entityLocalData = arrayListOf<Entity>()

                driveData.entityData.forEach { drive ->
                    if (drive.id !in localIds) {
                        Log.e("SYNC_PROCESS", "Menambahkan drive.id ${drive.id} ke entityLocalData")
                        entityLocalData.add(
                            Entity(
                                id = drive.id,
                                name = drive.value
                            )
                        )
                    }
                }

                if (entityLocalData.isNotEmpty()) {
                    db.entityDao().insertAll(data = entityLocalData)
                    return "Added data: ${Gson().toJson(entityLocalData)}"
                } else {
                    return "No data to sync"
                }
            } else {
                return "Unknown Version"
            }
        } else return "File does not exist"
    }
}