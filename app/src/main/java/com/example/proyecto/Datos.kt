package com.example.proyecto

import com.google.firebase.firestore.IgnoreExtraProperties
//aqui se enumera los tipos de alergenos
enum class Allergen { GLUTEN, LACTOSE, NUTS, SHELLFISH, EGGS, FISH }

//aqui esta los modelos de datos
//aqui esta los productos
@IgnoreExtraProperties
data class Product(
    val id: String = "",
    val name: String = "",
    val store: String = "Tienda Externa",
    val allergens: List<String> = emptyList(),
    val isVerified: Boolean = false,
    val imageUrl: String = ""
)

//la clase de los usuarios
data class UserProfile(
    val uid: String = "",
    val nombre: String = "",
    val alergias: List<String> = emptyList(),
    val role: String = "Explorador"
)

//la clase de los grupos
data class GroupProfile(
    val id: String = "",
    val nombre: String = "",
    val adminId: String = "",
    val miembros: List<String> = emptyList()
)

//esta clase esta en desuso porque al final no acabe pudiendo integrar los infermes
data class HistoryItem(
    val name: String = "",
    val isApto: Boolean = false,
    val timestamp: com.google.firebase.Timestamp? = null
)