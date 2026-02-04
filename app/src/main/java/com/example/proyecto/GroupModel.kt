package com.example.proyecto

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

class GroupViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val groupManager = GroupManager()

    // Lista de grupos observable por la UI
    var misGrupos = mutableStateOf<List<GroupProfile>>(emptyList())

    // Carga los grupos en tiempo real
    fun cargarGrupos(userId: String) {
        groupManager.escucharMisGrupos(userId) { lista ->
            misGrupos.value = lista
        }
    }

    // Crea el grupo en Firestore
    fun crearGrupo(nombre: String, adminId: String) {
        groupManager.crearGrupo(nombre, adminId) { exito ->
            // Aquí podrías manejar un estado de error si quisieras
        }
    }

    // En GroupModel.kt (o GroupViewModel.kt)

    fun eliminarGrupo(groupId: String) {
        groupManager.eliminarGrupo(groupId) { /* Opcional: manejar mensaje de éxito */ }
    }

    fun invitarUsuario(groupId: String, nuevoMiembroId: String) {
        groupManager.añadirMiembroPorId(groupId, nuevoMiembroId) { /* Opcional: manejar mensaje */ }
    }
}