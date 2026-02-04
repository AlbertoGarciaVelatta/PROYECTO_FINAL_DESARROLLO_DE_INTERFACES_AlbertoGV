package com.example.proyecto

import com.google.firebase.firestore.IgnoreExtraProperties

// 1. Mantenemos el Enum por si quieres usar selectores bonitos en la UI
enum class Allergen { GLUTEN, LACTOSE, NUTS, SHELLFISH, EGGS, FISH }

@IgnoreExtraProperties
data class Product(
    val id: String = "",
    val name: String = "",          // Usamos 'name' para que coincida con tu HistoryItem
    val store: String = "Tienda Externa",
    val allergens: List<String> = emptyList(), // Lista de strings (ej: "GLUTEN", "MILK")
    val isVerified: Boolean = false,
    val imageUrl: String = ""       // Añadimos esto porque la API da fotos y queda muy bien
)

data class UserProfile(
    val uid: String = "", // Nuevo campo para el ID único
    val nombre: String = "",
    val alergias: List<String> = emptyList(),
    val role: String = "Explorador"
)

data class GroupProfile(
    val id: String = "",           // ID único del grupo
    val nombre: String = "",       // Nombre del grupo (ej: "Familia")
    val adminId: String = "",      // UID del creador
    val miembros: List<String> = emptyList() // Lista de UIDs de los integrantes
)

// Clase para el historial (lo que se guarda cuando escaneas)
data class HistoryItem(
    val name: String = "",
    val isApto: Boolean = false,
    val timestamp: com.google.firebase.Timestamp? = null
)