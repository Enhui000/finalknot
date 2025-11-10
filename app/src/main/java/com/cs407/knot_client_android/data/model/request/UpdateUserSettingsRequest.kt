package com.cs407.knot_client_android.data.model.request

data class UpdateUserSettingsRequest(
    val nickname: String,
    val statusMessage: String,
    val email: String,
    val gender: String,
    val birthdate: String,
    val privacyLevel: String,
    val discoverable: Boolean
)