package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.VPhone
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(
    viewModel: VPhoneViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val allPhones by viewModel.allPhones.collectAsState()
    val isDeploying by viewModel.isDeploying.collectAsState()
    val selectedPhone by viewModel.selectedPhone.collectAsState()

    var showDeployDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Set Window KeepScreenOn flag reactively based on localWakelockActive
    val keepScreenOn by viewModel.localWakelockActive.collectAsState()
    val dimmerActive by viewModel.dimScreenActive.collectAsState()

    Scaffold(
        bottomBar = {
            if (!dimmerActive) {
                NavigationBar(
                    containerColor = DarkSurface,
                    modifier = Modifier.navigationBarsPadding(),
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentScreen == "dashboard",
                        onClick = { viewModel.navigateTo("dashboard") },
                        label = { Text("vPhones", color = Color.White) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "vPhones",
                                tint = if (currentScreen == "dashboard") NeonCyan else Color.White.copy(alpha = 0.6f)
                            )
                        }
                    )
                    NavigationBarItem(
                        selected = currentScreen == "local_farm",
                        onClick = { viewModel.navigateTo("local_farm") },
                        label = { Text("Local AFK", color = Color.White) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Local AFK",
                                tint = if (currentScreen == "local_farm") NeonCyan else Color.White.copy(alpha = 0.6f)
                            )
                        }
                    )
                    NavigationBarItem(
                        selected = currentScreen == "macros",
                        onClick = { viewModel.navigateTo("macros") },
                        label = { Text("Macros", color = Color.White) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = "Scripts",
                                tint = if (currentScreen == "macros") NeonCyan else Color.White.copy(alpha = 0.6f)
                            )
                        }
                    )
                    NavigationBarItem(
                        selected = currentScreen == "assistant",
                        onClick = { viewModel.navigateTo("assistant") },
                        label = { Text("AIdroid", color = Color.White) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "AI Assistant",
                                tint = if (currentScreen == "assistant") NeonCyan else Color.White.copy(alpha = 0.6f)
                            )
                        }
                    )
                }
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen switching with slide animations
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    "dashboard" -> DashboardScreen(
                        viewModel = viewModel,
                        allPhones = allPhones,
                        onDeployClick = { showDeployDialog = true },
                        selectedPhone = selectedPhone
                    )
                    "local_farm" -> LocalFarmScreen(viewModel = viewModel)
                    "macros" -> MacrosScreen(viewModel = viewModel)
                    "assistant" -> AssistantScreen(viewModel = viewModel)
                }
            }

            // Showing simulated deployment loader overlay
            if (isDeploying) {
                DeploymentLoaderOverlay()
            }

            // Real battery saving dimmer overlay
            if (dimmerActive) {
                LocalDimmerOverlay(
                    onDismiss = { viewModel.dimScreenActive.value = false }
                )
            }
        }
    }

    // Deploy dialog
    if (showDeployDialog) {
        DeployPhoneDialog(
            onDismiss = { showDeployDialog = false },
            onDeploy = { name, gamePackage, days, farmMode, region, fps ->
                viewModel.deployVPhone(name, gamePackage, days, farmMode, region, fps)
                showDeployDialog = false
            }
        )
    }
}

@Composable
fun DashboardScreen(
    viewModel: VPhoneViewModel,
    allPhones: List<VPhone>,
    onDeployClick: () -> Unit,
    selectedPhone: VPhone?
) {
    if (selectedPhone != null) {
        // Detailed Cloud screen streaming terminal
        PhoneDetailView(phone = selectedPhone, viewModel = viewModel)
    } else {
        // Default list of Cloud Phones
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DashboardHeader(onDeployClick = onDeployClick)
            }

            if (allPhones.isEmpty()) {
                item {
                    EmptyStatusSection(onDeployClick = onDeployClick)
                }
            } else {
                items(allPhones, key = { it.id }) { phone ->
                    VPhoneCard(phone = phone, onClick = { viewModel.selectPhone(phone) })
                }
            }
        }
    }
}

