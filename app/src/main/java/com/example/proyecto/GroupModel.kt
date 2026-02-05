package com.example.proyecto

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

class GroupViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val groupManager = GroupManager()

    var misGrupos = mutableStateOf<List<GroupProfile>>(emptyList())


    fun cargarGrupos(userId: String) {
        groupManager.escucharMisGrupos(userId) { lista ->
            misGrupos.value = lista
        }
    }


    fun crearGrupo(nombre: String, adminId: String) {
        groupManager.crearGrupo(nombre, adminId) { exito ->
        }
    }


    fun eliminarGrupo(context: android.content.Context, groupId: String) {
        groupManager.eliminarGrupo(groupId) { exito ->
            if (exito) {
                android.widget.Toast.makeText(context, "Grupo eliminado con éxito", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(context, "Error al eliminar el grupo", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun obtenerNombresMiembros(miembrosIds: List<String>, onResult: (List<String>) -> Unit) {

        if (miembrosIds.isEmpty()) {
            onResult(emptyList())
            return
        }

        db.collection("users")
            .whereIn("uid", miembrosIds)
            .get()
            .addOnSuccessListener { snapshot ->
                val nombres = snapshot.documents.map { it.getString("nombre") ?: "Usuario desconocido" }
                onResult(nombres)
            }
            .addOnFailureListener {
                onResult(listOf("Error al cargar nombres"))
            }
    }

    fun invitarUsuario(context: android.content.Context, groupId: String, nuevoMiembroId: String) {

        db.collection("users").document(nuevoMiembroId).get()
            .addOnSuccessListener { document ->
                val nombreInvitado = document.getString("nombre") ?: "El usuario"

                groupManager.añadirMiembroPorId(groupId, nuevoMiembroId) { exito ->
                    if (exito) {
                        android.widget.Toast.makeText(
                            context,
                            "$nombreInvitado ha sido agregado correctamente",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        android.widget.Toast.makeText(
                            context,
                            "Error al agregar a $nombreInvitado",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .addOnFailureListener {
                android.widget.Toast.makeText(context, "ID de usuario no encontrado", android.widget.Toast.LENGTH_SHORT).show()
            }
    }
}