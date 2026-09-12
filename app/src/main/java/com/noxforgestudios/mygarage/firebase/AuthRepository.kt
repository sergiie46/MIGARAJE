package com.noxforgestudios.mygarage.firebase

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.noxforgestudios.mygarage.domain.UserProfile
import kotlinx.coroutines.tasks.await

class AuthRepository(private val context: Context, private val auth: FirebaseAuth?) {
    val currentUser: FirebaseUser? get() = auth?.currentUser
    val isConfigured: Boolean get() = auth != null

    suspend fun signInWithGoogle(activity: Activity): Result<FirebaseUser> = runCatching {
        val firebaseAuth = auth ?: error("Firebase no está configurado. Añade app/google-services.json")
        val webClientIdRes = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (webClientIdRes == 0) error("Falta default_web_client_id. Descarga google-services.json tras activar Google Authentication")
        val webClientId = context.getString(webClientIdRes)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
        val manager = CredentialManager.create(activity)
        val result = try {
            manager.getCredential(activity, request)
        } catch (cancel: GetCredentialCancellationException) {
            throw SignInCancelledException()
        }
        val credential = result.credential
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            error("La credencial devuelta por Google no es compatible")
        }
        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
        firebaseAuth.signInWithCredential(firebaseCredential).await().user ?: error("Firebase no devolvió usuario")
    }

    suspend fun signOut(): Result<Unit> = runCatching {
        auth?.signOut()
        CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
    }

    suspend fun deleteCurrentUser(): Result<Unit> = runCatching {
        val user = auth?.currentUser ?: return@runCatching
        user.delete().await()
        CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
    }

    fun profile(user: FirebaseUser): UserProfile = UserProfile(
        uid = user.uid,
        displayName = user.displayName.orEmpty(),
        email = user.email.orEmpty(),
        photoUrl = user.photoUrl?.toString()
    )
}

class SignInCancelledException : Exception("Inicio de sesión cancelado")
