package com.w174rd.sample_room_gdrive.viewmodel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.w174rd.sample_room_gdrive.R
import com.w174rd.sample_room_gdrive.model.Meta
import com.w174rd.sample_room_gdrive.model.OnResponse

class SignInV2ViewModel: ViewModel() {

    val onResponse = MutableLiveData<OnResponse<Any>>()
    private var mGoogleSignInClient: GoogleSignInClient? = null

    fun initialGoogleAccount(context: Context) {
        context.let {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(it.getString(R.string.default_web_client_id))
                .requestEmail()
                .requestScopes(
                    Scope("https://www.googleapis.com/auth/drive.file"),
                    Scope("https://www.googleapis.com/auth/drive.appdata")

                )
                .build()

            mGoogleSignInClient = GoogleSignIn.getClient(it, gso)

            val account = GoogleSignIn.getLastSignedInAccount(it)
            if (account != null) {
                Log.d("GO-TKN-init", account.idToken.toString())
            }
        }
    }

    fun signIn(activityResult: ActivityResultLauncher<Intent>?) {
        try {
            activityResult?.let {
                val signInIntent = mGoogleSignInClient?.signInIntent
                it.launch(signInIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleSignInResult(activity: Activity, completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            account?.let {
                val idToken = it.idToken // Token OAuth 2.

                if (idToken != null) {
                    firebaseAuthWithGoogle(activity, idToken)
                } else {
                    onResponse.postValue(OnResponse.error(Meta(error = 1, code = 0, message = "token is null")))
                }

                Log.d("GO-TKN-result", idToken.toString())
            }
        } catch (e: ApiException) {
            onResponse.postValue(OnResponse.error(Meta(error = 1, code = 0, message = e.message.toString())))
        }
    }

    private fun firebaseAuthWithGoogle(activity: Activity, idToken: String) {
        onResponse.postValue(OnResponse.loading())
        val auth = FirebaseAuth.getInstance()
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener(activity) { task ->
            if (task.isSuccessful) {
                onResponse.postValue(OnResponse.success(idToken))
            } else {
                onResponse.postValue(OnResponse.error(Meta(error = 1, code = 0, message = task.exception.toString())))
            }
        }.addOnFailureListener(activity) {
            onResponse.postValue(OnResponse.error(Meta(error = 1, code = 0, message = it.message.toString())))
        }
    }

    fun signOutGoogle() {
        mGoogleSignInClient?.signOut()?.addOnCompleteListener {}
        val auth = FirebaseAuth.getInstance()
        auth.signOut()
    }

    fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1000) {
            /** GOOGLE */
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            if (task.isSuccessful) handleSignInResult(activity, task)
        }
    }

}