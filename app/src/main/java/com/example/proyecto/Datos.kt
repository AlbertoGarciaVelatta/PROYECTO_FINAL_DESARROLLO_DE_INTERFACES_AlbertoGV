package com.example.proyecto

import com.google.firebase.firestore.IgnoreExtraProperties

enum class Allergen { GLUTEN, LACTOSE, NUTS, SHELLFISH, EGGS, FISH }

@IgnoreExtraProperties
data class Product(
    val id: String = "",
    val name: String = "",
    val store: String = "Tienda Externa",
    val allergens: List<String> = emptyList(),
    val isVerified: Boolean = false,
    val imageUrl: String = ""
)

data class UserProfile(
    val uid: String = "",
    val nombre: String = "",
    val alergias: List<String> = emptyList(),
    val role: String = "Explorador"
)

data class GroupProfile(
    val id: String = "",
    val nombre: String = "",
    val adminId: String = "",
    val miembros: List<String> = emptyList()
)

data class HistoryItem(
    val name: String = "",
    val isApto: Boolean = false,
    val timestamp: com.google.firebase.Timestamp? = null
)