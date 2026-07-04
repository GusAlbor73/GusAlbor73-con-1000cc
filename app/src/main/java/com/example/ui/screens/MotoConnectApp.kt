package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.*
import com.example.R
import com.example.ui.MotoConnectViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Biker Theme Colors - Matching Logo Exactly
val ThemeDarkGray = Color(0xFF0F0F12)
val ThemeCardGray = Color(0xFF1B1B20)
val ThemeOrangeAccent = Color(0xFFDE7A3E)
val ThemeNeonBlue = Color(0xFF00E5FF)
val ThemeGreenSuccess = Color(0xFF00E676)
val ThemeRedDanger = Color(0xFFFF1744)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotoConnectApp(viewModel: MotoConnectViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val latestNotification by viewModel.latestPushNotification.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Control de navegación local básica (en lugar de complejas rutas para evitar fallos,
    // usamos una pantalla activa controlada por estado, lo cual es ultra estable y rápido)
    // "SELECTOR", "MAIN_APP"
    var currentScreen by remember { mutableStateOf("SELECTOR") }

    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            currentScreen = "SELECTOR"
        } else {
            currentScreen = "MAIN_APP"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeDarkGray)
    ) {
        Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
            when (screen) {
                "SELECTOR" -> ProfileSelectorScreen(
                    viewModel = viewModel,
                    onProfileSelected = { user ->
                        viewModel.loginAsUser(user)
                    }
                )
                "MAIN_APP" -> MainAppHub(
                    viewModel = viewModel,
                    currentUser = currentUser!!
                )
            }
        }

        // --- GLOBAL FLOATING PUSH NOTIFICATION SIMULATOR ---
        AnimatedVisibility(
            visible = latestNotification != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp, start = 16.dp, end = 16.dp)
                .zIndex(100f)
        ) {
            latestNotification?.let { alert ->
                PushNotificationBanner(
                    alert = alert,
                    onDismiss = { viewModel.clearLatestNotification() }
                )
            }
        }
    }
}

@Composable
fun PushNotificationBanner(
    alert: SecurityAlertEntity,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("push_notification_card")
            .clickable { onDismiss() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1616)),
        border = BorderStroke(1.5.dp, ThemeRedDanger),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(ThemeRedDanger.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (alert.type) {
                        "WEATHER_HAZARD" -> Icons.Default.CloudQueue
                        "ROAD_CLOSED" -> Icons.Default.Block
                        "ACCIDENT" -> Icons.Default.Warning
                        else -> Icons.Default.NotificationsActive
                    },
                    contentDescription = "Alerta",
                    tint = ThemeRedDanger
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1.0f)) {
                Text(
                    text = alert.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = alert.message,
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = Color.Gray
                )
            }
        }
    }
}

// --- SCREEN 1: PROFILE SELECTOR ---
@Composable
fun ProfileSelectorScreen(
    viewModel: MotoConnectViewModel,
    onProfileSelected: (UserEntity) -> Unit
) {
    val users by viewModel.allUsers.collectAsStateWithLifecycle()
    var showRegisterDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(30.dp))
        // Logo / Icono Principal
        Box(
            modifier = Modifier
                .size(110.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(ThemeOrangeAccent.copy(alpha = 0.4f), Color.Transparent)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.img_app_icon),
                contentDescription = "1000cc Biker Logo",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .border(2.dp, ThemeOrangeAccent, CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "1000cc Biker",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Asistencia, Servicios y Ruta Segura para Alta Cilindrada",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "SELECCIONA TU PERFIL PARA PROBAR LA APP:",
            color = ThemeOrangeAccent,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Lista de Perfiles de Prueba Pre-cargados
        if (users.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ThemeOrangeAccent)
            }
        } else {
            users.forEach { user ->
                ProfileSelectorCard(user = user, onClick = { onProfileSelected(user) })
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = { showRegisterDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("register_new_profile_button"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ThemeOrangeAccent),
            border = BorderStroke(1.5.dp, ThemeOrangeAccent)
        ) {
            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Registrar")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Registrar Nuevo Perfil", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(40.dp))
    }

    if (showRegisterDialog) {
        RegisterProfileDialog(
            onDismiss = { showRegisterDialog = false },
            onRegister = { name, email, phone, role, motorcycle, pType ->
                val newUser = UserEntity(
                    name = name,
                    email = email,
                    phone = phone,
                    role = role,
                    motorcycleModel = if (role == "BIKER") motorcycle else null,
                    providerType = if (role == "PROVIDER") pType else null,
                    latitude = 19.4326 + (Math.random() - 0.5) * 0.1,
                    longitude = -99.1332 + (Math.random() - 0.5) * 0.1
                )
                viewModel.insertUser(newUser)
                showRegisterDialog = false
            }
        )
    }
}

