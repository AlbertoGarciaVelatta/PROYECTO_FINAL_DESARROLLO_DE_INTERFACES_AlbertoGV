package com.example.proyecto

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.os.Bundle
import android.content.Intent
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObjects
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.FieldPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class ProductViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    var products = mutableStateOf<List<Product>>(emptyList())
    var history = mutableStateOf<List<HistoryItem>>(emptyList())
    var userAllergies = mutableStateOf<List<String>>(emptyList())
    var userName = mutableStateOf("Cargando...")
    var isLoading = mutableStateOf(false)

    var estaEscuchando = mutableStateOf(false)
    var scanResultMessage = mutableStateOf<String?>(null)
    var ultimoProductoEscaneado = mutableStateOf<Product?>(null)
    var grupoActivo = mutableStateOf<GroupProfile?>(null)

    init {
        escucharProductos()
    }

    private fun escucharProductos() {
        db.collection("products")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    products.value = snapshot.toObjects<Product>()
                }
            }
    }

    fun cargarPerfilUsuario(uid: String) {
        escucharHistorial(uid)
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val perfil = document.toObject<UserProfile>()
                if (perfil != null) {
                    userAllergies.value = perfil.alergias
                    userName.value = perfil.nombre
                }
            }
    }

    fun escucharHistorial(userUid: String) {
        db.collection("users").document(userUid).collection("history")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("DEBUG", "Error en el listener: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val lista = snapshot.toObjects(HistoryItem::class.java)
                    history.value = lista
                }
            }
    }

    fun escanearProducto(codigo: String) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            buscarYRegistrarProducto(codigo, uid)
        } else {
            scanResultMessage.value = "ERROR: Usuario no identificado"
        }
    }

    private fun buscarYRegistrarProducto(codigo: String, uid: String) {
        isLoading.value = true
        db.collection("products").document(codigo).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val producto = doc.toObject<Product>()
                    if (producto != null) procesarSegunModo(uid, producto)
                    isLoading.value = false
                } else {
                    consultarApiExterna(codigo) { nuevo ->
                        if (nuevo != null) {
                            db.collection("products").document(codigo).set(nuevo)
                            procesarSegunModo(uid, nuevo)
                        } else {
                            isLoading.value = false
                            scanResultMessage.value = "NO ENCONTRADO"
                        }
                    }
                }
            }
    }

    private fun procesarSegunModo(uid: String, producto: Product) {
        val grupo = grupoActivo.value
        if (grupo != null) obtenerMiembrosYProcesar(uid, producto, grupo)
        else procesarResultadoIndividual(uid, producto, userAllergies.value)
    }

    private fun obtenerMiembrosYProcesar(uid: String, producto: Product, grupo: GroupProfile) {
        if (grupo.miembros.isEmpty()) {
            procesarResultadoIndividual(uid, producto, userAllergies.value)
            return
        }

        db.collection("users").whereIn(FieldPath.documentId(), grupo.miembros).get()
            .addOnSuccessListener { snapshot ->
                val miembros = snapshot.toObjects(UserProfile::class.java)
                val alergenosProducto = producto.allergens.map { it.trim().uppercase() }

                val personasEnRiesgo = miembros.filter { persona ->
                    val alergiasPersona = persona.alergias.map { it.trim().uppercase() }
                    alergiasPersona.any { it in alergenosProducto }
                }.map { it.nombre }

                val esApto = personasEnRiesgo.isEmpty()
                ultimoProductoEscaneado.value = producto
                scanResultMessage.value = if (esApto) "APTO PARA GRUPO"
                else "NO APTO: ${personasEnRiesgo.joinToString()}"

                guardarEnHistorial(uid, producto, esApto, grupo.nombre)
            }
    }

    private fun procesarResultadoIndividual(uid: String, producto: Product, alergias: List<String>) {
        val alergiasUsuario = alergias.map { it.trim().uppercase() }
        val alergenosProducto = producto.allergens.map { it.trim().uppercase() }
        val contieneAlergenos = alergenosProducto.any { it in alergiasUsuario }
        val esApto = !contieneAlergenos

        ultimoProductoEscaneado.value = producto
        scanResultMessage.value = if (esApto) "¡APTO!" else "¡PELIGRO: NO APTO!"
        guardarEnHistorial(uid, producto, esApto, "Personal")
    }

    private fun guardarEnHistorial(uid: String, producto: Product, esApto: Boolean, contexto: String) {
        val nuevoItem = HistoryItem(
            name = producto.name,
            isApto = esApto,
            timestamp = com.google.firebase.Timestamp.now(),
            context = contexto
        )

        db.collection("users").document(uid).collection("history").add(nuevoItem)
            .addOnSuccessListener { isLoading.value = false }
            .addOnFailureListener { isLoading.value = false }
    }

    fun mostrarDetalleDesdeHistorial(item: HistoryItem) {
        val productoEncontrado = products.value.find { it.name == item.name }
        ultimoProductoEscaneado.value = productoEncontrado
        scanResultMessage.value = if (item.isApto) {
            if (item.context == "Personal") "¡APTO!" else "APTO PARA GRUPO"
        } else {
            if (item.context == "Personal") "¡PELIGRO: NO APTO!" else "NO APTO PARA EL GRUPO"
        }
    }

    private fun consultarApiExterna(codigo: String, onResult: (Product?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://world.openfoodfacts.org/api/v2/product/$codigo.json")
                val json = JSONObject(url.readText())
                if (json.getInt("status") == 1) {
                    val p = json.getJSONObject("product")
                    val nuevo = Product(
                        id = codigo,
                        name = p.optString("product_name", "Desconocido"),
                        allergens = mapearAlergenosApi(p.optString("allergens_tags", ""), p.optString("product_name", ""), p.optString("ingredients_text", "")),
                        imageUrl = p.optString("image_url", "")
                    )
                    withContext(Dispatchers.Main) { onResult(nuevo) }
                } else withContext(Dispatchers.Main) { onResult(null) }
            } catch (e: Exception) { withContext(Dispatchers.Main) { onResult(null) } }
        }
    }

    private fun mapearAlergenosApi(tags: String, nombre: String, ingredientes: String): List<String> {
        val detectados = mutableListOf<String>()

        // Normalizamos el texto
        val texto = "$nombre $ingredientes".lowercase()
            .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")

        // Detectamos las excepciones primero
        val sinLactosa = texto.contains("sin lactosa") || texto.contains("0% lactosa") ||
                texto.contains("lactofree") || tags.contains("en:lactose-free") ||
                texto.contains("leche sin lactosa")

        val sinGluten = texto.contains("sin gluten") || texto.contains("gluten free") ||
                tags.contains("en:gluten-free")

        //lista de alergenos
        val categorias = mapOf(
            "MOLUSCOS" to listOf("en:molluscs", "mejillon", "almeja", "pulpo", "calamar", "sepia", "caracol"),
            "CRUSTACEOS" to listOf("en:crustaceans", "gamba", "langostino", "cangrejo", "buey de mar", "cigala"),
            "GLUTEN" to listOf("en:gluten", "en:wheat", "en:barley", "en:rye", "en:oats", "trigo", "cebada", "centeno", "avena", "espelta", "kamut"),
            "LACTOSA" to listOf("en:milk", "en:dairy", "leche", "lactosa", "suero", "mantequilla", "queso", "yogur", "caseina", "nata", "semidesnatada", "desnatada"),
            "HUEVO" to listOf("en:eggs", "huevo", "albumina", "yema", "lysozyme", "ovomucina"),
            "FRUTOS SECOS" to listOf("en:nuts", "en:tree-nuts", "nuez", "almendra", "avellana", "anacardo", "pistacho", "pinon", "castana"),
            "CACAHUETES" to listOf("en:peanuts", "cacahuete", "mani", "arachis"),
            "SOJA" to listOf("en:soya", "soja", "lecitina de soja"),
            "PESCADO" to listOf("en:fish", "pescado", "bacalao", "atun", "merluza", "salmon"),
            "MOSTAZA" to listOf("en:mustard", "mostaza"),
            "SESAMO" to listOf("en:sesame", "sesamo", "ajonjoli"),
            "SULFITOS" to listOf("en:sulphites", "sulfitos", "dioxido de azufre")
        )
                    categorias.forEach { (cat, claves) ->
                val contieneClave = claves.any { texto.contains(it) || tags.contains(it) }

                if (contieneClave) {
                    if (cat == "LACTOSA" && sinLactosa) {
                        Log.d("ALERGENOS", "Detectada leche pero es SIN LACTOSA. Ignorando...")
                    } else if (cat == "GLUTEN" && sinGluten) {
                        Log.d("ALERGENOS", "Detectado gluten pero es SIN GLUTEN. Ignorando...")
                    } else {
                        detectados.add(cat)
                    }
                }
            }

                    Log.d("ALERGENOS", "Resultado final para $nombre: $detectados")
        return detectados.distinct()
    }
    fun obtenerEstadisticas(): Map<String, Int> {
        val h = history.value
        val total = h.size
        val aptos = h.count { it.isApto }
        return mapOf("Total" to total, "Aptos" to aptos, "No Aptos" to (total - aptos))
    }

    //aqui la opcion de escucha
    fun iniciarEscuchaContinua(context: android.content.Context, onCommand: () -> Unit) {
        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val textoRaw = matches?.get(0)?.lowercase() ?: ""
                val texto = textoRaw.replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")

                //las palabras que inician el scaner
                if (texto.contains("escanear") || texto.contains("analisis") ||
                    texto.contains("escaner") || texto.contains("revisar") || texto.contains("foto")) {
                    onCommand()
                }
                speechRecognizer.startListening(intent)
            }

            override fun onError(error: Int) {
                speechRecognizer.cancel()
                speechRecognizer.startListening(intent)
            }

            override fun onReadyForSpeech(params: Bundle?) { estaEscuchando.value = true }
            override fun onEndOfSpeech() { estaEscuchando.value = false }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer.startListening(intent)
    }
}