@Composable
fun DashboardHeader(onDeployClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "V-FINGER SPACE",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                ),
                color = NeonCyan
            )
            Text(
                text = "Server Virtual Cloud Gratis Anda",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
        }

        Button(
            onClick = onDeployClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricPurple,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("deploy_cloud_phone_button")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Deploy", tint = Color.White)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Sewa VPhone", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun EmptyStatusSection(onDeployClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 80.dp)
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📡",
            fontSize = 48.sp,
            color = NeonCyan
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Belum Ada vPhone Aktif",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Sewa Cloud Android Phone secara gratis sekarang! Sediakan bot 24/7 untuk memainkan game AFK kesayangan Anda tanpa menguras baterai real HP Anda.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onDeployClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonCyan,
                contentColor = ObsidianDark
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Mulai Hubungkan vPhone", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun VPhoneCard(phone: VPhone, onClick: () -> Unit) {
    val durationPercent = (phone.hoursElapsed / (phone.afkDaysRequested * 24.0)).coerceIn(0.0, 1.0)
    val percentageFormatted = DecimalFormat("0.0%").format(durationPercent)
    val hoursRemaining = ((phone.afkDaysRequested * 24.0) - phone.hoursElapsed).coerceAtLeast(0.0)
    val daysRemaining = DecimalFormat("0.0").format(hoursRemaining / 24.0)

    val gameLabel = when (phone.gamePackage) {
        "com.gravity.romg" -> "Ragnarok M"
        "com.lgame.roorigin" -> "Ragnarok Origin"
        "com.mobile.legends" -> "Mobile Legends"
        "com.miHoYo.GenshinImpact" -> "Genshin Impact"
        "com.pearlabyss.blackdesertm" -> "Black Desert M"
        else -> "Custom Game Bot"
    }

    val statusColor = when (phone.status) {
        "ONLINE" -> OnlineGreen
        "PAUSED" -> AmberPending
        else -> CrimsonAlert
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(
                1.dp,
                if (phone.status == "ONLINE") NeonCyan.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .testTag("phone_card_${phone.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = phone.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                Text(
                    text = phone.status,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Game Terpasang",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Text(
                        text = gameLabel,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = NeonCyan
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Farming Taktik",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Text(
                        text = phone.farmMode,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = ElectricPurple
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Grinding stats row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🪙 GOLD", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                    Text("+${phone.goldEarned}", fontWeight = FontWeight.Bold, color = NeonCyan)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🌟 EXP", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                    Text("+${phone.xpEarned}", fontWeight = FontWeight.Bold, color = ElectricPurple)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎁 JARAHAN", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                    Text("${phone.lootCount} item", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Bar of Rent Hours
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Durasi AFK: ${phone.hoursElapsed.toInt()}j / ${phone.afkDaysRequested * 24}j (${percentageFormatted})",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )

                Text(
                    text = "Sisa $daysRemaining Hari",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { durationPercent.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = NeonCyan,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
fun PhoneDetailView(phone: VPhone, viewModel: VPhoneViewModel) {
    val phoneLogs by viewModel.phoneLogs.collectAsState()
    val logs = phoneLogs[phone.id] ?: listOf("[SYSTEM] Meluncurkan dekripsi stream remote video...")
    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var showExtendModal by remember { mutableStateOf(false) }

    // Auto scroll logs to bottom
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            scope.launch {
                scrollState.animateScrollToItem(logs.size - 1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Back toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { viewModel.selectPhone(null) },
                modifier = Modifier.background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            ) {
                Text(
                    text = "←",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Text(
                text = phone.name.uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = NeonCyan
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (phone.status == "ONLINE") OnlineGreen else CrimsonAlert)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = phone.status,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (phone.status == "ONLINE") OnlineGreen else CrimsonAlert
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large simulated streaming terminal display resembling a smartphone frame!
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(2.dp, NeonCyan, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(ObsidianDark)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Video header simulation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VPhone IP: ${phone.ipAddress} [${phone.region}]",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${phone.fpsPreset} FPS (ECO STRM)",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = OnlineGreen,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Inner Console simulation - Render some graphical layout of a robot/game or logs
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(12.dp)
                ) {
                    // Spinning cyber lines
                    val infiniteTransition = rememberInfiniteTransition(label = "Radar")
                    val angle by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(8000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "Rotation"
                    )

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        val radius = size.minDimension / 3

                        // Draw Grid lines
                        for (i in 1..8) {
                            val x = size.width * (i / 9f)
                            drawLine(
                                color = NeonCyan.copy(alpha = 0.08f),
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Orbit circles
                        drawCircle(
                            color = ElectricPurple.copy(alpha = 0.15f),
                            radius = radius,
                            center = Offset(centerX, centerY),
                            style = Stroke(width = 2.dp.toPx())
                        )
                        drawCircle(
                            color = NeonCyan.copy(alpha = 0.25f),
                            radius = radius * 0.6f,
                            center = Offset(centerX, centerY),
                            style = Stroke(width = 1.dp.toPx())
                        )

                        // Center node
                        drawCircle(
                            color = NeonCyan,
                            radius = 8.dp.toPx(),
                            center = Offset(centerX, centerY)
                        )
                    }

                    // Terminal Live Logging Stream Overlay
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Loot Tambahan: +${phone.lootCount}", fontSize = 11.sp, color = Color.White)
                                Text("XP Terkumpul: +${phone.xpEarned}", fontSize = 11.sp, color = NeonCyan)
                                Text("Gold: +${phone.goldEarned}", fontSize = 11.sp, color = Color.Yellow)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // scrolling list of terminal commands
                        LazyColumn(
                            state = scrollState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(logs) { log ->
                                Text(
                                    text = log,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (log.contains("[SYSTEM]")) NeonCyan else if (log.contains("JARAH")) Color.Yellow else Color.White,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom Controls for virtual phone
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.togglePhoneStatus(phone) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (phone.status == "ONLINE") Color(0xFFF57C00) else OnlineGreen,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (phone.status == "ONLINE") {
                    Row(
                        modifier = Modifier.size(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.width(4.dp).fillMaxHeight().clip(RoundedCornerShape(1.dp)).background(Color.White))
                        Box(modifier = Modifier.width(4.dp).fillMaxHeight().clip(RoundedCornerShape(1.dp)).background(Color.White))
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Toggle Status",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (phone.status == "ONLINE") "Hentikan AFK" else "Lanjutkan AFK",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = { showExtendModal = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = ObsidianDark
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Tambah Hari AFK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { viewModel.deleteVPhone(phone) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CrimsonAlert,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("delete_phone_button")
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
            }
        }
    }

    // Modal to request free AFK days extension
    if (showExtendModal) {
        RequestExtendDialog(
            phone = phone,
            onDismiss = { showExtendModal = false },
            onConfirm = { additionalDays ->
                viewModel.requestExtendDays(phone, additionalDays)
                showExtendModal = false
            }
        )
    }
}

@Composable
fun LocalFarmScreen(viewModel: VPhoneViewModel) {
    var keepScreenAwake by remember { mutableStateOf(false) }

    // Use DisposableEffect to handle KeepScreenOn on MainActivity window
    val context = LocalContext.current
    DisposableEffect(keepScreenAwake) {
        val window = (context as? android.app.Activity)?.window
        if (window != null) {
            if (keepScreenAwake) {
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "LOCAL AFK GRIND UTILITIES",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = NeonCyan
                )
                Text(
                    text = "Gunakan peralatan ini jika ingin menjalankan bot game langsung di HP fisik Anda ini",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔒 Pengaman Layar Wakelock",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Aktifkan saklar di bawah ini untuk mengunci HP Anda agar tidak pernah terkunci otomatis/sleep. Berguna saat meninggalkan game tetap terbuka semalaman.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (keepScreenAwake) "Status: SELALU NYALA (AKTIF)" else "Status: TIDAK AKTIF",
                            fontWeight = FontWeight.SemiBold,
                            color = if (keepScreenAwake) OnlineGreen else OffliningGrey
                        )

                        Switch(
                            checked = keepScreenAwake,
                            onCheckedChange = { keepScreenAwake = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ObsidianDark,
                                checkedTrackColor = OnlineGreen
                            )
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔌 Mode Dimmer Baterai Super Hemat",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Melapisi layar Anda dengan warna hitam pekat 99% transparan. Mengurangi konsumsi daya layar (AMOLED/OLED) secara radikal saat bertempur otomatis semalaman, dan melindungi dari burn-in layar fisik.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.dimScreenActive.value = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Aktifkan Black Dimmer Overlay", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.15f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📱 Panduan Auto-Tapper Pendamping",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sewa VPhone di V-Finger berjalan di server cloud kami 100% tanpa menyedot kuota/CPU Anda harian. Namun jika Anda menggunakan utilitas Lokal AFK untuk game fisik yang tidak ramah emulator cloud, pasangkan dengan aplikasi Tambahan 'Auto Clicker - Automatic Tap' dari Google Play Store dan set rute makro skrip dari menu Asisten V-Finger!",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MacrosScreen(viewModel: VPhoneViewModel) {
    val macros by viewModel.customMacros.collectAsState()
    var showAddMacroDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MACRO SCRIPT HUB",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        ),
                        color = NeonCyan
                    )
                    Text(
                        text = "Katalog urutan tap taktik untuk game Anda",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                Button(
                    onClick = { showAddMacroDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ObsidianDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Tambah", fontWeight = FontWeight.Bold)
                }
            }
        }

        items(macros) { macro ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = macro.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Target: ${macro.gameTarget}",
                                fontSize = 11.sp,
                                color = NeonCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        IconButton(
                            onClick = { viewModel.deleteMacro(macro) }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Saran Hapus", tint = CrimsonAlert)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = macro.description,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "URUTAN LANGKAH (TAP INSTAN):",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    macro.instructions.forEachIndexed { idx, ins ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${idx + 1}.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricPurple,
                                modifier = Modifier.width(20.dp)
                            )
                            Text(
                                text = ins,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddMacroDialog) {
        AddMacroDialog(
            onDismiss = { showAddMacroDialog = false },
            onConfirm = { name, desc, game, steps ->
                viewModel.addMacroScript(name, desc, game, steps)
                showAddMacroDialog = false
            }
        )
    }
}

@Composable
fun AssistantScreen(viewModel: VPhoneViewModel) {
    val isGmLoading by viewModel.isGeminiLoading.collectAsState()
    val gemResponse by viewModel.geminiResponse.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "AIdroid AFK ASSISTANT",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = NeonCyan
                )
                Text(
                    text = "Tanyakan taktik penempatan koordinat & skrip bypass anti-cheat game menggunakan AI cerdas Gemini",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tulis Game & Goal Target Anda:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = NeonCyan,
                            unfocusedContainerColor = DarkBackground,
                            focusedContainerColor = DarkBackground
                        ),
                        placeholder = {
                            Text(
                                "Contoh: Ragnarok M farming Wild Wolf semalaman",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.4f)
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                viewModel.askGeminiForGuide(searchQuery)
                            }
                        },
                        enabled = searchQuery.isNotBlank() && !isGmLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = ObsidianDark,
                            disabledContainerColor = NeonCyan.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isGmLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ObsidianDark, strokeWidth = 2.dp)
                        } else {
                            Text("Tanyakan Taktik AI", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, ElectricPurple.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🧠 RESPONS STRATEGI AI:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricPurple
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (gemResponse != null) {
                        Text(
                            text = gemResponse!!,
                            fontSize = 13.sp,
                            color = Color.White,
                            lineHeight = 20.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    } else {
                        Text(
                            text = "Silakan masukkan tujuan farming game Anda di atas dan klik tombol tanya untuk merumuskan instruksi bot eksklusif.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp)
                        )
                    }
                }
            }
        }
    }
}

// Fullscreen Black Dimmer Overlay for AMOLED panels
@Composable
fun LocalDimmerOverlay(onDismiss: () -> Unit) {
    var swipeState by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(enabled = false) {} // block clickthrough
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "V-FINGER BATTERY SAVER ACTIVE",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Layar fisik sedang dimatikan/pekat. CPU Anda tetap memproses game latar depan dengan kunci layar menyala secara stabil harian.",
                    fontSize = 11.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center
                )
            }

            // Swipe to unlock visual drag
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text("Klik 2x Untuk Batalkan Dimmer", fontSize = 12.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun DeploymentLoaderOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            CircularProgressIndicator(color = NeonCyan)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "ALOKASI SERVER VIRTUAL...",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = NeonCyan,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Mendaftarkan Cloud Node baru di pusat server Singapore. Meluncurkan visual streaming emulator. Mohon tunggu sebentar...",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}

// Deploy VPhone dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeployPhoneDialog(
    onDismiss: () -> Unit,
    onDeploy: (String, String, Int, String, String, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var gamePackage by remember { mutableStateOf("com.gravity.romg") }
    var afkDays by remember { mutableFloatStateOf(3f) }
    var farmMode by remember { mutableStateOf("GRINDING") }
    var region by remember { mutableStateOf("Singapore") }
    var fpsPreset by remember { mutableIntStateOf(60) }

    val gamesList = listOf(
        "com.gravity.romg" to "Ragnarok M (Auto Farming)",
        "com.lgame.roorigin" to "Ragnarok Origin (Daily Quests)",
        "com.mobile.legends" to "Mobile Legends (AI Push Lanes)",
        "com.miHoYo.GenshinImpact" to "Genshin Impact (Collect Ore)",
        "com.pearlabyss.blackdesertm" to "Black Desert M (Camp Farming)",
        "custom" to "Nama Skrip Kustom (Ubah manual)"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxHeight(0.85f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "SEWA VPHONE (GRATIS)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Instalasi virtual device baru di datacenter aman.",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Cloud Virtual Device", fontSize = 11.sp) },
                        placeholder = { Text("Contoh: ROM-vPhone-01") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            focusedLabelColor = NeonCyan
                        ),
                        singleLine = true
                    )
                }

                item {
                    Text("Pilih Target Game / Preset Scrip:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Column {
                        gamesList.forEach { (pkg, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { gamePackage = pkg }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = gamePackage == pkg,
                                    onClick = { gamePackage = pkg },
                                    colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(label, fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Durasi Sewa: ${afkDays.toInt()} Hari", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("GRATIS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OnlineGreen)
                    }

                    Slider(
                        value = afkDays,
                        onValueChange = { afkDays = it },
                        valueRange = 1f..30f,
                        steps = 29,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                    Text("Mintalah durasi 1 hingga 30 hari sesuai kebutuhan tanpa koin/premium.", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Region Server:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row {
                                listOf("Singapore", "Silicon Valley").forEach { r ->
                                    FilterChip(
                                        selected = region == r,
                                        onClick = { region = r },
                                        label = { Text(r, fontSize = 11.sp) },
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                            }
                        }

                        Column {
                            Text("Target FPS:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row {
                                listOf(30, 60).forEach { f ->
                                    FilterChip(
                                        selected = fpsPreset == f,
                                        onClick = { fpsPreset = f },
                                        label = { Text("${f}fps", fontSize = 11.sp) },
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Text("Taktik Mode:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("GRINDING", "QUESTS", "RAIDS").forEach { m ->
                            FilterChip(
                                selected = farmMode == m,
                                onClick = { farmMode = m },
                                label = { Text(m, fontSize = 10.sp) },
                                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Batalkan", color = Color.White.copy(alpha = 0.6f))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onDeploy(name, gamePackage, afkDays.toInt(), farmMode, region, fpsPreset) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ObsidianDark)
                        ) {
                            Text("Mulai Sewa", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Modal dialog to extend Cloud Phone AFK days for free
@Composable
fun RequestExtendDialog(
    phone: VPhone,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var additionalDays by remember { mutableFloatStateOf(5f) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "MINTA TAMBAH HARI AFK",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = NeonCyan
                )

                Text(
                    text = "Tambahkan rentang sewa virtual secara gratis untuk ${phone.name}. Tidak dipungut biaya apa pun.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Column {
                    Text(
                        text = "Tambah: +${additionalDays.toInt()} Hari",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )

                    Slider(
                        value = additionalDays,
                        onValueChange = { additionalDays = it },
                        valueRange = 1f..15f,
                        steps = 14,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = Color.White.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(additionalDays.toInt()) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ObsidianDark)
                    ) {
                        Text("Konfirmasi", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Dialog to add custom macro scripts
@Composable
fun AddMacroDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var game by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "REKAM MAKRO BARU",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = NeonCyan
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Makro", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, focusedLabelColor = NeonCyan)
                )

                OutlinedTextField(
                    value = game,
                    onValueChange = { game = it },
                    label = { Text("Target Game", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, focusedLabelColor = NeonCyan)
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Deskripsi Singkat", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, focusedLabelColor = NeonCyan)
                )

                OutlinedTextField(
                    value = steps,
                    onValueChange = { steps = it },
                    label = { Text("Langkah Per-Baris", fontSize = 11.sp) },
                    placeholder = { Text("Contoh:\nTap(1024, 720)\nWait(3s)\nTap(200, 150)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, focusedLabelColor = NeonCyan)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = Color.White.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(name, desc, game, steps) },
                        enabled = name.isNotBlank() && game.isNotBlank() && steps.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ObsidianDark)
                    ) {
                        Text("Simpan Makro", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
