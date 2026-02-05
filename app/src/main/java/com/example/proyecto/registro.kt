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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val opcionesAlergias = listOf(
        "GLUTEN", "LACTOSA", "HUEVO", "FRUTOS SECOS",
        "CACAHUETES", "SOJA", "PESCADO", "CRUSTÁCEOS",
        "MOLUSCOS", "MOSTAZA", "SÉSAMO", "SULFITOS", "ALTRAMUZ", "APIO"
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundMint)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.logoentero),
            contentDescription = "AllergyControl",
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            contentScale = ContentScale.FillWidth
        )

        Spacer(modifier = Modifier.height(20.dp))

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text(if (isLoginMode) "Usuario" else "Nombre Completo") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SafeGreen)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SafeGreen)
            )

            if (!isLoginMode) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Mis Alergias:", style = MaterialTheme.typography.titleMedium, color = SlateGray)
                Spacer(modifier = Modifier.height(8.dp))

                opcionesAlergias.chunked(2).forEach { fila ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        fila.forEach { alergia ->
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = alergiasSeleccionadas.contains(alergia),
                                    onCheckedChange = {
                                        if (it) alergiasSeleccionadas.add(alergia)
                                        else alergiasSeleccionadas.remove(alergia)
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = SafeGreen)
                                )
                                Text(
                                    text = alergia,
                                    color = SlateGray,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = { onAction(nombre, password, alergiasSeleccionadas.toList()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SafeGreen)
            ) {
                Text(
                    text = if (isLoginMode) "INGRESAR" else "REGISTRARSE",
                    fontWeight = FontWeight.Bold
                )
            }

            TextButton(
                onClick = onSwitchMode,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = if (isLoginMode) "¿No tienes cuenta? Crea una" else "¿Ya tienes cuenta? Entra",
                    color = SafeGreen
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}