package com.example.proyecto

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(historyItems: List<HistoryItem>) {
    var filterBy by remember { mutableStateOf("TODOS") }

    val filteredList = when (filterBy) {
        "APTOS" -> historyItems.filter { it.isApto }
        "NO APTOS" -> historyItems.filter { !it.isApto }
        else -> historyItems
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Mi Historial",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // FILTROS MÁS MODERNOS (Chips)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filterBy == "TODOS",
                onClick = { filterBy = "TODOS" },
                label = { Text("Todos") }
            )
            FilterChip(
                selected = filterBy == "APTOS",
                onClick = { filterBy = "APTOS" },
                label = { Text("Aptos") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFE8F5E9))
            )
            FilterChip(
                selected = filterBy == "NO APTOS",
                onClick = { filterBy = "NO APTOS" },
                label = { Text("No Aptos") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFFEBEE))
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No hay registros en esta categoría", color = Color.Gray)
            }
        } else {
            LazyColumn {
                items(filteredList) { item ->
                    ListItem(
                        headlineContent = { Text(item.name, style = MaterialTheme.typography.bodyLarge) },
                        supportingContent = {
                            Text(
                                text = if (item.isApto) "Seguro para tu consumo" else "Contiene alérgenos no aptos",
                                color = if (item.isApto) Color(0xFF388E3C) else Color.Red
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = if (item.isApto) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (item.isApto) Color(0xFF4CAF50) else Color.Red,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                }
            }
        }
    }
}
