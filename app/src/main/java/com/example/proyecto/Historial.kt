package com.example.proyecto

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.proyecto.ui.theme.*

@Composable
fun InformeSaludScreen(viewModel: ProductViewModel) {
    var filtroSoloPeligrosos by remember { mutableStateOf(false) }

    val historialCompleto = viewModel.history.value
    val stats = viewModel.obtenerEstadisticas()
    val total = stats["Total"] ?: 0

    val datosAMostrar = if (filtroSoloPeligrosos) {
        historialCompleto.filter { !it.isApto }
    } else {
        historialCompleto
    }

    // Usamos un Box para que el mensaje gigante pueda flotar encima de la lista
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Dashboard de Seguridad", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Análisis automático de tus escaneos", color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("TOTAL", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("$total", fontSize = 24.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = WarningRed.copy(0.1f))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("ALERTAS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = WarningRed)
                            Text("${stats["No Aptos"]}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = WarningRed)
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Índice de Seguridad", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        val progreso = if (total > 0) (stats["Aptos"]!!.toFloat() / total) else 0f
                        LinearProgressIndicator(
                            progress = progreso,
                            modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                            color = SafeGreen,
                            trackColor = WarningRed.copy(0.2f)
                        )
                        Text("${(progreso * 100).toInt()}% de productos aptos", fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                    Text("Filtrar solo peligrosos", modifier = Modifier.weight(1f), fontSize = 14.sp)
                    Switch(
                        checked = filtroSoloPeligrosos,
                        onCheckedChange = { filtroSoloPeligrosos = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = WarningRed)
                    )
                }
            }

            // --- LISTA DE HISTORIAL ---
            items(datosAMostrar) { item ->
                ListItem(
                    // NUEVO: Añadimos el clickable para que reaccione al toque
                    modifier = Modifier.clickable {
                        viewModel.mostrarDetalleDesdeHistorial(item)
                    },
                    headlineContent = { Text(item.name, fontWeight = FontWeight.Medium) },
                    supportingContent = {
                        Column {
                            Text(item.timestamp?.toDate()?.toLocaleString() ?: "Fecha desconocida")
                            Text(
                                text = if (item.context == "Personal") "👤 Personal" else "👥 Grupo: ${item.context}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (item.context == "Personal") Color.Gray else Color(0xFF6200EE),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    },
                    trailingContent = {
                        Icon(
                            imageVector = if (item.isApto) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (item.isApto) SafeGreen else WarningRed
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                HorizontalDivider(color = Color.LightGray.copy(0.5f))
            }
        }

        // --- NUEVO: LÓGICA PARA MOSTRAR EL RESULTADO VISUAL GIGANTE ---
        val mensaje = viewModel.scanResultMessage.value
        val producto = viewModel.ultimoProductoEscaneado.value

        if (mensaje != null) {
            // Este Box ocupa toda la pantalla y pone el mensaje en el centro
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Llamamos a tu función que ya tienes creada
                ResultadoVisualGigante(
                    mensaje = mensaje,
                    producto = producto,
                    onDismiss = { viewModel.scanResultMessage.value = null }
                )
            }
        }
    }
}