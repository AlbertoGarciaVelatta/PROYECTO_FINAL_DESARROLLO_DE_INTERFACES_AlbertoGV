package com.example.proyecto

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AuthManager {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()


    private fun generarFakeEmail(nombre: String): String {
        return "${nombre.lowercase().replace(" ", "")}@allergycontrol.com"
    }

    fun registrarUsuario(nombre: String, pass: String, alergias: List<String>, onResult: (Boolean, String?) -> Unit) {
        val email = generarFakeEmail(nombre)
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

    fun loguearUsuario(nombre: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        val email = generarFakeEmail(nombre)
        val safePass = if (pass.length < 6) pass + "123456" else pass
        auth.signInWithEmailAndPassword(email, safePass)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { onResult(false, "Nombre o contraseña incorrectos") }
    }
}

class GroupManager {
    private val db = FirebaseFirestore.getInstance()

    fun crearGrupo(nombreGrupo: String, adminId: String, onResult: (Boolean) -> Unit) {
        val groupId = db.collection("groups").document().id
        val nuevoGrupo = GroupProfile(id = groupId, nombre = nombreGrupo, adminId = adminId, miembros = listOf(adminId))
        db.collection("groups").document(groupId).set(nuevoGrupo).addOnSuccessListener { onResult(true) }.addOnFailureListener { onResult(false) }
    }

    fun eliminarGrupo(groupId: String, onResult: (Boolean) -> Unit) {
        db.collection("groups").document(groupId).delete().addOnSuccessListener { onResult(true) }.addOnFailureListener { onResult(false) }
    }

    fun añadirMiembroPorId(groupId: String, nuevoMiembroId: String, onResult: (Boolean) -> Unit) {
        db.collection("groups").document(groupId)
            .update("miembros", FieldValue.arrayUnion(nuevoMiembroId))
            .addOnSuccessListener { onResult(true) }.addOnFailureListener { onResult(false) }
    }

    fun escucharMisGrupos(userId: String, onUpdate: (List<GroupProfile>) -> Unit) {
        db.collection("groups").whereArrayContains("miembros", userId)
            .addSnapshotListener { snapshot, _ ->
                onUpdate(snapshot?.toObjects(GroupProfile::class.java) ?: emptyList())
            }
    }
}