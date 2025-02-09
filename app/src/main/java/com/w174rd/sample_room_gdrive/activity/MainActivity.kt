package com.w174rd.sample_room_gdrive.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.w174rd.sample_room_gdrive.R
import com.w174rd.sample_room_gdrive.adaptor.LocalDataAdapter
import com.w174rd.sample_room_gdrive.databinding.ActivityMainBinding
import com.w174rd.sample_room_gdrive.db.DataBase
import com.w174rd.sample_room_gdrive.model.Entity
import com.w174rd.sample_room_gdrive.model.Meta
import com.w174rd.sample_room_gdrive.model.OnResponse
import com.w174rd.sample_room_gdrive.viewmodel.DatabaseViewModel
import com.w174rd.sample_room_gdrive.viewmodel.SignInV2ViewModel

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    lateinit var db: DataBase
    lateinit var mAdapter: LocalDataAdapter

    private lateinit var binding: ActivityMainBinding

    private val viewModelAuth by lazy {
        ViewModelProvider(this)[SignInV2ViewModel::class.java]
    }

    private val viewModelDB by lazy {
        ViewModelProvider(this)[DatabaseViewModel::class.java]
    }

    private val onActionResultGoogle = registerForResult { requestCode, resultCode, data ->
        viewModelAuth.onActivityResult(
            activity = this,
            requestCode = 1000,
            resultCode = resultCode,
            data = data
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Contruct view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Init Room DB
        db = Room.databaseBuilder(applicationContext, DataBase::class.java, "sample-db").build()

        viewModelAuth.initialGoogleAccount(context = this)

        setupRecycler()
        checkAuth()
        onClick()
        initViewModel()
    }

    private fun onClick() {
        binding.apply {
            btnAddData.setOnClickListener {
                print(etData.text.toString())
                viewModelDB.insertData(db, Entity(name = etData.text.toString()))
            }

            btnLogin.setOnClickListener {
//                viewModelAuth.signIn(this@MainActivity)
                viewModelAuth.signIn(activityResult = onActionResultGoogle)
            }

            btnLogout.setOnClickListener {
                viewModelAuth.signOutGoogle()
                checkAuth()
            }
        }
    }

    private fun checkAuth() {
        val user = Firebase.auth.currentUser
        if (user != null) {
            binding.btnLogin.visibility = View.GONE
            binding.groupLogin.visibility = View.VISIBLE

            binding.txtEmail.text = resources.getString(R.string.account_, user.email)
        } else {
            binding.btnLogin.visibility = View.VISIBLE
            binding.groupLogin.visibility = View.GONE
        }
    }

    private fun initViewModel() {
        /** ============ DATA BASE ============== */
        viewModelDB.getData(db = db)
        viewModelDB.responseData.observe(this) {
            mAdapter.submitList(it)
            binding.etData.text?.clear()
        }

        /** ============ FIREBASE AUTH ============== */
        viewModelAuth.onResponse.observe(this) {
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
                    viewModelAuth.signOutGoogle()
                    val error = getDataMeta(it.error)
                    showAlertDialog(context = this, title = "Error", message = error.message)
                }
            }
        }
    }

    private fun setupRecycler() {
        mAdapter = LocalDataAdapter()

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = mAdapter
        }
    }


    /** ============================================================================================ */

    fun showAlertDialog(context: Context, title: String, message: String? = "") {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Ok") { dialog, _ ->
                dialog.dismiss()
            }
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

    private fun AppCompatActivity.registerForResult(onResult: (requestCode: Int, resultCode: Int, data: Intent?) -> Unit): ActivityResultLauncher<Intent> {
        return this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val intent = result.data
            val requestCode = intent?.getIntExtra("REQUEST_CODE", -1) ?: -1
            onResult(requestCode, result.resultCode, intent)
        }
    }
}