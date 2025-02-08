package com.w174rd.sample_room_gdrive.activity

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.w174rd.sample_room_gdrive.R
import com.w174rd.sample_room_gdrive.databinding.ActivityMainBinding
import com.w174rd.sample_room_gdrive.model.Meta
import com.w174rd.sample_room_gdrive.model.OnResponse
import com.w174rd.sample_room_gdrive.viewmodel.SignInViewModel

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    private lateinit var binding: ActivityMainBinding

    private val viewModel by lazy {
        ViewModelProvider(this)[SignInViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkAuth()

        binding.btnLogin.setOnClickListener {
            viewModel.signIn(this)
        }

        binding.btnLogout.setOnClickListener {
            viewModel.signOutGoogle()
            checkAuth()
        }

        initViewModel()
    }

    private fun checkAuth() {
        val user = Firebase.auth.currentUser
        if (user != null) {
            binding.btnLogin.visibility = View.GONE
            binding.btnLogout.visibility = View.VISIBLE
        } else {
            binding.btnLogin.visibility = View.VISIBLE
            binding.btnLogout.visibility = View.GONE
        }
    }

    private fun initViewModel() {
        viewModel.onResponse.observe(this) {
            when(it.status) {
                OnResponse.LOADING -> {
                    Toast.makeText(this, "Loading...", Toast.LENGTH_SHORT).show()
                }
                OnResponse.SUCCESS -> {
                    showAlertDialog(context = this, title = "Success", message = it.data.toString())
                    checkAuth()
                }
                OnResponse.ERROR -> {
                    checkAuth()
                    val error = getDataMeta(it.error)
                    showAlertDialog(context = this, title = "Error", message = error.message)
                }
            }
        }
    }

    fun showAlertDialog(context: Context, title: String, message: String? = "") {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Ok") { dialog, _ ->
                dialog.dismiss()
            }
//            .setNegativeButton("Batal") { dialog, _ ->
//                dialog.dismiss()
//            }
            .show()
    }


    private fun getDataMeta(data: Any?): Meta {
        var dataMeta = Meta()
        dataMeta.error = 1
        dataMeta.code = 0
        dataMeta.message = resources.getString(R.string.unknown)

        try {
            dataMeta = data as Meta
        } catch (e: Exception) {
            Toast.makeText(this, "getDataMeta() ${e.message}", Toast.LENGTH_SHORT).show()
        }

        return dataMeta
    }
}