package com.example.proyecto

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

class GroupViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance() // Instancia de la BD
    private val groupManager = GroupManager() // Clase auxiliar para operaciones de grupos

    // Estado reactivo: cuando cambia esta lista, la UI se actualiza automáticamente
    var misGrupos = mutableStateOf<List<GroupProfile>>(emptyList())


    fun cargarGrupos(userId: String) {
        //se encarga de actualizar en tiempo real el estado de los grupos si alguien te añade a un grupo, aparecerá sin reiniciar la app
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

    //esta funcion se encarga de obtener los nombres de los usuarios para los grupos con los ID
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

    //esta funcion es para invitar a los usuarios al grupo
    fun invitarUsuario(context: android.content.Context, groupId: String, nuevoMiembroId: String) {
// Primero verifica que el usuario existe antes de intentar añadirlo
        db.collection("users").document(nuevoMiembroId).get()
            .addOnSuccessListener { document ->
                val nombreInvitado = document.getString("nombre") ?: "El usuario"

                groupManager.añadirMiembroPorId(groupId, nuevoMiembroId) { exito ->
                    if (exito) {
                        //un mensaje emergente para informar al usuario que se ha invitado correctamente
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