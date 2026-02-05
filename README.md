# Allergy Control

Allergy Control es una aplicación móvil desarrollada en **Android Studio** con **Jetpack Compose** diseñada para ayudar a personas con alergias alimentarias a identificar productos aptos de forma rápida y segura.

---

##  Características
* **Escaneo Inteligente:** Identifica productos y verifica sus ingredientes.

usando la api de OpenFoodFacts, si no tenemos el producto en la base de datos lo buscamos alli y se importa en la base de datos

  
     CONSULTA API EXTERNA (OpenFoodFacts)
      RA8.f (Uso de recursos): Ejecuta la petición en un hilo secundario
     para no bloquear la interfaz de usuario.
     
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

    y en este apartado de codigo se analiza si contiene algun componente alergeno

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

* **Perfiles de Alergia:** Configura tus alérgenos personales (gluten, lactosa, frutos secos, etc.).

en la creacion de usuario 

* **Indicadores Visuales:** Interfaz clara con tarjetas de colores:
    *  **Verde (APTO):** El producto es seguro para ti.
    *  **Rojo (NO APTO):** Se han detectado alérgenos que coinciden con tu perfil.
 
    *  aqui esta el trozo de codigo del main que se encarga de mostrar el resultado

 @Composable
fun ResultadoVisualGigante(mensaje: String, producto: Product?, onDismiss: () -> Unit) {
    // Lógica binaria para determinar el color de la alerta
    val esApto = mensaje.contains("APTO") && !mensaje.contains("NO APTO")
    val colorP = if (esApto) SafeGreen else AlertRed

    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(28.dp),
        color = if (esApto) Color(0xFFE8F5E9) else Color(0xFFFFEBEE), //los colores dependiendo si es apto o no
        border = BorderStroke(4.dp, colorP),
        shadowElevation = 20.dp
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // Imagen del producto
            if (!producto?.imageUrl.isNullOrEmpty()) {
                Surface(modifier = Modifier.size(120.dp), shape = RoundedCornerShape(12.dp), color = Color.White) {
                    AsyncImage(model = producto?.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize().padding(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(producto?.name?.uppercase() ?: "PRODUCTO", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(mensaje, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, color = if (esApto) Color(0xFF1B5E20) else Color(0xFFB71C1C))

            // Información detallada en caso de peligro
            if (!esApto && !producto?.allergens.isNullOrEmpty()) {
                Text("Alérgenos detectados: ${producto?.allergens?.joinToString(", ")}", color = AlertRed, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp), textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = colorP), modifier = Modifier.fillMaxWidth()) {
                Text("CERRAR", fontWeight = FontWeight.Bold)
            }
        }
    }
}

   
* **Diseño Moderno:** Interfaz basada en Material 3 con una paleta de colores profesional (Verde ment y Gris Ceniza).

## Tecnologías Utilizadas
* **Lenguaje:** [Kotlin](https://kotlinlang.org/)
* **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
* **Arquitectura:** MVVM (Model-View-ViewModel)
* **Componentes de UI:** Cards personalizadas, LazyColumns y estados reactivos.

## Guia de uso
para empezar, hace falta iniciar o abrir nuestra app

<img width="150" height="210" alt="image" src="https://github.com/user-attachments/assets/b789a8e7-5a87-49fb-9aa8-4799f2196edb" />

Y ya dentro para poder utilizar sus funciones crearemos una cuenta

<img width="150" height="210" alt="image" src="https://github.com/user-attachments/assets/d1aef304-0234-483a-bd0d-1022b089c47e" />

Y como nuestra app consiste en saber si somos alergicos a determinados productos, al momento de crear nuestro susario tendremos que seleccionar las alergias

<img width="150" height="270" alt="image" src="https://github.com/user-attachments/assets/c34cc898-00c1-45dd-84c4-7c7adcfa8e73" />

Una vez creado el usuario ya podremos usar la app

<img width="150" height="210" alt="image" src="https://github.com/user-attachments/assets/1ef2975e-483b-4ce7-b2e9-4f9942814ae1" />

Como se puede apreciar la interfaz es muy sencilla y simple, ya que lo que buscamos es que el usuario pueda utilizarla de manera rapida.
en la parte superior a los lados del nombre de la app, se pueden apreciar dos iconos

<img width="432" height="79" alt="image" src="https://github.com/user-attachments/assets/84c563d5-7b6b-427c-84f9-7cb81ef1bb78" />

El de la derecha es para salir de la cuenta, y el de la izquierda es para sacar el menu lateral, que explicare posteriormente.
Pero el mas importante es el boton central, que es el que da toda la funcionalidad a la app

<img width="150" height="150" alt="image" src="https://github.com/user-attachments/assets/9103a55b-6ed5-4519-aff5-7199dedb3018" />

al pulsarlo nos pedira permiso para usar la camara, y despues podremos escanear los qr de los productos para saber si tenemos alergias
aqui unas imagenes de este punto

<img width="150" height="150" alt="image" src="https://github.com/user-attachments/assets/373c2cb1-5733-48b5-993c-ec255067cd9d" />

<img width="150" height="210" alt="image" src="https://github.com/user-attachments/assets/3084dafc-da62-42b7-a7cd-82927c951b4c" />

<img width="150" height="210" alt="image" src="https://github.com/user-attachments/assets/d1d48c47-1258-419f-b500-3361dfc9d3e8" />

y como ultimo punto seria el menu desplegable
para darle mas sentido al apartado de crear usuario
y al hecho de que normalmente si vas a hacer la compra, y no vives solo o vas a quedar con tus amigos.
normalmente deberias estar atento a las alergias de los demas si vas a comprar algo que pueden tomar.
por eso el menu laterar sirve para formar grupos, que añades gente con una ID que se te asigna al crear un usuario

<img width="150" height="210" alt="image" src="https://github.com/user-attachments/assets/7cad0ad5-4f25-49f0-ad7f-6f883245b12b" />

para este punto vamos a crear rapidamente a otro usuario (pero antes copio con antelacion la ID de Antonio)
He creado ha Luis que es intolerante a la lactosa.

bien, procederemos a crear un grupo que llamaremos Readme

<img width="150" height="210" alt="image" src="https://github.com/user-attachments/assets/c83650b5-18c7-42d8-b846-1953be3261ec" />
<img width="150" height="210" alt="image" src="https://github.com/user-attachments/assets/87d55c40-54a1-4d9c-9574-eb42b597cc6b" />
<img width="150" height="210" alt="image" src="https://github.com/user-attachments/assets/a3a6107e-064b-45f6-8f2d-160b02fe7ac7" />

como vemos el grupo ya esta creado, ahora podemos agregar usuarios
(aqui la funcion de copiar tu ID ayuda bastante a pasarselo a alguien)

<img width="150" height="210" alt="image" src="https://github.com/user-attachments/assets/eab39843-9ffe-4150-afa2-ec542df3c019" />

<img width="150" height="210" alt="image" src="https://github.com/user-attachments/assets/1dfd16c7-0570-4858-b8e1-0a55cbeec6ad" />

ahora que hemos agregado a un usuario podemos ver que intregantes hay en nuestro grupo

<img width="150" height="210" alt="image" src="https://github.com/user-attachments/assets/c402f5ac-f559-4f12-99b3-bfafe041f7f8" />

<img width="150" height="210" alt="image" src="https://github.com/user-attachments/assets/21596d61-21e7-4778-97e7-4ac55198999d" />

y por ultimo, todo el motivo que este todo esta parafernalia del grupo.
Poder comprobar en grupo
aqui las imagenes

<img width="150" height="210" alt="image" src="https://github.com/user-attachments/assets/ba9aa235-06cf-497b-9a97-def867c436e1" />
<img width="150" height="210" alt="image" src="https://github.com/user-attachments/assets/ae8211eb-a971-42a3-9aba-d817d820c458" />
<img width="150" height="210" alt="image" src="https://github.com/user-attachments/assets/189eacb6-fba1-4b05-8fd0-15fa18e69870" />
<img width="150" height="210" alt="image" src="https://github.com/user-attachments/assets/833ba1f6-8d9e-48ec-8294-1bbc4e10379c" />
(con esta ultima imagen enseño una de las primeras pruebas que hice y corregi,
ya que esta app coge la lista de ingredientes del producto y lo filtra por palabras clave, 
al inicio tuve problemas con la leche con y sin lactosa)


## 📦 Distribución de aplicaciones (RA7)

Para garantizar que Allergy Control llegue a los usuarios de forma segura y profesional, se ha definido el siguiente proceso de distribución:

1. Empaquetado y Generación del Paquete (RA7.a, RA7.c)
La aplicación se empaqueta utilizando el formato Android App Bundle (.aab) mediante el entorno oficial Android Studio. Este formato es el estándar profesional actual, ya que optimiza el tamaño del archivo final descargado por el usuario, incluyendo solo los recursos (idiomas, densidades de pantalla) necesarios para su dispositivo específico.

<img width="150" height="210" alt="image" src="https://github.com/user-attachments/assets/8a9d40df-cf6e-41aa-b1d0-a3b820b112d4" />

<img width="150" height="210" alt="image" src="https://github.com/user-attachments/assets/034e78c7-6134-41e0-9e0e-0847aba794ac" />


2. Firma Digital y Seguridad (RA7.e)
Para asegurar la integridad de la app y la identidad del autor, el paquete ha sido firmado digitalmente utilizando una KeyStore privada. Se ha configurado el proceso de Google Play Billing Service para que la firma sea verificada en cada actualización, evitando así la suplantación de la aplicación por terceros malintencionados.

<img width="150" height="210" alt="image" src="https://github.com/user-attachments/assets/bed2e4d5-e665-4735-88f1-9076d2e1ba14" />

<img width="150" height="210" alt="image" src="https://github.com/user-attachments/assets/c4ce21a1-f034-4bac-8ec1-483350fda148" />

<img width="150" height="210" alt="image" src="https://github.com/user-attachments/assets/e61144f7-d2e3-4e26-a7e1-dfaf11e194f0" />

<img width="150" height="210" alt="image" src="https://github.com/user-attachments/assets/7683a68a-74c8-462e-99cb-99e5233c859f" />

<img width="150" height="210" alt="image" src="https://github.com/user-attachments/assets/ad6737b9-138d-4f95-b647-8a43df088248" />



4. Personalización y Canales de Distribución (RA7.b, RA7.h)
Se han definido dos canales de distribución principales:

Canal de Producción (Google Play Store): Uso de la consola de Google Play para personalizar la ficha técnica, iconos, capturas de pantalla y descripción profesional.

Canal de Pruebas (Firebase App Distribution): Uso de herramientas externas para enviar versiones beta a un grupo de probadores (testers) antes del lanzamiento oficial.

4. Instalación Desatendida y Desinstalación (RA7.f, RA7.g)
Instalación: Al distribuirse mediante la Store oficial, la instalación es desatendida. El sistema operativo gestiona automáticamente los permisos y la descompresión del paquete sin intervención técnica del usuario.

Desinstalación: Se ha configurado el manifiesto de la aplicación para que, al desinstalarse, el sistema limpie de forma automática la caché y los datos locales, cumpliendo con las normativas de privacidad y optimización de almacenamiento.

5. Uso de Herramientas Externas (RA7.d)
Utilice firebase como base de datos para almacenar los grupos, usuarios y productos
punto que decir, los usuarios son en verdad correos que pongo el @ fuera esto, por lo tanto no se pueden repetir nombres
<img width="500" height="650" alt="image" src="https://github.com/user-attachments/assets/dbf4b894-b7c6-4b54-b77f-4b34ff16be41" />

## 🧪 Pruebas Avanzadas (RA8)
Para asegurar la fiabilidad de una aplicación destinada a la salud (alergias alimentarias), se ha seguido un protocolo de pruebas que garantiza la estabilidad del sistema:

1. Pruebas de Regresión (RA8.c)
Se han implementado ciclos de pruebas de regresión tras cada actualización de funcionalidades clave (como la implementación del sistema de grupos) y por eso me di cuenta que al intentar añadir el historial me daban varios errores.

Otro ejemplo es que tras añadir la lógica de "Modo Grupal", volvi a ejecutar los casos de prueba de "Escaneo Individual" y "Registro de Usuario".

Objetivo: Verificar que la nueva lógica de filtrado compartido no rompió la capacidad de la app de detectar alérgenos de forma personal.

2. Pruebas de Volumen y Estrés (RA8.d)
Volumen: Se realizaron pruebas de carga en la base de datos de Firestore simulando productos con listas de ingredientes extensas (más de 100 palabras) para asegurar que el algoritmo de búsqueda por palabras clave (.contains) no sufriera retardos perceptibles.


3. Pruebas de Seguridad (RA8.e) -
La seguridad se ha centrado en la protección de la identidad y la integridad de los grupos:

Autenticación: Se utiliza Firebase Authentication, lo que garantiza que las contraseñas están cifradas y gestionadas por Google. El acceso a la aplicación está restringido exclusivamente a usuarios autenticados.

Privacidad de Grupos: El sistema de grupos utiliza identificadores únicos (UID). Las pruebas de seguridad consistieron en verificar que un usuario solo puede acceder a la información de los grupos donde ha sido invitado.

Ofuscación de IDs: En la interfaz (Drawer lateral), el ID del usuario se muestra recortado (usando .take(12)) para evitar que miradas indiscretas puedan copiar el identificador completo sin el consentimiento del usuario, quien debe pulsar el botón "Copiar" deliberadamente.

4. Análisis de Uso de Recursos (RA8.f)
Se ha optimizado la aplicación para un consumo mínimo de recursos del dispositivo:

Memoria RAM: Uso de la librería Coil para la gestión de imágenes. Coil gestiona automáticamente el caching y libera la memoria de las imágenes que ya no están en pantalla, evitando fugas de memoria (memory leaks).

Batería: El escáner de cámara (CameraX) se vincula al ciclo de vida (Lifecycle) de la pantalla. Esto significa que la cámara se apaga instantáneamente cuando el usuario sale de la pantalla de escaneo o minimiza la app, ahorrando energía significativamente.

## Informes (RA5)
Para ser totalmente sincero, intenté hacerlo cuando ya tenía la app prácticamente hecha y se me complicó muchísimo. No paraban de salirme errores por todas partes y me acabé estresando por no encontrar una solución, así que decidí dejarlo y volví al último commit, ya que no me parecia algo necesario en la aplicación.

##  Paleta de Colores Corporativa
intente que la paleta de colores fuera proxima al verde porque es u color que representa la salud,
aunque para el apartado de grupos he usado un color mas oscuro para que fuera evidente el cambio
*  **Safe Green** (`#27AE60`) - Productos aptos para el consumo.
*  **Warning Red** (`#E74C3C`) - Alerta de presencia de alérgenos.
*  **Slate Gray** (`#607D8B`) - Tipografía secundaria y elementos de soporte.
*  **Background Mint** (`#F1F8F5`) - Color de fondo principal para un estilo limpio y sanitario.
*  **Card White** (`#FFFFFF`) - Superficie de los contenedores de información.

## 💻 Instalación
1. Clona este repositorio:
   ```bash
  git clone https://github.com/AlbertoGarciaVelatta/allergy-control.git (https://github.com/AlbertoGarciaVelatta/allergy-control.git)