@Composable
fun ProfileSelectorCard(user: UserEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("profile_selector_${user.role.lowercase()}_${user.id}"),
        colors = CardDefaults.cardColors(containerColor = ThemeCardGray),
        border = BorderStroke(
            1.dp,
            when (user.role) {
                "ADMIN" -> ThemeNeonBlue
                "PROVIDER" -> ThemeOrangeAccent
                else -> Color.Gray.copy(alpha = 0.4f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.DarkGray, CircleShape)
                    .clip(CircleShape)
            ) {
                if (user.avatarUrl != null) {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = user.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = when (user.role) {
                            "ADMIN" -> Icons.Default.AdminPanelSettings
                            "PROVIDER" -> Icons.Default.Handyman
                            else -> Icons.Default.Person
                        },
                        contentDescription = "Avatar",
                        tint = Color.LightGray,
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = when (user.role) {
                        "ADMIN" -> "Administrador Master"
                        "PROVIDER" -> "Proveedor de Servicios • ${user.providerType ?: "General"}"
                        else -> "Motociclista • ${user.motorcycleModel ?: "Sin Moto"}"
                    },
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            Box(
                modifier = Modifier
                    .background(
                        color = when (user.role) {
                            "ADMIN" -> ThemeNeonBlue.copy(alpha = 0.15f)
                            "PROVIDER" -> ThemeOrangeAccent.copy(alpha = 0.15f)
                            else -> Color.White.copy(alpha = 0.08f)
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = user.role,
                    color = when (user.role) {
                        "ADMIN" -> ThemeNeonBlue
                        "PROVIDER" -> ThemeOrangeAccent
                        else -> Color.LightGray
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterProfileDialog(
    onDismiss: () -> Unit,
    onRegister: (String, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("BIKER") } // BIKER, PROVIDER
    var motorcycle by remember { mutableStateOf("") }
    var providerType by remember { mutableStateOf("Mecánico") } // Mecánico, Grúa, Repuestos

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Perfil de Prueba", color = Color.White) },
        containerColor = ThemeCardGray,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre Completo") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemeOrangeAccent,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = ThemeOrangeAccent
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("reg_name_input")
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemeOrangeAccent,
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("reg_email_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Teléfono de Contacto") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemeOrangeAccent,
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("reg_phone_input")
                )

                Text("Rol en la Plataforma:", color = Color.White, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FilterChip(
                        selected = role == "BIKER",
                        onClick = { role = "BIKER" },
                        label = { Text("Motociclista") },
                        modifier = Modifier.weight(1f).testTag("chip_biker")
                    )
                    FilterChip(
                        selected = role == "PROVIDER",
                        onClick = { role = "PROVIDER" },
                        label = { Text("Proveedor") },
                        modifier = Modifier.weight(1f).testTag("chip_provider")
                    )
                }

                if (role == "BIKER") {
                    OutlinedTextField(
                        value = motorcycle,
                        onValueChange = { motorcycle = it },
                        label = { Text("Modelo de Motocicleta (ej. Yamaha R3)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThemeOrangeAccent,
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("reg_motorcycle_input")
                    )
                } else {
                    Text("Tipo de Proveedor:", color = Color.White, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Mecánico", "Grúa", "Repuestos").forEach { type ->
                            FilterChip(
                                selected = providerType == type,
                                onClick = { providerType = type },
                                label = { Text(type) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && email.isNotBlank()) {
                        onRegister(name, email, phone, role, motorcycle, providerType)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ThemeOrangeAccent),
                modifier = Modifier.testTag("submit_reg_button")
            ) {
                Text("Registrar", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}

// --- SCREEN 2: MAIN APP HUB (TABS CONTROL BASED ON ROLE) ---
@Composable
fun MainAppHub(
    viewModel: MotoConnectViewModel,
    currentUser: UserEntity
) {
    // Controlamos el TAB seleccionado del BottomNav
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ThemeDarkGray,
        bottomBar = {
            NavigationBar(
                containerColor = ThemeCardGray,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                val items = when (currentUser.role) {
                    "ADMIN" -> listOf(
                        Triple("Parámetros", Icons.Default.Tune, 0),
                        Triple("Controversias", Icons.Default.Gavel, 1),
                        Triple("Usuarios", Icons.Default.Group, 2)
                    )
                    "PROVIDER" -> listOf(
                        Triple("Dashboard", Icons.Default.Dashboard, 0),
                        Triple("Servicios", Icons.Default.Handyman, 1),
                        Triple("Historial", Icons.Default.History, 2)
                    )
                    else -> listOf(
                        Triple("Ruta & SOS", Icons.Default.Navigation, 0),
                        Triple("Servicios", Icons.Default.Handyman, 1),
                        Triple("Alertas", Icons.Default.CrisisAlert, 2),
                        Triple("Mis Viajes", Icons.Default.History, 3)
                    )
                }

                items.forEach { (label, icon, index) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(imageVector = icon, contentDescription = label) },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = if (currentUser.role == "ADMIN") ThemeNeonBlue else ThemeOrangeAccent,
                            selectedTextColor = if (currentUser.role == "ADMIN") ThemeNeonBlue else ThemeOrangeAccent,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = (if (currentUser.role == "ADMIN") ThemeNeonBlue else ThemeOrangeAccent).copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_tab_$index")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentUser.role) {
                "ADMIN" -> when (selectedTab) {
                    0 -> AdminParametersTab(viewModel)
                    1 -> AdminDisputesTab(viewModel)
                    else -> AdminUsersTab(viewModel)
                }
                "PROVIDER" -> when (selectedTab) {
                    0 -> ProviderDashboardTab(viewModel, currentUser)
                    1 -> ProviderServicesTab(viewModel, currentUser)
                    else -> ProviderHistoryTab(viewModel, currentUser)
                }
                else -> when (selectedTab) {
                    0 -> BikerMapTab(viewModel, currentUser)
                    1 -> BikerServicesTab(viewModel, currentUser)
                    2 -> BikerAlertsTab(viewModel, currentUser)
                    else -> BikerHistoryTab(viewModel, currentUser)
                }
            }
        }
    }
}


// ==========================================
// ========== 1. BIKER SCREENS/TABS ==========
// ==========================================

@Composable
fun BikerMapTab(viewModel: MotoConnectViewModel, biker: UserEntity) {
    val isTripActive by viewModel.isTripActive.collectAsStateWithLifecycle()
    val bLat by viewModel.bikerLatitude.collectAsStateWithLifecycle()
    val bLng by viewModel.bikerLongitude.collectAsStateWithLifecycle()
    val sosState by viewModel.sosState.collectAsStateWithLifecycle()
    val sosDistance by viewModel.sosProviderDistance.collectAsStateWithLifecycle()
    val appConfig by viewModel.appConfig.collectAsStateWithLifecycle()
    val alerts by viewModel.securityAlerts.collectAsStateWithLifecycle()
    val users by viewModel.allUsers.collectAsStateWithLifecycle()

    var showSOSConfirmation by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Sponsored Advertisement Banner!
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ad_banner_card"),
                colors = CardDefaults.cardColors(containerColor = ThemeCardGray),
                border = BorderStroke(1.dp, ThemeNeonBlue.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = "Anuncio",
                        tint = ThemeNeonBlue,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = appConfig.adText,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. Profile Quick Stats
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "¡Hola, ${biker.name.split(" ").first()}!",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${biker.motorcycleModel} • Lat: %.4f, Lng: %.4f".format(bLat, bLng),
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
                IconButton(
                    onClick = { viewModel.logout() },
                    modifier = Modifier.background(Color.White.copy(alpha = 0.08f), CircleShape).testTag("logout_button")
                ) {
                    Icon(imageVector = Icons.Default.Logout, contentDescription = "Cerrar Sesión", tint = Color.White)
                }
            }
        }

        // 3. Interactive Highway Simulation Map Canvas
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                border = BorderStroke(1.5.dp, Color.DarkGray)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Custom Canvas simulating a Map/Highway
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Draw Grid lines
                        for (i in 0..w.toInt() step 60) {
                            drawLine(Color.DarkGray.copy(alpha = 0.15f), Offset(i.toFloat(), 0f), Offset(i.toFloat(), h))
                        }
                        for (j in 0..h.toInt() step 60) {
                            drawLine(Color.DarkGray.copy(alpha = 0.15f), Offset(0f, j.toFloat()), Offset(w, j.toFloat()))
                        }

                        // Draw Simulated Biker Highway Route (Highway curve)
                        val path = Path().apply {
                            moveTo(w * 0.1f, h * 0.9f)
                            quadraticTo(w * 0.4f, h * 0.8f, w * 0.5f, h * 0.5f)
                            quadraticTo(w * 0.6f, h * 0.2f, w * 0.9f, h * 0.1f)
                        }
                        drawPath(path, Color.Gray.copy(alpha = 0.5f), style = Stroke(width = 24f))
                        drawPath(path, Color.Yellow.copy(alpha = 0.8f), style = Stroke(width = 4f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)))

                        // Draw Security Alerts in map space
                        alerts.forEachIndexed { idx, alert ->
                            // Map the alerts dynamically based on relative coords to biker
                            val relLat = alert.latitude - 19.4326
                            val relLng = alert.longitude - (-99.1332)
                            val alertX = (w / 2) + (relLng * 2000).toFloat()
                            val alertY = (h / 2) - (relLat * 2000).toFloat()

                            if (alertX in 0f..w && alertY in 0f..h) {
                                // Draw Alert Hazard Symbol (Triangle)
                                val triPath = Path().apply {
                                    moveTo(alertX, alertY - 14f)
                                    lineTo(alertX - 14f, alertY + 14f)
                                    lineTo(alertX + 14f, alertY + 14f)
                                    close()
                                }
                                drawPath(triPath, ThemeRedDanger)
                            }
                        }

                        // Draw Service Providers in map space
                        users.filter { it.role == "PROVIDER" }.forEach { p ->
                            val relLat = (p.latitude ?: 19.4326) - bLat
                            val relLng = (p.longitude ?: -99.1332) - bLng
                            val pX = (w / 2) + (relLng * 4000).toFloat()
                            val pY = (h / 2) - (relLat * 4000).toFloat()

                            if (pX in 0f..w && pY in 0f..h) {
                                drawCircle(
                                    color = ThemeNeonBlue,
                                    radius = 10f,
                                    center = Offset(pX, pY)
                                )
                                drawCircle(
                                    color = ThemeNeonBlue.copy(alpha = 0.2f),
                                    radius = 24f,
                                    center = Offset(pX, pY),
                                    style = Stroke(width = 2f)
                                )
                            }
                        }

                        // Draw biker (self) at center of the frame
                        drawCircle(
                            color = ThemeOrangeAccent,
                            radius = 12f,
                            center = Offset(w / 2, h / 2)
                        )
                        drawCircle(
                            color = ThemeOrangeAccent.copy(alpha = 0.25f),
                            radius = 35f,
                            center = Offset(w / 2, h / 2)
                        )
                    }

                    // Floating GPS/Map details overlays
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (isTripActive) ThemeGreenSuccess else Color.Gray, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isTripActive) "EN CARRETERA (ACTIVO)" else "GPS EN REPOSO",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Road trip toggler overlay
                    Button(
                        onClick = { viewModel.toggleRoadTrip() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTripActive) ThemeRedDanger else ThemeOrangeAccent
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .testTag("toggle_road_trip_button")
                    ) {
                        Icon(
                            imageVector = if (isTripActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Ruta"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isTripActive) "Detener Viaje" else "Iniciar Viaje",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // SOS Active Emergency Tracker Panel
        if (sosState != "IDLE") {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sos_status_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1414)),
                    border = BorderStroke(1.5.dp, ThemeRedDanger)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Emergency,
                                contentDescription = "S.O.S",
                                tint = ThemeRedDanger,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "EMERGENCIA S.O.S ACTIVA",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                            if (sosState == "SEARCHING" || sosState == "ASSIGNED") {
                                CircularProgressIndicator(
                                    color = ThemeRedDanger,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = when (sosState) {
                                "SEARCHING" -> "Buscando proveedor de asistencia vial o mecánico más cercano a tu ubicación..."
                                "ASSIGNED" -> "¡Proveedor asignado! Se dirige a tu ubicación actual en carretera."
                                "ARRIVED" -> "¡El proveedor ha llegado al lugar! Revisa tu motocicleta y confirma la transacción."
                                else -> ""
                            },
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )

                        if (sosState == "ASSIGNED") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Distancia aproximada: %.1f km".format(sosDistance),
                                color = ThemeNeonBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            // Linear progress representing distance getting smaller
                            LinearProgressIndicator(
                                progress = { (sosDistance / 5.0).coerceIn(0.0, 1.0).toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = ThemeRedDanger,
                                trackColor = Color.DarkGray
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (sosState == "ARRIVED") {
                                Button(
                                    onClick = { viewModel.completeSOSTransaction() },
                                    colors = ButtonDefaults.buttonColors(containerColor = ThemeGreenSuccess),
                                    modifier = Modifier.weight(1f).testTag("confirm_sos_btn")
                                ) {
                                    Text("Confirmar y Pagar $800.00", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { viewModel.cancelSOS() },
                                    border = BorderStroke(1.dp, Color.Gray),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                                    modifier = Modifier.weight(1f).testTag("cancel_sos_btn")
                                ) {
                                    Text("Cancelar")
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // SOS Trigger Button
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSOSConfirmation = true }
                        .testTag("sos_card_trigger"),
                    colors = CardDefaults.cardColors(containerColor = ThemeRedDanger),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmergencyShare,
                            contentDescription = "Auxilio SOS",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "BOTÓN DE AUXILIO SOS",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Grúa o auxilio mecánico urgente en carretera en un solo toque",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Ir",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }

    if (showSOSConfirmation) {
        AlertDialog(
            onDismissRequest = { showSOSConfirmation = false },
            title = { Text("¿Confirmas solicitud de S.O.S urgente?", color = Color.White) },
            containerColor = ThemeCardGray,
            text = {
                Text(
                    "Se enviará tu geolocalización actual (Lat: %.4f, Lng: %.4f) al proveedor más cercano. La tarifa base de emergencia es de $800.00 pesos MXN.".format(bLat, bLng),
                    color = Color.LightGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.triggerSOS()
                        showSOSConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeRedDanger),
                    modifier = Modifier.testTag("confirm_sos_popup_btn")
                ) {
                    Text("¡S.O.S Enviar!", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSOSConfirmation = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun BikerServicesTab(viewModel: MotoConnectViewModel, biker: UserEntity) {
    val bLat by viewModel.bikerLatitude.collectAsStateWithLifecycle()
    val bLng by viewModel.bikerLongitude.collectAsStateWithLifecycle()
    val services by viewModel.allServices.collectAsStateWithLifecycle()
    val users by viewModel.allUsers.collectAsStateWithLifecycle()

    var selectedCategory by remember { mutableStateOf("Mecánica") }
    var searchKeyword by remember { mutableStateOf("") }

    var serviceToHire by remember { mutableStateOf<ServiceEntity?>(null) }
    var checkoutStep by remember { mutableStateOf(false) }

    // Card Details for payment
    var cardNumber by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvc by remember { mutableStateOf("") }

    val categories = listOf("Mecánica", "Grúas", "Repuestos")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Proveedores y Servicios",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Busca, contrata y paga de manera segura en tu área",
            color = Color.Gray,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        OutlinedTextField(
            value = searchKeyword,
            onValueChange = { searchKeyword = it },
            placeholder = { Text("Buscar servicio o repuesto...", color = Color.Gray) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar", tint = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = ThemeOrangeAccent,
                unfocusedBorderColor = Color.Gray
            ),
            modifier = Modifier.fillMaxWidth().testTag("service_search_bar"),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Category Badges Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            categories.forEach { cat ->
                val isSel = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = if (isSel) ThemeOrangeAccent else ThemeCardGray,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { selectedCategory = cat }
                        .padding(vertical = 10.dp)
                        .testTag("cat_chip_$cat"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cat,
                        color = if (isSel) Color.White else Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Filter Services list
        val filteredServices = services.filter {
            it.category.equals(selectedCategory, ignoreCase = true) &&
                    (searchKeyword.isEmpty() || it.name.contains(searchKeyword, ignoreCase = true) || it.description.contains(searchKeyword, ignoreCase = true))
        }

        if (filteredServices.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.SearchOff, contentDescription = "No encontrado", tint = Color.Gray, modifier = Modifier.size(50.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No se encontraron servicios en esta categoría.", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredServices) { service ->
                    val provider = users.find { it.id == service.providerId }
                    val distance = provider?.let {
                        viewModel.calculateDistance(bLat, bLng, it.latitude ?: 0.0, it.longitude ?: 0.0)
                    } ?: 0.0

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("service_card_${service.id}"),
                        colors = CardDefaults.cardColors(containerColor = ThemeCardGray)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = service.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "Ofrecido por: ${provider?.name ?: "Proveedor"}",
                                        color = ThemeNeonBlue,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = "$${service.price} MXN",
                                    color = ThemeOrangeAccent,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = service.description,
                                color = Color.LightGray,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = "Ubicación", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "A %.1f km de ti".format(distance),
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                                Button(
                                    onClick = { serviceToHire = service },
                                    colors = ButtonDefaults.buttonColors(containerColor = ThemeOrangeAccent),
                                    modifier = Modifier.testTag("hire_btn_${service.id}")
                                ) {
                                    Text("Contratar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal de contratación y Checkout
    if (serviceToHire != null) {
        val s = serviceToHire!!
        val provider = users.find { it.id == s.providerId }

        AlertDialog(
            onDismissRequest = {
                serviceToHire = null
                checkoutStep = false
            },
            containerColor = ThemeCardGray,
            title = {
                Text(
                    text = if (checkoutStep) "Pago Seguro y Confiable" else "Detalle de Contratación",
                    color = Color.White
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!checkoutStep) {
                        Text(text = "Servicio: ${s.name}", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(text = s.description, color = Color.LightGray, fontSize = 13.sp)
                        Text(text = "Proveedor: ${provider?.name}", color = ThemeNeonBlue, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Costo del Servicio:", color = Color.Gray)
                            Text("$${s.price} MXN", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Pasarela de pago simulada
                        Text("Ingresa los datos para realizar la transacción segura (Simulado):", color = Color.LightGray, fontSize = 12.sp)
                        OutlinedTextField(
                            value = cardNumber,
                            onValueChange = { if (it.length <= 16) cardNumber = it },
                            label = { Text("Número de Tarjeta (16 dígitos)") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ThemeOrangeAccent),
                            modifier = Modifier.fillMaxWidth().testTag("checkout_card_num")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = cardExpiry,
                                onValueChange = { cardExpiry = it },
                                label = { Text("Vencimiento") },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ThemeOrangeAccent),
                                modifier = Modifier.weight(1f).testTag("checkout_card_exp")
                            )
                            OutlinedTextField(
                                value = cardCvc,
                                onValueChange = { cardCvc = it },
                                label = { Text("CVC") },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ThemeOrangeAccent),
                                modifier = Modifier.weight(1f).testTag("checkout_card_cvc")
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (!checkoutStep) {
                    Button(
                        onClick = { checkoutStep = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeOrangeAccent),
                        modifier = Modifier.testTag("go_to_checkout_btn")
                    ) {
                        Text("Ir a Pagar", color = Color.White)
                    }
                } else {
                    Button(
                        onClick = {
                            if (cardNumber.length == 16) {
                                viewModel.hireService(s, bLat, bLng)
                                serviceToHire = null
                                checkoutStep = false
                                cardNumber = ""
                                cardExpiry = ""
                                cardCvc = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeGreenSuccess),
                        modifier = Modifier.testTag("submit_payment_btn")
                    ) {
                        Text("Confirmar Transacción", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    serviceToHire = null
                    checkoutStep = false
                }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun BikerAlertsTab(viewModel: MotoConnectViewModel, biker: UserEntity) {
    val alerts by viewModel.securityAlerts.collectAsStateWithLifecycle()
    var showReportDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Alertas de Seguridad",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Reportes preventivos de carretera en tiempo real",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            Button(
                onClick = { showReportDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = ThemeRedDanger),
                modifier = Modifier.testTag("report_hazard_btn")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Reportar")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reportar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (alerts.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay alertas activas en carretera actualmente.", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(alerts) { alert ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ThemeCardGray),
                        border = BorderStroke(1.dp, Color.DarkGray)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(ThemeRedDanger.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (alert.type) {
                                        "ROAD_CLOSED" -> Icons.Default.Block
                                        "WEATHER_HAZARD" -> Icons.Default.CloudQueue
                                        "ACCIDENT" -> Icons.Default.Warning
                                        else -> Icons.Default.Campaign
                                    },
                                    contentDescription = "Tipo",
                                    tint = ThemeRedDanger
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = alert.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = alert.message,
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Reportado hace unos momentos • Lat: %.4f, Lng: %.4f".format(alert.latitude, alert.longitude),
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        var rTitle by remember { mutableStateOf("") }
        var rMessage by remember { mutableStateOf("") }
        var rType by remember { mutableStateOf("ROAD_CLOSED") }

        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Reportar Alerta en Carretera", color = Color.White) },
            containerColor = ThemeCardGray,
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = rTitle,
                        onValueChange = { rTitle = it },
                        label = { Text("Título de la alerta") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ThemeRedDanger),
                        modifier = Modifier.fillMaxWidth().testTag("report_title_input")
                    )
                    OutlinedTextField(
                        value = rMessage,
                        onValueChange = { rMessage = it },
                        label = { Text("Descripción o detalles del peligro") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ThemeRedDanger),
                        modifier = Modifier.fillMaxWidth().testTag("report_desc_input")
                    )
                    Text("Tipo de Peligro:", color = Color.White, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "ROAD_CLOSED" to "Cierre",
                            "WEATHER_HAZARD" to "Clima",
                            "ACCIDENT" to "Choque"
                        ).forEach { (tCode, tLabel) ->
                            FilterChip(
                                selected = rType == tCode,
                                onClick = { rType = tCode },
                                label = { Text(tLabel) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rTitle.isNotBlank()) {
                            viewModel.reportSecurityAlert(rTitle, rMessage, rType)
                            showReportDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeRedDanger),
                    modifier = Modifier.testTag("submit_hazard_btn")
                ) {
                    Text("Enviar Reporte", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun BikerHistoryTab(viewModel: MotoConnectViewModel, biker: UserEntity) {
    val bookings by viewModel.allBookings.collectAsStateWithLifecycle()
    val users by viewModel.allUsers.collectAsStateWithLifecycle()

    val bikerBookings = bookings.filter { it.bikerId == biker.id }

    var bookingToRate by remember { mutableStateOf<BookingEntity?>(null) }
    var ratingStars by remember { mutableIntStateOf(5) }
    var ratingComment by remember { mutableStateOf("") }

    var bookingToDispute by remember { mutableStateOf<BookingEntity?>(null) }
    var disputeMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Mis Contrataciones y Viajes",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Historial, confirmación y estatus de tus contrataciones",
            color = Color.Gray,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (bikerBookings.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("Aún no has contratado servicios en la aplicación.", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(bikerBookings) { booking ->
                    val provider = users.find { it.id == booking.providerId }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("booking_card_${booking.id}"),
                        colors = CardDefaults.cardColors(containerColor = ThemeCardGray)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = booking.serviceName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                BookingStatusBadge(status = booking.status)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Proveedor: ${provider?.name ?: "Cargando..."}",
                                color = Color.LightGray,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Costo: $${booking.pricePaid} MXN",
                                color = ThemeOrangeAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Action buttons based on status
                            when (booking.status) {
                                "ACCEPTED" -> {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { viewModel.updateBookingStatus(booking.id, "COMPLETED") },
                                            colors = ButtonDefaults.buttonColors(containerColor = ThemeGreenSuccess),
                                            modifier = Modifier.weight(1f).testTag("complete_job_${booking.id}")
                                        ) {
                                            Text("Finalizar y Confirmar", color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = { bookingToDispute = booking },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ThemeRedDanger),
                                            border = BorderStroke(1.dp, ThemeRedDanger),
                                            modifier = Modifier.testTag("dispute_job_${booking.id}")
                                        ) {
                                            Text("Problema")
                                        }
                                    }
                                }
                                "COMPLETED" -> {
                                    if (booking.providerRating == null) {
                                        Button(
                                            onClick = { bookingToRate = booking },
                                            colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonBlue),
                                            modifier = Modifier.fillMaxWidth().testTag("rate_provider_btn_${booking.id}")
                                        ) {
                                            Icon(imageVector = Icons.Default.Star, contentDescription = "Estrella")
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Calificar Proveedor", color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(imageVector = Icons.Default.Star, contentDescription = "Calificado", tint = Color.Yellow, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Calificaste al proveedor: ${booking.providerRating} ★ \n\"${booking.providerComment ?: ""}\"",
                                                    fontSize = 11.sp,
                                                    color = Color.LightGray
                                                )
                                            }
                                        }
                                    }
                                }
                                "DISPUTED" -> {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = ThemeRedDanger.copy(alpha = 0.15f))
                                    ) {
                                        Text(
                                            text = "En Controversia: Esperando revisión del Administrador Master.",
                                            color = ThemeRedDanger,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(10.dp)
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

    // Modal de calificación
    if (bookingToRate != null) {
        val b = bookingToRate!!
        AlertDialog(
            onDismissRequest = { bookingToRate = null },
            title = { Text("Calificar Servicio y Proveedor", color = Color.White) },
            containerColor = ThemeCardGray,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("¿Cómo calificarías este servicio?", color = Color.LightGray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        (1..5).forEach { star ->
                            IconButton(onClick = { ratingStars = star }) {
                                Icon(
                                    imageVector = if (star <= ratingStars) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Estrella",
                                    tint = if (star <= ratingStars) Color.Yellow else Color.Gray,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = ratingComment,
                        onValueChange = { ratingComment = it },
                        label = { Text("Deja un comentario o reseña...") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ThemeOrangeAccent),
                        modifier = Modifier.fillMaxWidth().testTag("rate_comment_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rateBooking(b.id, ratingStars.toFloat(), ratingComment, isByBiker = true)
                        bookingToRate = null
                        ratingComment = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeOrangeAccent),
                    modifier = Modifier.testTag("submit_rating_btn")
                ) {
                    Text("Calificar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookingToRate = null }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }

    // Modal de controversia
    if (bookingToDispute != null) {
        val b = bookingToDispute!!
        AlertDialog(
            onDismissRequest = { bookingToDispute = null },
            title = { Text("Iniciar Controversia / Queja", color = Color.White) },
            containerColor = ThemeCardGray,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Describe el problema detalladamente. El administrador resolverá la controversia.", color = Color.LightGray)
                    OutlinedTextField(
                        value = disputeMessage,
                        onValueChange = { disputeMessage = it },
                        label = { Text("Motivo del problema...") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ThemeRedDanger),
                        modifier = Modifier.fillMaxWidth().testTag("dispute_message_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (disputeMessage.isNotBlank()) {
                            viewModel.reportDispute(b.id, disputeMessage)
                            bookingToDispute = null
                            disputeMessage = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeRedDanger),
                    modifier = Modifier.testTag("submit_dispute_btn")
                ) {
                    Text("Enviar Reporte", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookingToDispute = null }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun BookingStatusBadge(status: String) {
    val color = when (status) {
        "PENDING" -> Color.Yellow
        "ACCEPTED" -> ThemeNeonBlue
        "COMPLETED" -> ThemeGreenSuccess
        "CANCELLED" -> Color.Gray
        "DISPUTED" -> ThemeRedDanger
        "RESOLVED_FAVOR_BIKER" -> ThemeGreenSuccess
        "RESOLVED_FAVOR_PROVIDER" -> ThemeNeonBlue
        else -> Color.White
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = when (status) {
                "PENDING" -> "Pendiente"
                "ACCEPTED" -> "Aceptado"
                "COMPLETED" -> "Completado"
                "CANCELLED" -> "Cancelado"
                "DISPUTED" -> "Controversia"
                "RESOLVED_FAVOR_BIKER" -> "Resuelto Biker"
                "RESOLVED_FAVOR_PROVIDER" -> "Resuelto Prov"
                else -> status
            },
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}


// ==========================================
// ========== 2. PROVIDER SCREENS ==========
// ==========================================

@Composable
fun ProviderDashboardTab(viewModel: MotoConnectViewModel, provider: UserEntity) {
    val bookings by viewModel.allBookings.collectAsStateWithLifecycle()
    val users by viewModel.allUsers.collectAsStateWithLifecycle()

    val providerBookings = bookings.filter { it.providerId == provider.id }
    val activeJobs = providerBookings.filter { it.status == "PENDING" || it.status == "ACCEPTED" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "¡Hola, ${provider.name}!",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Dashboard de Proveedor • Calificación: %.1f ★".format(provider.rating),
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                IconButton(
                    onClick = { viewModel.logout() },
                    modifier = Modifier.background(Color.White.copy(alpha = 0.08f), CircleShape).testTag("logout_button")
                ) {
                    Icon(imageVector = Icons.Default.Logout, contentDescription = "Cerrar Sesión", tint = Color.White)
                }
            }
        }

        // Active Emergency / Job requests
        item {
            Text(
                text = "Solicitudes Activas (${activeJobs.size})",
                color = ThemeOrangeAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        if (activeJobs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ThemeCardGray)
                ) {
                    Box(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No tienes solicitudes pendientes o activas.", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(activeJobs) { booking ->
                val biker = users.find { it.id == booking.bikerId }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("provider_job_card_${booking.id}"),
                    colors = CardDefaults.cardColors(containerColor = ThemeCardGray),
                    border = BorderStroke(1.dp, if (booking.serviceId == null) ThemeRedDanger else Color.DarkGray)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (booking.serviceId == null) {
                                    Icon(imageVector = Icons.Default.Emergency, contentDescription = "SOS", tint = ThemeRedDanger, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = booking.serviceName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                            BookingStatusBadge(status = booking.status)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Cliente: ${biker?.name ?: "Biker"}",
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Moto: ${biker?.motorcycleModel ?: "No especificada"}",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Monto Total: $${booking.pricePaid} MXN",
                            color = ThemeGreenSuccess,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Comisión de plataforma aplicable: $%.2f MXN".format(booking.commissionPaid),
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Action controls
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (booking.status == "PENDING") {
                                Button(
                                    onClick = { viewModel.updateBookingStatus(booking.id, "ACCEPTED") },
                                    colors = ButtonDefaults.buttonColors(containerColor = ThemeOrangeAccent),
                                    modifier = Modifier.weight(1f).testTag("accept_job_${booking.id}")
                                ) {
                                    Text("Aceptar Servicio", color = Color.White)
                                }
                            } else if (booking.status == "ACCEPTED") {
                                Button(
                                    onClick = { viewModel.updateBookingStatus(booking.id, "COMPLETED") },
                                    colors = ButtonDefaults.buttonColors(containerColor = ThemeGreenSuccess),
                                    modifier = Modifier.weight(1f).testTag("complete_job_${booking.id}")
                                ) {
                                    Text("Marcar como Terminado", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProviderServicesTab(viewModel: MotoConnectViewModel, provider: UserEntity) {
    val services by viewModel.allServices.collectAsStateWithLifecycle()
    val providerServices = services.filter { it.providerId == provider.id }

    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Tus Servicios Ofertados",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Registra y edita los servicios que ofreces",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = ThemeOrangeAccent),
                modifier = Modifier.testTag("add_service_btn")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Añadir")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Añadir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (providerServices.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("Aún no tienes servicios registrados. Añade uno arriba.", color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(providerServices) { s ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ThemeCardGray)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = s.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(text = "$${s.price} MXN", color = ThemeOrangeAccent, fontWeight = FontWeight.Bold)
                            }
                            Text(text = "Categoría: ${s.category}", color = ThemeNeonBlue, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = s.description, color = Color.LightGray, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                TextButton(
                                    onClick = { viewModel.deleteService(s.id) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = ThemeRedDanger),
                                    modifier = Modifier.testTag("delete_service_btn_${s.id}")
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Eliminar", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var price by remember { mutableStateOf("") }
        var cat by remember { mutableStateOf("Mecánica") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Registrar Nuevo Servicio", color = Color.White) },
            containerColor = ThemeCardGray,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre del servicio") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ThemeOrangeAccent),
                        modifier = Modifier.fillMaxWidth().testTag("add_service_name_input")
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Descripción") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ThemeOrangeAccent),
                        modifier = Modifier.fillMaxWidth().testTag("add_service_desc_input")
                    )
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Precio base (MXN)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ThemeOrangeAccent),
                        modifier = Modifier.fillMaxWidth().testTag("add_service_price_input")
                    )
                    Text("Categoría:", color = Color.White, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Mecánica", "Grúas", "Repuestos").forEach { c ->
                            FilterChip(
                                selected = cat == c,
                                onClick = { cat = c },
                                label = { Text(c) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pDouble = price.toDoubleOrNull() ?: 0.0
                        if (name.isNotBlank() && pDouble > 0) {
                            viewModel.addNewService(name, desc, pDouble, cat)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeOrangeAccent),
                    modifier = Modifier.testTag("submit_service_btn")
                ) {
                    Text("Publicitar Servicio", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun ProviderHistoryTab(viewModel: MotoConnectViewModel, provider: UserEntity) {
    val bookings by viewModel.allBookings.collectAsStateWithLifecycle()
    val users by viewModel.allUsers.collectAsStateWithLifecycle()

    val providerHistory = bookings.filter { it.providerId == provider.id && it.status != "PENDING" && it.status != "ACCEPTED" }

    var bookingToRate by remember { mutableStateOf<BookingEntity?>(null) }
    var rStars by remember { mutableIntStateOf(5) }
    var rComment by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Historial y Evaluaciones",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Historial completo de asistencias cerradas o canceladas",
            color = Color.Gray,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (providerHistory.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("No tienes servicios completados o cerrados en tu historial.", color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(providerHistory) { booking ->
                    val biker = users.find { it.id == booking.bikerId }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ThemeCardGray)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = booking.serviceName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                BookingStatusBadge(status = booking.status)
                            }
                            Text(text = "Motociclista: ${biker?.name ?: "Biker"}", color = Color.LightGray, fontSize = 12.sp)
                            Text(text = "Cobro: $${booking.pricePaid} MXN", color = ThemeGreenSuccess, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            if (booking.status == "COMPLETED") {
                                if (booking.bikerRating == null) {
                                    Button(
                                        onClick = { bookingToRate = booking },
                                        colors = ButtonDefaults.buttonColors(containerColor = ThemeOrangeAccent),
                                        modifier = Modifier.fillMaxWidth().testTag("rate_biker_btn_${booking.id}")
                                    ) {
                                        Text("Calificar Motociclista", color = Color.White)
                                    }
                                } else {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = "Calificaste a este motociclista: ${booking.bikerRating} ★\n\"${booking.bikerComment ?: ""}\"",
                                            fontSize = 11.sp,
                                            color = Color.LightGray,
                                            modifier = Modifier.padding(8.dp)
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

    if (bookingToRate != null) {
        val b = bookingToRate!!
        AlertDialog(
            onDismissRequest = { bookingToRate = null },
            title = { Text("Calificar Motociclista", color = Color.White) },
            containerColor = ThemeCardGray,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("¿Cómo calificarías el comportamiento de este usuario?", color = Color.LightGray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        (1..5).forEach { star ->
                            IconButton(onClick = { rStars = star }) {
                                Icon(
                                    imageVector = if (star <= rStars) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Estrella",
                                    tint = if (star <= rStars) Color.Yellow else Color.Gray,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = rComment,
                        onValueChange = { rComment = it },
                        label = { Text("Mensaje o reseña de usuario...") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ThemeOrangeAccent),
                        modifier = Modifier.fillMaxWidth().testTag("rate_biker_comment_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rateBooking(b.id, rStars.toFloat(), rComment, isByBiker = false)
                        bookingToRate = null
                        rComment = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeOrangeAccent),
                    modifier = Modifier.testTag("submit_biker_rating")
                ) {
                    Text("Guardar Calificación", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookingToRate = null }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}


// ==========================================
// ========== 3. ADMIN SCREENS/TABS ==========
// ==========================================

@Composable
fun AdminParametersTab(viewModel: MotoConnectViewModel) {
    val appConfig by viewModel.appConfig.collectAsStateWithLifecycle()

    var commissionInput by remember { mutableStateOf("") }
    var adTextInput by remember { mutableStateOf("") }

    LaunchedEffect(appConfig) {
        commissionInput = appConfig.commissionPercentage.toString()
        adTextInput = appConfig.adText
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Panel de Parámetros Master",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Ajusta comisiones e inserta publicidad en tiempo real",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            IconButton(
                onClick = { viewModel.logout() },
                modifier = Modifier.background(Color.White.copy(alpha = 0.08f), CircleShape).testTag("logout_button")
            ) {
                Icon(imageVector = Icons.Default.Logout, contentDescription = "Cerrar Sesión", tint = Color.White)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ThemeCardGray)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "COMISIÓN DE LA PLATAFORMA",
                    color = ThemeNeonBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Porcentaje cobrado en cada transacción realizada entre motociclistas y proveedores.",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = commissionInput,
                        onValueChange = { commissionInput = it },
                        label = { Text("Porcentaje (%)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ThemeNeonBlue),
                        modifier = Modifier.weight(1f).testTag("commission_input_field")
                    )
                    Text(
                        text = "% actual: ${appConfig.commissionPercentage}%",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ThemeCardGray)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "PUBLICIDAD PATROCINADA EN TIEMPO REAL",
                    color = ThemeNeonBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Inserta un banner promocional que aparecerá al inicio de todos los usuarios de la app.",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = adTextInput,
                    onValueChange = { adTextInput = it },
                    label = { Text("Texto del anuncio publicitario") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ThemeNeonBlue),
                    modifier = Modifier.fillMaxWidth().testTag("ad_input_field"),
                    minLines = 3
                )
            }
        }

        Button(
            onClick = {
                val commValue = commissionInput.toDoubleOrNull() ?: 10.0
                viewModel.updatePlatformConfig(commValue, adTextInput)
            },
            colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonBlue),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_admin_params_btn")
        ) {
            Icon(imageVector = Icons.Default.Save, contentDescription = "Guardar", tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Guardar y Aplicar Cambios", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AdminDisputesTab(viewModel: MotoConnectViewModel) {
    val bookings by viewModel.allBookings.collectAsStateWithLifecycle()
    val users by viewModel.allUsers.collectAsStateWithLifecycle()

    val disputedBookings = bookings.filter { it.status == "DISPUTED" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Mesa de Controversias y Problemas",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Revisa y resuelve quejas de contrataciones conflictivas",
            color = Color.Gray,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (disputedBookings.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay controversias pendientes por resolver.", color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(disputedBookings) { booking ->
                    val biker = users.find { it.id == booking.bikerId }
                    val provider = users.find { it.id == booking.providerId }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dispute_admin_card_${booking.id}"),
                        colors = CardDefaults.cardColors(containerColor = ThemeCardGray),
                        border = BorderStroke(1.dp, ThemeRedDanger)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Servicio: ${booking.serviceName}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Biker: ${biker?.name} • Proveedor: ${provider?.name}",
                                color = ThemeNeonBlue,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Motivo reportado por el biker:", color = ThemeRedDanger, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    Text(
                                        text = "\"${booking.disputeMessage ?: "Sin mensaje"}\"",
                                        color = Color.LightGray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Monto en disputa: $${booking.pricePaid} MXN",
                                color = ThemeOrangeAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.resolveDispute(booking.id, resolveInFavorOfBiker = true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ThemeGreenSuccess),
                                    modifier = Modifier.weight(1f).testTag("resolve_favor_biker_${booking.id}")
                                ) {
                                    Text("Reembolsar Biker", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { viewModel.resolveDispute(booking.id, resolveInFavorOfBiker = false) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonBlue),
                                    modifier = Modifier.weight(1f).testTag("resolve_favor_provider_${booking.id}")
                                ) {
                                    Text("Liberar a Proveedor", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminUsersTab(viewModel: MotoConnectViewModel) {
    val users by viewModel.allUsers.collectAsStateWithLifecycle()

    var editingUser by remember { mutableStateOf<UserEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Directorio de Usuarios",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Controla, corrige datos y elimina perfiles en controversia",
            color = Color.Gray,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(users) { u ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ThemeCardGray)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = u.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(text = "${u.role} • ${u.email}", color = Color.Gray, fontSize = 12.sp)
                            Text(text = "Tel: ${u.phone}", color = Color.LightGray, fontSize = 11.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = { editingUser = u },
                                modifier = Modifier.background(Color.White.copy(alpha = 0.05f), CircleShape).testTag("edit_user_${u.id}")
                            ) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar", tint = ThemeNeonBlue, modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { viewModel.deleteUser(u.id) },
                                modifier = Modifier.background(Color.White.copy(alpha = 0.05f), CircleShape).testTag("delete_user_${u.id}")
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar", tint = ThemeRedDanger, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingUser != null) {
        val u = editingUser!!
        var eName by remember { mutableStateOf(u.name) }
        var eEmail by remember { mutableStateOf(u.email) }
        var ePhone by remember { mutableStateOf(u.phone) }
        var eMoto by remember { mutableStateOf(u.motorcycleModel ?: "") }

        AlertDialog(
            onDismissRequest = { editingUser = null },
            title = { Text("Corregir Datos de Usuario", color = Color.White) },
            containerColor = ThemeCardGray,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = eName,
                        onValueChange = { eName = it },
                        label = { Text("Nombre") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ThemeNeonBlue),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = eEmail,
                        onValueChange = { eEmail = it },
                        label = { Text("Email") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ThemeNeonBlue),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ePhone,
                        onValueChange = { ePhone = it },
                        label = { Text("Teléfono") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ThemeNeonBlue),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (u.role == "BIKER") {
                        OutlinedTextField(
                            value = eMoto,
                            onValueChange = { eMoto = it },
                            label = { Text("Modelo de Motocicleta") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ThemeNeonBlue),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateUserDetails(
                            u.copy(
                                name = eName,
                                email = eEmail,
                                phone = ePhone,
                                motorcycleModel = if (u.role == "BIKER") eMoto else u.motorcycleModel
                            )
                        )
                        editingUser = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonBlue),
                    modifier = Modifier.testTag("submit_edit_user_btn")
                ) {
                    Text("Guardar Cambios", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingUser = null }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}
