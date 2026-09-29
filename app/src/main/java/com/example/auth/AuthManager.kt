package com.example.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.data.AppContainer
import com.example.sync.FirebaseConfigHelper
import com.example.sync.SyncState
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitAuth(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            continuation.resume(result)
        }
        addOnFailureListener { exception ->
            continuation.resumeWith(kotlin.Result.failure(exception))
        }
    }

class AuthManager(private val context: Context) {

    private val _authState = MutableStateFlow<AuthState>(AuthState.SignedOut)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        val user = auth.currentUser
        if (user != null) {
            val authUser = user.toAuthUser()
            _authState.value = AuthState.SignedIn(authUser)
            AppContainer.syncPreferences.setCurrentUser(authUser.uid, authUser.email)
        } else {
            _authState.value = AuthState.SignedOut
            AppContainer.syncPreferences.setCurrentUser(null, null)
            AppContainer.syncPreferences.updateSyncState(
                SyncState.SIGNED_OUT,
                isConfigured = true,
                errorMessage = null
            )
        }
    }

    init {
        checkInitialAuthState()
    }

    private fun checkInitialAuthState() {
        if (!FirebaseConfigHelper.isFirebaseConfigured(context)) {
            _authState.value = AuthState.SignedOut
            return
        }

        try {
            val auth = FirebaseAuth.getInstance()
            auth.addAuthStateListener(authListener)
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val authUser = currentUser.toAuthUser()
                _authState.value = AuthState.SignedIn(authUser)
                AppContainer.syncPreferences.setCurrentUser(authUser.uid, authUser.email)
            } else {
                _authState.value = AuthState.SignedOut
            }
        } catch (_: Throwable) {
            _authState.value = AuthState.SignedOut
        }
    }

    val currentUser: AuthUser?
        get() = when (val state = _authState.value) {
            is AuthState.SignedIn -> state.user
            else -> null
        }

    val isUserSignedIn: Boolean
        get() = _authState.value is AuthState.SignedIn

    suspend fun signInWithGoogle(activity: Activity): Result<AuthUser> = withContext(Dispatchers.Main) {
        if (!FirebaseConfigHelper.isFirebaseConfigured(context)) {
            val errorMsg = "Firebase is not configured. Please ensure google-services.json is valid."
            _authState.value = AuthState.Error(errorMsg)
            return@withContext Result.failure(Exception(errorMsg))
        }

        val serverClientId = AuthPreferences.resolveServerClientId(context)
        if (serverClientId.isNullOrBlank()) {
            val errorMsg = "MISSING_CLIENT_ID"
            _authState.value = AuthState.Error("Web Client ID is required for Google Sign-In.")
            return@withContext Result.failure(Exception(errorMsg))
        }

        _authState.value = AuthState.SigningIn

        try {
            val credentialManager = CredentialManager.create(activity)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(
                request = request,
                context = activity
            )

            val credential = response.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = FirebaseAuth.getInstance().signInWithCredential(authCredential).awaitAuth()
                val user = authResult.user

                if (user != null) {
                    val authUser = user.toAuthUser()
                    _authState.value = AuthState.SignedIn(authUser)
                    AppContainer.syncPreferences.setCurrentUser(authUser.uid, authUser.email)

                    // Keep syncing automatically from now on (no app restart needed)
                    com.example.sync.SyncWorker.schedulePeriodicSync(context)

                    // Trigger cloud sync in background with the new UID
                    try {
                        AppContainer.syncManager.sync()
                    } catch (_: Exception) {}

                    Result.success(authUser)
                } else {
                    val errorMsg = "Authentication completed but user profile was not returned."
                    _authState.value = AuthState.Error(errorMsg)
                    Result.failure(Exception(errorMsg))
                }
            } else {
                val errorMsg = "Unsupported credential format received."
                _authState.value = AuthState.Error(errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: GetCredentialCancellationException) {
            // User backed out or cancelled Google Sign-In dialog
            _authState.value = AuthState.SignedOut
            Result.failure(Exception("CANCELLED"))
        } catch (e: NoCredentialException) {
            val errorMsg = "No Google accounts available on this device."
            _authState.value = AuthState.Error(errorMsg)
            Result.failure(Exception(errorMsg))
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Failed to sign in with Google."
            _authState.value = AuthState.Error(errorMsg)
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            if (FirebaseConfigHelper.isFirebaseConfigured(context)) {
                FirebaseAuth.getInstance().signOut()
            }
        } catch (_: Throwable) {}

        _authState.value = AuthState.SignedOut
        AppContainer.syncPreferences.setCurrentUser(null, null)
        AppContainer.syncPreferences.updateSyncState(
            SyncState.SIGNED_OUT,
            isConfigured = true,
            errorMessage = null
        )
        // All Room data remains untouched!
    }

    private fun FirebaseUser.toAuthUser(): AuthUser {
        return AuthUser(
            uid = uid,
            email = email,
            displayName = displayName,
            photoUrl = photoUrl?.toString()
        )
    }
}
