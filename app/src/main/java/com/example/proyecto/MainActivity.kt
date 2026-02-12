package com.example.proyecto

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.proyecto.ui.theme.*
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.proyecto.ui.theme.ProyectoTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

//la paleta de colores se la app


@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inicialización de gestores de Firebase
        val authManager = AuthManager()
        val firebaseAuth = FirebaseAuth.getInstance()

        setContent {
            ProyectoTheme {
                val context = LocalContext.current
                val clipboardManager = LocalClipboardManager.current
                val scope = rememberCoroutineScope()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

                // Inyección de ViewModels
                val productViewModel: ProductViewModel = viewModel()
                val groupViewModel: GroupViewModel = viewModel()

                // Estados reactivos para la navegación
                var userUid by remember { mutableStateOf(firebaseAuth.currentUser?.uid) }
                var isLoginMode by remember { mutableStateOf(true) }
                var currentTab by remember { mutableStateOf("home") }
                var mostrarDialogoGrupo by remember { mutableStateOf(false) }
                var mostrarDialogoInvitacion by remember { mutableStateOf<String?>(null) }
                var miembrosAMostrar by remember { mutableStateOf<List<String>?>(null) }

                // ESTADO NUEVO: Controla si se ve la cámara
                var mostrarScanner by remember { mutableStateOf(false) }

                // --- LÓGICA DE COLORES DINÁMICOS ---
                val grupoActivo = productViewModel.grupoActivo.value
                val estaEnModoGrupo = grupoActivo != null
                // Si hay grupo usamos GroupPurple, si no SafeGreen
                val colorDinamico = if (estaEnModoGrupo) GroupPurple else SafeGreen
                // El fondo debajo del botón cambia según el modo
                val fondoDinamico = if (estaEnModoGrupo) Color(0xFFF3E5F5) else BackgroundMint

                // Observador del mensaje de resultado del escáner
                val resultMsg by productViewModel.scanResultMessage

                // aqui fuerza que si no hay usuario, forzamos pantalla de Registro/Login
                if (userUid == null) {
                    RegistroMinimoScreen(
                        isLoginMode = isLoginMode,
                        onSwitchMode = { isLoginMode = !isLoginMode },
                        onAction = { nom, pass, alergias ->
                            if (isLoginMode) {
                                authManager.loguearUsuario(nom, pass) { exito, error ->
                                    if (exito) userUid = firebaseAuth.currentUser?.uid
                                    else Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                }
                            } else {
                                authManager.registrarUsuario(nom, pass, alergias) { exito, error ->
                                    if (exito) userUid = firebaseAuth.currentUser?.uid
                                    else Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                } else {
                    // Una vez logueado, disparamos la carga de datos de Firebase
                    LaunchedEffect(userUid) {
                        userUid?.let {
                            productViewModel.cargarPerfilUsuario(it)
                            productViewModel.escucharHistorial(it)
                            groupViewModel.cargarGrupos(it)
                        }
                    }

                    // --- NUEVO: ESCUCHA POR VOZ SIEMPRE ACTIVA ---
                    val permissionLauncherVoz = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { concedido ->
                        if (concedido) {
                            productViewModel.iniciarEscuchaContinua(context) {
                                mostrarScanner = true // Si detecta "Escanear", abre la cámara
                            }
                        }
                    }

                    // Lanzamos la petición de micro al inicio
                    LaunchedEffect(Unit) {
                        permissionLauncherVoz.launch(Manifest.permission.RECORD_AUDIO)
                    }

                    // Gestión de permisos de cámara corrigiendo el error de setContent
                    val permissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { concedido ->
                        if (concedido) {
                            mostrarScanner = true
                        } else {
                            Toast.makeText(context, "Permiso de cámara necesario", Toast.LENGTH_SHORT).show()
                        }
                    }

                    // --- SECCIÓN DE DIÁLOGOS EMERGENTES ---
                    if (mostrarDialogoGrupo) {
                        DialogoCrearGrupo(onDismiss = { mostrarDialogoGrupo = false }, onConfirm = { nombre -> groupViewModel.crearGrupo(nombre, userUid!!) })
                    }

                    mostrarDialogoInvitacion?.let { groupId ->
                        DialogoInvitarUsuario(onDismiss = { mostrarDialogoInvitacion = null }, onConfirm = { amigoId -> groupViewModel.invitarUsuario(context, groupId, amigoId) })
                    }

                    miembrosAMostrar?.let { lista ->
                        DialogoVerMiembros(nombres = lista, onDismiss = { miembrosAMostrar = null })
                    }

                    //Menú lateral + Contenido
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet(
                                modifier = Modifier.width(320.dp),
                                drawerContainerColor = fondoDinamico // Color de fondo del menú
                            ) {
                                DrawerContent(
                                    userUid = userUid,
                                    productViewModel = productViewModel,
                                    groupViewModel = groupViewModel,
                                    clipboardManager = clipboardManager,
                                    context = context,
                                    misGrupos = groupViewModel.misGrupos.value,
                                    colorApp = GroupPurple,
                                    onCreateGroupClick = {
                                        mostrarDialogoGrupo = true
                                        scope.launch { drawerState.close() }
                                    },
                                    onInviteClick = { mostrarDialogoInvitacion = it },
                                    onDeleteGroupClick = { groupViewModel.eliminarGrupo(context, it) },
                                    onViewMembersClick = { miembrosAMostrar = it },
                                    onCloseDrawer = { scope.launch { drawerState.close() } }
                                )
                            }
                        }
                    ) {
                        Scaffold(
                            containerColor = fondoDinamico, // CAMBIO: Color debajo del botón
                            topBar = {
                                CenterAlignedTopAppBar(
                                    title = { Text("ALLERGY CONTROL", fontWeight = FontWeight.Black, color = Color.White) },
                                    navigationIcon = {
                                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                            Icon(Icons.Default.Menu, null, tint = Color.White)
                                        }
                                    },
                                    actions = {
                                        // Icono que indica que la app está escuchando
                                        if (productViewModel.estaEscuchando.value) {
                                            Icon(Icons.Default.GraphicEq, null, tint = Color.White, modifier = Modifier.padding(end = 8.dp))
                                        }
                                        IconButton(onClick = { firebaseAuth.signOut(); userUid = null }) {
                                            Icon(Icons.Default.Logout, null, tint = Color.White)
                                        }
                                    },
                                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = colorDinamico)
                                )
                            },
                            bottomBar = {
                                NavigationBar(containerColor = Color.White) {
                                    NavigationBarItem(
                                        selected = currentTab == "home",
                                        onClick = { currentTab = "home" },
                                        label = { Text("Inicio") },
                                        icon = { Icon(Icons.Default.QrCodeScanner, null) },
                                        colors = NavigationBarItemDefaults.colors(selectedIconColor = colorDinamico, selectedTextColor = colorDinamico)
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == "stats",
                                        onClick = { currentTab = "stats" },
                                        label = { Text("Informes") },
                                        icon = { Icon(Icons.Default.BarChart, null) },
                                        colors = NavigationBarItemDefaults.colors(selectedIconColor = colorDinamico, selectedTextColor = colorDinamico)
                                    )
                                }
                            }
                        ) { padding ->
                            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                                // Cambiamos el contenido según la pestaña seleccionada
                                when (currentTab) {
                                    "home" -> {
                                        MainScreenContent(
                                            isLoading = productViewModel.isLoading.value,
                                            grupoActivo = productViewModel.grupoActivo.value,
                                            colorActual = colorDinamico, // Pasamos el color para el botón
                                            onDesactivarGrupo = { productViewModel.grupoActivo.value = null }
                                        ) {
                                            permissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    }
                                    "stats" -> {
                                        InformeSaludScreen(productViewModel)
                                    }
                                }

                                // CAPA DEL SCANNER: Solo aparece si mostrarScanner es true
                                if (mostrarScanner) {
                                    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                                        ScannerScreen { code ->
                                            productViewModel.escanearProducto(code)
                                            mostrarScanner = false // Cerramos el scanner al detectar código
                                        }
                                        // Botón para cerrar el scanner manualmente
                                        IconButton(
                                            onClick = { mostrarScanner = false },
                                            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                                        ) {
                                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(32.dp))
                                        }
                                    }
                                }

                                // Mantenemos la animación de resultado (visible siempre que haya mensaje)
                                AnimatedVisibility(
                                    visible = resultMsg != null,
                                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                                ) {
                                    resultMsg?.let { msg ->
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            ResultadoVisualGigante(
                                                mensaje = msg,
                                                producto = productViewModel.ultimoProductoEscaneado.value,
                                                onDismiss = { productViewModel.scanResultMessage.value = null }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Menú lateral
 * Gestiona el perfil del usuario y la lista de grupos.
 */
@Composable
fun DrawerContent(
    userUid: String?,
    productViewModel: ProductViewModel,
    groupViewModel: GroupViewModel,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    context: android.content.Context,
    misGrupos: List<GroupProfile>,
    colorApp: Color, // NUEVO
    onCreateGroupClick: () -> Unit,
    onInviteClick: (String) -> Unit,
    onDeleteGroupClick: (String) -> Unit,
    onViewMembersClick: (List<String>) -> Unit,
    onCloseDrawer: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Cabecera con el ID del usuario
        item {
            Box(modifier = Modifier.fillMaxWidth().background(colorApp).padding(24.dp)) {
                Column {
                    Text("TU ID DE USUARIO", color = Color.White.copy(0.6f), fontSize = 10.sp)
                    Text(userUid?.take(15) ?: "", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(userUid ?: ""))
                            Toast.makeText(context, "ID Copiado", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.2f))
                    ) { Text("COPIAR", fontSize = 10.sp) }
                }
            }
        }

        // Cabecera de Grupos
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("MIS GRUPOS", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), color = colorApp)
                IconButton(onClick = onCreateGroupClick) { Icon(Icons.Default.AddCircle, null, tint = colorApp) }
            }
        }

// Lista de los grupos
        items(misGrupos) { grupo ->
            val esAdmin = grupo.adminId == userUid
            NavigationDrawerItem(
                label = {
                    Column {
                        Text(grupo.nombre, fontWeight = FontWeight.Bold)
                        TextButton(
                            onClick = { groupViewModel.obtenerNombresMiembros(grupo.miembros) { onViewMembersClick(it) } },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Visibility, null, modifier = Modifier.size(12.dp), tint = colorApp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ver ${grupo.miembros.size} miembros", fontSize = 10.sp, color = colorApp)
                        }
                    }
                },
                selected = productViewModel.grupoActivo.value?.id == grupo.id,
                onClick = { productViewModel.grupoActivo.value = grupo; onCloseDrawer() },
                icon = { Icon(Icons.Default.Groups, null) },
                colors = NavigationDrawerItemDefaults.colors(selectedIconColor = colorApp, selectedTextColor = colorApp),
                badge = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onInviteClick(grupo.id) }) { Icon(Icons.Default.PersonAdd, null, Modifier.size(20.dp), tint = colorApp) }
                        if (esAdmin) {
                            IconButton(onClick = { onDeleteGroupClick(grupo.id) }) { Icon(Icons.Default.Delete, null, Modifier.size(20.dp), tint = WarningRed) }
                        }
                    }
                },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

/**
 * Pantalla central
 * Muestra el botón de escaneo y el estado del modo actual.
 */
@Composable
fun MainScreenContent(
    isLoading: Boolean,
    grupoActivo: GroupProfile?,
    colorActual: Color, // NUEVO
    onDesactivarGrupo: () -> Unit,
    onScanClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(60.dp), color = colorActual)
        } else {
            // Indicador de grupo activo (Modo Colaborativo)
            if (grupoActivo != null) {
                InputChip(
                    selected = true,
                    onClick = onDesactivarGrupo,
                    label = { Text("Grupo: ${grupoActivo.nombre}") },
                    trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) },
                    colors = InputChipDefaults.inputChipColors(selectedContainerColor = colorActual.copy(0.1f))
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            // Botón central de escaneo ( y la opcion de cambiar de color según el modo)
            Button(
                onClick = onScanClick, modifier = Modifier.size(220.dp),
                shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = colorActual),
                elevation = ButtonDefaults.buttonElevation(12.dp)
            ) { Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(100.dp)) }
            Spacer(modifier = Modifier.height(24.dp))
            Text(if (grupoActivo != null) "MODO GRUPO ACTIVO" else "MODO PERSONAL", fontWeight = FontWeight.Bold, color = colorActual)
            Text("Di 'ESCANEAR' para abrir la cámara", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

/**
 * Resultado Visual (Alerta de Alérgenos)
 * Muestra si el producto es APTO o NO APTO con código de colores.
 */
@Composable
fun ResultadoVisualGigante(mensaje: String, producto: Product?, onDismiss: () -> Unit) {
    // Lógica binaria para determinar el color de la alerta
    val esApto = mensaje.contains("APTO") && !mensaje.contains("NO APTO")
    val colorP = if (esApto) SafeGreen else WarningRed

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
                Text("Alérgenos detectados: ${producto?.allergens?.joinToString(", ")}", color = WarningRed, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp), textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = colorP), modifier = Modifier.fillMaxWidth()) {
                Text("CERRAR", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DialogoVerMiembros(nombres: List<String>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Integrantes del Grupo", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                items(nombres) { nombre ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountCircle, null, tint = SafeGreen, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(nombre, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("CERRAR", color = SafeGreen) } }
    )
}

@Composable
fun DialogoCrearGrupo(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var n by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Nuevo Grupo") }, text = { OutlinedTextField(value = n, onValueChange = { n = it }, label = { Text("Nombre") }) }, confirmButton = { Button(onClick = { onConfirm(n); onDismiss() }) { Text("Crear") } })
}

@Composable
fun DialogoInvitarUsuario(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var id by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Invitar") }, text = { OutlinedTextField(value = id, onValueChange = { id = it }, label = { Text("ID Usuario") }) }, confirmButton = { Button(onClick = { onConfirm(id); onDismiss() }) { Text("Añadir") } })
}