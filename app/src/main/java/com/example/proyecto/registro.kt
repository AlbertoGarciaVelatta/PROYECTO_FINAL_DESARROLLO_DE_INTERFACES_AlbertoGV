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

@Composable
fun RegistroMinimoScreen(
    isLoginMode: Boolean,
    onSwitchMode: () -> Unit,
    onAction: (String, String, List<String>) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val alergiasSeleccionadas = remember { mutableStateListOf<String>() }
    val opcionesAlergias = listOf("GLUTEN", "LACTOSA", "FRUTOS SECOS", "HUEVO", "PESCADO", "SOJA")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundMint)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // BANNER A TODO LO ANCHO
        Image(
            painter = painterResource(id = R.drawable.logoentero),
            contentDescription = "AllergyControl",
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            contentScale = ContentScale.FillWidth // Ocupa todo el ancho sin márgenes
        )

        Spacer(modifier = Modifier.height(40.dp))

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text(if (isLoginMode) "Usuario" else "Nombre Completo") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SafeGreen)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SafeGreen)
            )

            if (!isLoginMode) {
                Spacer(modifier = Modifier.height(30.dp))
                Text("Mis Alergias:", style = MaterialTheme.typography.titleMedium, color = SlateGray )
                Spacer(modifier = Modifier.height(8.dp))

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

            Button(
                onClick = { onAction(nombre, password, alergiasSeleccionadas.toList()) },
                modifier = Modifier.fillMaxWidth().height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SafeGreen)
            ) {
                Text(if (isLoginMode) "INGRESAR" else "REGISTRARSE")
            }

            TextButton(
                onClick = onSwitchMode,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(if (isLoginMode) "¿No tienes cuenta? Crea una" else "¿Ya tienes cuenta? Entra", color = SafeGreen)
            }
        }
    }
}