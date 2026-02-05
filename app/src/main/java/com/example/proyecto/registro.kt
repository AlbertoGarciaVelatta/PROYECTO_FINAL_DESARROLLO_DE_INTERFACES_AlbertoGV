package com.example.proyecto

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.proyecto.ui.theme.BackgroundMint
import com.example.proyecto.ui.theme.SafeGreen
import com.example.proyecto.ui.theme.SlateGray

/**
 * PANTALLA: RegistroMinimoScreen
 * Gestiona tanto el acceso de usuarios existentes como la creación de nuevos perfiles.
 * RA7.b (Personalización): Utiliza una estética limpia basada en el color 'SafeGreen'.
 */
@Composable
fun RegistroMinimoScreen(
    isLoginMode: Boolean, // Alterna entre modo "Entrar" o "Registrarse"
    onSwitchMode: () -> Unit,
    onAction: (String, String, List<String>) -> Unit // Callback que envía los datos al AuthManager
) {
    // ESTADOS LOCALES: Controlan lo que el usuario escribe en tiempo real
    var nombre by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Lista reactiva para las alergias
    val alergiasSeleccionadas = remember { mutableStateListOf<String>() }

    // Listado predefinido de alérgenos comunes (Estandarización de datos)
    val opcionesAlergias = listOf("GLUTEN", "LACTOSA", "FRUTOS SECOS", "HUEVO", "PESCADO", "SOJA")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundMint)
            .verticalScroll(rememberScrollState()), // Permite scroll en móviles pequeños al abrir el teclado
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Logo en la parte superior
        Image(
            painter = painterResource(id = R.drawable.logoentero),
            contentDescription = "AllergyControl",
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            contentScale = ContentScale.FillWidth
        )

        Spacer(modifier = Modifier.height(40.dp))

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            // Identificador de usuario
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text(if (isLoginMode) "Usuario" else "Nombre Completo") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SafeGreen)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Contraseña
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SafeGreen)
            )

            // Solo mostramos el selector de alergias si el usuario se está registrando
            if (!isLoginMode) {
                Spacer(modifier = Modifier.height(30.dp))
                Text("Mis Alergias:", style = MaterialTheme.typography.titleMedium, color = SlateGray )
                Spacer(modifier = Modifier.height(8.dp))

                // Generación dinámica de Checkboxes para cada alérgeno
                opcionesAlergias.forEach { alergia ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = alergiasSeleccionadas.contains(alergia),
                            onCheckedChange = {
                                if (it) alergiasSeleccionadas.add(alergia)
                                else alergiasSeleccionadas.remove(alergia)
                            },
                            colors = CheckboxDefaults.colors(checkedColor = SafeGreen)
                        )
                        Text(text = alergia, color = SlateGray )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // El boton Ejecuta la acción (Login o Registro)
            Button(
                onClick = { onAction(nombre, password, alergiasSeleccionadas.toList()) },
                modifier = Modifier.fillMaxWidth().height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SafeGreen)
            ) {
                Text(if (isLoginMode) "INGRESAR" else "REGISTRARSE")
            }

            // boton de texto que permite al usuario alternar entre Login y Registro sin cambiar de pantalla
            TextButton(
                onClick = onSwitchMode,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(if (isLoginMode) "¿No tienes cuenta? Crea una" else "¿Ya tienes cuenta? Entra", color = SafeGreen)
            }
        }
    }
}