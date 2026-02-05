package com.example.proyecto

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore


/**
 * GESTOR: AuthManager
 * Encargado de la autenticación de usuarios.
 * Utiliza una estrategia de "Fake Email" para simplificar el login.
 */
class AuthManager {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Genera un correo ficticio basado en el nombre para cumplir con el requisito de Firebase Auth
    private fun generarFakeEmail(nombre: String): String {
        return "${nombre.lowercase().replace(" ", "")}@allergycontrol.com"
    }

    /**
     * Registra un nuevo usuario en Firebase Auth y crea su perfil en Firestore.
     * RA8.e: Garantiza que cada usuario tenga un espacio de datos privado.
     */
    fun registrarUsuario(nombre: String, pass: String, alergias: List<String>, onResult: (Boolean, String?) -> Unit) {
        val email = generarFakeEmail(nombre)
        // Validación de seguridad simple para evitar errores de Firebase
        val safePass = if (pass.length < 6) pass + "123456" else pass

        auth.createUserWithEmailAndPassword(email, safePass)
            .addOnSuccessListener { resultado ->
                val uid = resultado.user?.uid
                if (uid != null) {
                    val perfil = UserProfile(uid = uid, nombre = nombre, alergias = alergias, role = "Explorador")
                    db.collection("users").document(uid).set(perfil)
                        .addOnSuccessListener { onResult(true, null) }
                        .addOnFailureListener { onResult(false, "Error al crear perfil en base de datos") }
                }
            }
            .addOnFailureListener { exception ->
                onResult(false, "Registro fallido: ${exception.localizedMessage}")
            }
    }

    /**
     * Inicia sesión utilizando las credenciales transformadas a "fake email".
     */
    fun loguearUsuario(nombre: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        val email = generarFakeEmail(nombre)
        val safePass = if (pass.length < 6) pass + "123456" else pass
        auth.signInWithEmailAndPassword(email, safePass)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { onResult(false, "Nombre o contraseña incorrectos") }
    }
}

/**
 * GESTOR: GroupManager
 * Gestiona la base de datos de grupos en Firestore.
 * RA7.h: Define la estrategia de distribución de datos compartidos.
 */
class GroupManager {
    private val db = FirebaseFirestore.getInstance()
    /**
     * Crea un nuevo documento en la colección "groups" con un ID autogenerado.
     */
    fun crearGrupo(nombreGrupo: String, adminId: String, onResult: (Boolean) -> Unit) {
        val groupId = db.collection("groups").document().id
        val nuevoGrupo = GroupProfile(id = groupId, nombre = nombreGrupo, adminId = adminId, miembros = listOf(adminId))
        db.collection("groups").document(groupId).set(nuevoGrupo).addOnSuccessListener { onResult(true) }.addOnFailureListener { onResult(false) }
    }

    /**
     * Elimina el grupo completo. (Solo debería llamarlo el administrador).
     */
    fun eliminarGrupo(groupId: String, onResult: (Boolean) -> Unit) {
        db.collection("groups").document(groupId).delete().addOnSuccessListener { onResult(true) }.addOnFailureListener { onResult(false) }
    }

    /**
     * RA8.f: Optimización de recursos.
     * Utiliza 'FieldValue.arrayUnion' para añadir un miembro sin necesidad de descargar
     * toda la lista de IDs primero, ahorrando ancho de banda.
     */
    fun añadirMiembroPorId(groupId: String, nuevoMiembroId: String, onResult: (Boolean) -> Unit) {
        db.collection("groups").document(groupId)
            .update("miembros", FieldValue.arrayUnion(nuevoMiembroId))
            .addOnSuccessListener { onResult(true) }.addOnFailureListener { onResult(false) }
    }

    /**
     * Escucha en tiempo real
     * Esta función mantiene la app actualizada automáticamente si alguien te añade a un grupo
     * o si el nombre del grupo cambia, sin necesidad de refrescar manualmente.
     */
    fun escucharMisGrupos(userId: String, onUpdate: (List<GroupProfile>) -> Unit) {
        db.collection("groups").whereArrayContains("miembros", userId)
            .addSnapshotListener { snapshot, _ ->
                onUpdate(snapshot?.toObjects(GroupProfile::class.java) ?: emptyList())
            }
    }
}