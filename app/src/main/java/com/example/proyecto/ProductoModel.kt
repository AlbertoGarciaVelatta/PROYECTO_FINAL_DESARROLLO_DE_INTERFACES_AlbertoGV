package com.example.proyecto

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObjects
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

/**
 * VIEWMODEL: ProductViewModel
 * Es el motor de la aplicación. Gestiona el estado del escáner, los perfiles y la lógica de alérgenos.
 */
class ProductViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    // Estados reactivos: La UI se "suscribe" a estos cambios
    var products = mutableStateOf<List<Product>>(emptyList())
    var history = mutableStateOf<List<HistoryItem>>(emptyList())
    var userAllergies = mutableStateOf<List<String>>(emptyList())
    var userName = mutableStateOf("Cargando...")
    var isLoading = mutableStateOf(false)

    var scanResultMessage = mutableStateOf<String?>(null)
    var ultimoProductoEscaneado = mutableStateOf<Product?>(null)
    var grupoActivo = mutableStateOf<GroupProfile?>(null)

    init {
        escucharProductos() //Al iniciar, se conecta a Firestore
    }

    /**
     * Sincronización en tiempo real con la base de datos de productos.
     */
    private fun escucharProductos() {
        db.collection("products")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    products.value = snapshot.toObjects<Product>()
                }
            }
    }

    /**
     * RA8.e (Seguridad): Carga solo los datos del perfil del usuario autenticado.
     */
    fun cargarPerfilUsuario(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val perfil = document.toObject<UserProfile>()
                if (perfil != null) {
                    userAllergies.value = perfil.alergias
                    userName.value = perfil.nombre
                }
            }
    }

    /**
     * RA5 (Informes): Escucha la subcolección 'history' para mostrar qué ha escaneado el usuario.
     */
    fun escucharHistorial(userUid: String) {
        db.collection("users").document(userUid).collection("history")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    history.value = snapshot.documents.map { doc ->
                        HistoryItem(
                            name = doc.getString("name") ?: "Desconocido",
                            isApto = doc.getBoolean("isApto") ?: false,
                            timestamp = doc.getTimestamp("timestamp")
                        )
                    }
                }
            }
    }

    /**
     * LÓGICA PRINCIPAL: Orquesta la búsqueda del producto.
     * 1. Mira en la BD propia de Firestore.
     * 2. Si no está, consulta una API externa (OpenFoodFacts).
     */
    fun buscarYRegistrarProducto(codigo: String, uid: String) {
        isLoading.value = true
        val grupo = grupoActivo.value

        db.collection("products").document(codigo).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val producto = doc.toObject<Product>()
                    // Si hay un grupo activo, la lógica cambia a modo "colaborativo"
                    if (grupo != null) obtenerMiembrosYProcesar(uid, producto!!, grupo)
                    else procesarResultadoIndividual(uid, producto!!, userAllergies.value)
                } else {
                    // RA7.d (Herramientas Externas): Uso de API externa si el producto es nuevo
                    consultarApiExterna(codigo) { nuevoProducto ->
                        if (nuevoProducto != null) {
                            db.collection("products").document(codigo).set(nuevoProducto)
                            if (grupo != null) obtenerMiembrosYProcesar(uid, nuevoProducto, grupo)
                            else procesarResultadoIndividual(uid, nuevoProducto, userAllergies.value)
                        } else {
                            isLoading.value = false
                            scanResultMessage.value = "PRODUCTO NO ENCONTRADO"
                        }
                    }
                }
            }
            .addOnFailureListener {
                isLoading.value = false
                scanResultMessage.value = "ERROR DE CONEXIÓN"
            }
    }

    /**
     * P Cruza los ingredientes del producto con las alergias
     * de todos los miembros del grupo seleccionado.
     */
    private fun obtenerMiembrosYProcesar(uid: String, producto: Product, grupo: GroupProfile) {
        db.collection("users")
            .whereIn("uid", grupo.miembros)
            .get()
            .addOnSuccessListener { snapshot ->
                val miembros = snapshot.toObjects(UserProfile::class.java)
                val personasEnRiesgo = mutableListOf<String>()

                miembros.forEach { persona ->
                    val esAlergico = producto.allergens.any { pA ->
                        persona.alergias.any { it.trim().equals(pA.trim(), ignoreCase = true) }
                    }
                    if (esAlergico) personasEnRiesgo.add(persona.nombre)
                }

                val esApto = personasEnRiesgo.isEmpty()
                ultimoProductoEscaneado.value = producto
                scanResultMessage.value = if (esApto) "APTO PARA TODO EL GRUPO"
                else "NO APTO para: ${personasEnRiesgo.joinToString(", ")}"

                guardarEnHistorial(uid, producto, esApto)
            }
    }

    /**
     * PROCESAMIENTO INDIVIDUAL: Compara ingredientes solo con el usuario actual.
     */
    private fun procesarResultadoIndividual(uid: String, producto: Product, misAlergias: List<String>) {
        val conflictivos = producto.allergens.filter { pA ->
            misAlergias.any { it.trim().equals(pA.trim(), ignoreCase = true) }
        }
        val esApto = conflictivos.isEmpty()
        ultimoProductoEscaneado.value = producto
        scanResultMessage.value = if (esApto) "¡ESTE PRODUCTO ES APTO!" else "¡ATENCIÓN: NO APTO!"

        guardarEnHistorial(uid, producto, esApto)
    }

    /**
     * Registro en la subcolección de historial para la trazabilidad del usuario.
     */
    private fun guardarEnHistorial(uid: String, producto: Product, esApto: Boolean) {
        val registro = hashMapOf(
            "name" to producto.name,
            "isApto" to esApto,
            "timestamp" to com.google.firebase.Timestamp.now()
        )
        db.collection("users").document(uid).collection("history")
            .document(producto.id).set(registro)
            .addOnCompleteListener { isLoading.value = false }
    }

    /**
     * CONSULTA API EXTERNA (OpenFoodFacts)
     * RA8.f (Uso de recursos): Ejecuta la petición en un hilo secundario (Dispatchers.IO)
     * para no bloquear la interfaz de usuario.
     */
    private fun consultarApiExterna(codigo: String, onResult: (Product?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://world.openfoodfacts.org/api/v2/product/$codigo.json")
                val text = url.readText()
                val json = JSONObject(text)
                if (json.getInt("status") == 1) {
                    val p = json.getJSONObject("product")
                    val nuevo = Product(
                        id = codigo,
                        name = p.optString("product_name", "Desconocido"),
                        store = p.optString("brands", "Marca desconocida"),
                        allergens = mapearAlergenosApi(
                            p.optString("allergens_tags", ""),
                            p.optString("product_name", ""),
                            p.optString("ingredients_text", "")
                        ),
                        imageUrl = p.optString("image_url", ""),
                        isVerified = false
                    )
                    withContext(Dispatchers.Main) { onResult(nuevo) }
                } else withContext(Dispatchers.Main) { onResult(null) }
            } catch (e: Exception) { withContext(Dispatchers.Main) { onResult(null) } }
        }
    }

    /**
     * ALGORITMO DE FILTRADO (El "Cerebro" de la app)
     * Cruza etiquetas oficiales de la API con búsqueda de palabras clave en el texto.
     */
    private fun mapearAlergenosApi(tags: String, nombre: String, ingredientes: String): List<String> {
        val detectados = mutableListOf<String>()
        val textoCompleto = "$nombre $ingredientes".lowercase()
        val tagsList = tags.lowercase()

        // Diccionario de categorías y palabras clave para la detección
        val categorias = mapOf(
            "MOLUSCOS" to listOf("en:molluscs", "mejillón", "almeja", "pulpo", "calamar", "sepia", "caracol"),
            "CRUSTÁCEOS" to listOf("en:crustaceans", "gamba", "langostino", "cangrejo", "buey de mar", "cigala"),
            "GLUTEN" to listOf("en:gluten", "en:wheat", "en:barley", "en:rye", "en:oats", "trigo", "cebada", "centeno", "avena", "espelta", "kamut"),
            "LACTOSA" to listOf("en:milk", "en:dairy", "leche", "lactosa", "suero", "mantequilla", "queso", "yogur", "caseína", "nata"),
            "HUEVO" to listOf("en:eggs", "huevo", "albúmina", "yema", "lysozyme", "ovomucina"),
            "FRUTOS SECOS" to listOf("en:nuts", "en:tree-nuts", "nuez", "almendra", "avellana", "anacardo", "pistacho", "piñón", "castaña", "nuez de brasil", "macadamia"),
            "CACAHUETES" to listOf("en:peanuts", "cacahuete", "maní", "arachis"),
            "SOJA" to listOf("en:soya", "en:soybeans", "soja", "lecitina de soja", "glycine max"),
            "PESCADO" to listOf("en:fish", "pescado", "bacalao", "atún", "merluza", "salmón"),
            "MARISCO" to listOf("en:crustaceans", "en:molluscs", "marisco", "gamba", "langostino", "mejillón", "almeja", "calamar", "cangrejo"),
            "MOSTAZA" to listOf("en:mustard", "mostaza", "sinapis"),
            "SÉSAMO" to listOf("en:sesame-seeds", "sésamo", "ajonjolí"),
            "SULFITOS" to listOf("en:sulphites", "sulfitos", "e220", "dióxido de azufre")
        )

        //Detección por etiquetas oficiales (Tags)
        categorias.forEach { (categoria, palabrasClave) ->
            if (palabrasClave.any { it.startsWith("en:") && tagsList.contains(it) }) {
                detectados.add(categoria)
            }
        }

        //Detección por texto (Búsqueda inteligente para evitar falsos positivos como "Sin Lactosa")
        categorias.forEach { (categoria, palabrasClave) ->
            val palabrasSoloTexto = palabrasClave.filter { !it.startsWith("en:") }
            if (palabrasSoloTexto.any { textoCompleto.contains(it) }) {
                // RA8.c (Pruebas de regresión): Evita marcar como peligroso si el texto dice "SIN [alérgeno]"
                if (!textoCompleto.contains("sin ${categoria.lowercase()}")) {
                    detectados.add(categoria)
                }
            }
        }
        //Detección de trazas o contaminación cruzada
        if (textoCompleto.contains("puede contener") || textoCompleto.contains("trazas de")) {
            categorias.keys.forEach { cat ->
                if (textoCompleto.contains(cat.lowercase())) detectados.add(cat)
            }
        }

        return detectados.distinct()
    }
}