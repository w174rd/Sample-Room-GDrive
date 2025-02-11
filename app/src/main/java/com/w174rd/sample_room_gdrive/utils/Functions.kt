package com.w174rd.sample_room_gdrive.utils

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.w174rd.sample_room_gdrive.R
import com.w174rd.sample_room_gdrive.databinding.DialogProgressBinding
import com.w174rd.sample_room_gdrive.model.Meta

object Functions {

    lateinit var dialog: AlertDialog

    fun showProgressDialog(context: Context, cancelable: Boolean? = true): Dialog {
        val binding = DialogProgressBinding.inflate(LayoutInflater.from(context))

        dialog = MaterialAlertDialogBuilder(context)
            .setView(binding.root)
            .setCancelable(cancelable ?: true)
            .create()
            dialog.show()
        return dialog
    }

    fun dismissProgressDialog() {
        if (this::dialog.isInitialized) {
            dialog.dismiss()
        }
    }

    fun showAlertDialog(context: Context, title: String, message: String? = "") {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Ok") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


    fun getDataMeta(context: Context, data: Any?): Meta {
        var dataMeta = Meta()
        dataMeta.error = 1
        dataMeta.code = 0
        dataMeta.message = context.resources.getString(R.string.unknown)

        try {
            dataMeta = data as Meta
        } catch (e: Exception) {
            Log.e("getDataMeta", e.message.toString())
        }

        return dataMeta
    }

    fun AppCompatActivity.registerForResult(onResult: (requestCode: Int, resultCode: Int, data: Intent?) -> Unit): ActivityResultLauncher<Intent> {
        return this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val intent = result.data
            val requestCode = intent?.getIntExtra("REQUEST_CODE", -1) ?: -1
            onResult(requestCode, result.resultCode, intent)
        }
    }

    fun getVersionName(): String {
        return "1.0.0"
    }
}