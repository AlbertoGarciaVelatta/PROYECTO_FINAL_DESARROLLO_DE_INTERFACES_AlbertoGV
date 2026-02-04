package com.example.proyecto

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ProductCard(product: Product, userAllergies: List<String>) {
    val esPeligroso = product.allergens.any { allergen ->
        userAllergies.any { it.trim().equals(allergen.trim(), ignoreCase = true) }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (esPeligroso) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = product.name, style = MaterialTheme.typography.titleMedium)
            Text(text = product.store, style = MaterialTheme.typography.bodySmall)
            Text(
                text = if (esPeligroso) "NO APTO" else "APTO",
                color = if (esPeligroso) Color.Red else Color(0xFF4CAF50),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}