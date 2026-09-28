package com.example.auth

data class AuthUser(
    val uid: String,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null
)

sealed class AuthState {
    object SignedOut : AuthState()
    object SigningIn : AuthState()
    data class SignedIn(val user: AuthUser) : AuthState()
    data class Error(val message: String) : AuthState()
}
