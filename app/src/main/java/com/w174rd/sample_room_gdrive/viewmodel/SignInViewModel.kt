package com.w174rd.sample_room_gdrive.viewmodel

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.w174rd.sample_room_gdrive.R
import com.w174rd.sample_room_gdrive.model.Meta
import com.w174rd.sample_room_gdrive.model.OnResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignInViewModel : ViewModel(){

    val onResponse = MutableLiveData<OnResponse<Any>>()

    fun signIn(activity: Activity) {
        val credentialManager = CredentialManager.create(activity) //import from androidx.CredentialManager

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(activity.resources.getString(R.string.default_web_client_id)) //from https://console.firebase.google.com/project/my-firebase-chat-2aac3/authentication/providers
            .build()

        val request = GetCredentialRequest.Builder() //import from androidx.CredentialManager
            .addCredentialOption(googleIdOption)
            .build()

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val result: GetCredentialResponse = credentialManager.getCredential( //import from androidx.CredentialManager
                    request = request,
                    context = activity,
                )
                handleSignIn(activity, result)
            } catch (e: GetCredentialException) { //import from androidx.CredentialManager
                onResponse.postValue(OnResponse.error(Meta(error = 1, code = 0, message = "signIn() ${e.message.toString()}")))
            }
        }
    }

    private fun handleSignIn(activity: Activity, result: GetCredentialResponse) {
        // Handle the successfully returned credential.
        when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        // Use googleIdTokenCredential and extract id to validate and authenticate on your server.
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        firebaseAuthWithGoogle(activity, googleIdTokenCredential.idToken)
                    } catch (e: GoogleIdTokenParsingException) {
                        onResponse.postValue(OnResponse.error(Meta(error = 1, code = 0, message = "handleSignIn() Received an invalid google id token response")))
                    }
                } else {
                    // Catch any unrecognized custom credential type here.
                    onResponse.postValue(OnResponse.error(Meta(error = 1, code = 0, message = "handleSignIn() Unexpected type of credential")))
                }
            }

            else -> {
                // Catch any unrecognized credential type here.
                onResponse.postValue(OnResponse.error(Meta(error = 1, code = 0, message = "handleSignIn() Unexpected type of credential")))
            }
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
                onResponse.postValue(OnResponse.error(Meta(error = 1, code = 0, message = "firebaseAuthWithGoogle() ${task.exception.toString()}")))
            }
        }.addOnFailureListener(activity) {
            onResponse.postValue(OnResponse.error(Meta(error = 1, code = 0, message = "firebaseAuthWithGoogle() ${it.message.toString()}")))
        }
    }

    fun signOutGoogle() {
        val auth = FirebaseAuth.getInstance()
        auth.signOut()
    }
}