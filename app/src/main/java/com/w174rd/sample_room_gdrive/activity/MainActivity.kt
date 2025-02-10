package com.w174rd.sample_room_gdrive.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
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
import com.w174rd.sample_room_gdrive.model.OnResponse
import com.w174rd.sample_room_gdrive.utils.Functions.dismissProgressDialog
import com.w174rd.sample_room_gdrive.utils.Functions.getDataMeta
import com.w174rd.sample_room_gdrive.utils.Functions.registerForResult
import com.w174rd.sample_room_gdrive.utils.Functions.showAlertDialog
import com.w174rd.sample_room_gdrive.utils.Functions.showProgressDialog
import com.w174rd.sample_room_gdrive.viewmodel.DatabaseViewModel
import com.w174rd.sample_room_gdrive.viewmodel.GoogleDriveViewModel
import com.w174rd.sample_room_gdrive.viewmodel.SignInViewModel

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    lateinit var db: DataBase
    lateinit var mAdapter: LocalDataAdapter

    private lateinit var binding: ActivityMainBinding

    private val viewModelAuth by lazy {
        ViewModelProvider(this)[SignInViewModel::class.java]
    }

    private val viewModelDB by lazy {
        ViewModelProvider(this)[DatabaseViewModel::class.java]
    }

    private val viewModelGDrive by lazy {
        ViewModelProvider(this)[GoogleDriveViewModel::class.java]
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

            btnBackup.setOnClickListener {
                viewModelGDrive.uploadDatabaseToDrive(activity = this@MainActivity)
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
                    showProgressDialog(this@MainActivity)
                }
                OnResponse.SUCCESS -> {
                    dismissProgressDialog()
//                    showAlertDialog(context = this, title = "Success", message = it.data.toString())
                    Log.d("TOKEN", it.data.toString())
                    checkAuth()
                }
                OnResponse.ERROR -> {
                    dismissProgressDialog()
                    checkAuth()
                    viewModelAuth.signOutGoogle()
                    val error = getDataMeta(this@MainActivity, it.error)
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
}