package com.example.motioncues

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.isNotEmpty()) {
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Some permissions were denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefsManager = PreferencesManager(this)

        val SkyBlue = Color(0xFF87CEFA)
        val LightSkyBlue = Color(0xFFE1F5FE)
        val Yellowish = Color(0xFFFFF59D)
        
        val customTheme = lightColorScheme(
            primary = Color(0xFF0288D1),
            secondary = Yellowish,
            background = LightSkyBlue,
            surface = Color.White,
            onBackground = Color(0xFF003B5C),
            onSurface = Color(0xFF003B5C)
        )

        setContent {
            MaterialTheme(colorScheme = customTheme) {
                var hasSeenIntro by remember { mutableStateOf(prefsManager.hasSeenIntro) }
                var showSettings by remember { mutableStateOf(false) }
                
                if (!hasSeenIntro) {
                    IntroScreen(onFinish = {
                        prefsManager.hasSeenIntro = true
                        hasSeenIntro = true
                    })
                } else if (showSettings) {
                    SettingsScreen(
                        prefsManager = prefsManager,
                        onBack = { showSettings = false },
                        onRequestPermissions = { requestAllPermissions() },
                        checkPermissions = { hasRequiredPermissions() },
                        checkOverlay = { Settings.canDrawOverlays(this) }
                    )
                } else {
                    MainScreen(
                        onOpenSettings = { showSettings = true },
                        onToggleService = { toggleMotionService() }
                    )
                }
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun requestAllPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && 
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            Toast.makeText(this, "Please enable 'Display over other apps'", Toast.LENGTH_LONG).show()
        } else if (permissionsToRequest.isEmpty()) {
            Toast.makeText(this, "All permissions already granted!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleMotionService() {
        if (MotionService.isRunning) {
            stopService(Intent(this, MotionService::class.java))
        } else {
            if (Settings.canDrawOverlays(this) && hasRequiredPermissions()) {
                val serviceIntent = Intent(this, MotionService::class.java)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to start: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(this, "Please grant Overlay & Notification permissions in Settings first", Toast.LENGTH_LONG).show()
                requestAllPermissions()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IntroScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> Slide1()
                    1 -> Slide2()
                    2 -> Slide3()
                    3 -> Slide4(onFinish)
                }
            }

            // Pager indicator
            Row(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(4) { iteration ->
                    val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else Color.LightGray
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(10.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            if (pagerState.currentPage < 3) {
                TextButton(onClick = { 
                    coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }) {
                    Text("NEXT", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Spacer(modifier = Modifier.height(48.dp)) // Reserve space to prevent jitter
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun Slide1() {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(1000)) + scaleIn(initialScale = 0.8f, animationSpec = tween(1000))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "MOTION",
                fontSize = 54.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp
            )
            Text(
                text = "CUES",
                fontSize = 48.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 8.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "Welcome to a smoother journey.",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun Slide2() {
    val infiniteTransition = rememberInfiniteTransition(label = "eye_ear")
    val eyeOffset by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eye"
    )
    val earOffset by infiniteTransition.animateFloat(
        initialValue = 15f,
        targetValue = -15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing), // Out of sync timing
            repeatMode = RepeatMode.Reverse
        ),
        label = "ear"
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Visibility,
                contentDescription = "Eyes",
                modifier = Modifier.size(80.dp).offset(y = eyeOffset.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = "Mismatch",
                modifier = Modifier.size(50.dp),
                tint = Color(0xFFFBC02D) // Yellowish
            )
            Icon(
                imageVector = Icons.Outlined.Hearing, // Hearing icon
                contentDescription = "Ears",
                modifier = Modifier.size(80.dp).offset(y = earOffset.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(64.dp))
        
        Text(
            text = "The Problem",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Motion sickness occurs when what your eyes see doesn't match what your inner ear feels.",
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 24.sp
        )
    }
}

@Composable
fun Slide3() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dotOffset by infiniteTransition.animateFloat(
        initialValue = -60f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot"
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(width = 240.dp, height = 160.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(4) {
                    Row(
                        modifier = Modifier.fillMaxWidth().offset(x = dotOffset.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        repeat(5) {
                            Box(modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(64.dp))
        
        Text(
            text = "The Solution",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Animated dots sync with your vehicle's motion to naturally realign your senses.",
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 24.sp
        )
    }
}

@Composable
fun Slide4(onFinish: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Ready",
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "How to Start",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Text("Via this App interface", fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SwipeDown, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Text("Via the Quick Settings tile in your notification panel", fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground, lineHeight = 24.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(64.dp))
        
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("LET'S GO", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onOpenSettings: () -> Unit,
    onToggleService: () -> Unit
) {
    var isServiceRunning by remember { mutableStateOf(MotionService.isRunning) }
    LaunchedEffect(Unit) {
        while(true) {
            isServiceRunning = MotionService.isRunning
            kotlinx.coroutines.delay(500)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    IconButton(onClick = onOpenSettings, modifier = Modifier.padding(8.dp)) {
                        Icon(
                            Icons.Default.Settings, 
                            contentDescription = "Settings", 
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "MOTION",
                fontSize = 54.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp
            )
            Text(
                text = "CUES",
                fontSize = 48.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 8.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isServiceRunning) Color(0xFFC8E6C9) else Color(0xFFFFCCBC)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (isServiceRunning) "OVERLAY ACTIVE" else "OVERLAY INACTIVE",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isServiceRunning) Color(0xFF2E7D32) else Color(0xFFD84315),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(80.dp))

            // Beautiful Giant Button
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(CircleShape)
                    .background(if (isServiceRunning) Color(0xFFFF5252) else MaterialTheme.colorScheme.secondary)
                    .border(
                        width = 8.dp, 
                        color = if (isServiceRunning) Color(0xFFD32F2F) else Color(0xFFFBC02D), 
                        shape = CircleShape
                    )
                    .clickable { onToggleService() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isServiceRunning) "STOP" else "START",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isServiceRunning) Color.White else Color(0xFF003B5C),
                    letterSpacing = 4.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefsManager: PreferencesManager,
    onBack: () -> Unit,
    onRequestPermissions: () -> Unit,
    checkPermissions: () -> Boolean,
    checkOverlay: () -> Boolean
) {
    val scrollState = rememberScrollState()

    var dotSize by remember { mutableStateOf(prefsManager.dotSize) }
    var dotSpacing by remember { mutableStateOf(prefsManager.dotSpacing) }
    var dotOpacity by remember { mutableStateOf(prefsManager.dotOpacity) }
    var dotColor by remember { mutableStateOf(prefsManager.dotColor) }
    var tiltSensitivity by remember { mutableStateOf(prefsManager.tiltSensitivity) }
    var speedMultiplier by remember { mutableStateOf(prefsManager.speedMultiplier) }

    val presetColors = listOf(
        android.graphics.Color.BLACK,
        android.graphics.Color.DKGRAY,
        android.graphics.Color.parseColor("#1976D2"), // Blue
        android.graphics.Color.parseColor("#388E3C"), // Green
        android.graphics.Color.parseColor("#D32F2F")  // Red
    )

    var notifyGranted by remember { mutableStateOf(checkPermissions()) }
    var overlayGranted by remember { mutableStateOf(checkOverlay()) }
    
    LaunchedEffect(Unit) {
        while(true) {
            notifyGranted = checkPermissions()
            overlayGranted = checkOverlay()
            kotlinx.coroutines.delay(1000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("System Permissions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(if (notifyGranted) Color.Green else Color.Red))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(if (notifyGranted) "Notifications: Granted" else "Notifications: Missing", fontWeight = FontWeight.Medium)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(if (overlayGranted) Color.Green else Color.Red))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(if (overlayGranted) "Screen Overlay: Granted" else "Screen Overlay: Missing", fontWeight = FontWeight.Medium)
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Button(onClick = onRequestPermissions, modifier = Modifier.fillMaxWidth()) {
                        Text("Manage Permissions")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Visual Style", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Dot Color", style = MaterialTheme.typography.bodyMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        presetColors.forEach { colorInt ->
                            val isSelected = dotColor == colorInt
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorInt))
                                    .border(
                                        width = if (isSelected) 4.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        dotColor = colorInt
                                        prefsManager.dotColor = colorInt
                                    }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Dot Base Size: ${dotSize.toInt()}", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = dotSize,
                        onValueChange = { dotSize = it; prefsManager.dotSize = it },
                        valueRange = 8f..40f
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("Dot Spacing: ${dotSpacing.toInt()}", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = dotSpacing,
                        onValueChange = { dotSpacing = it; prefsManager.dotSpacing = it },
                        valueRange = 40f..250f
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("Opacity: ${(dotOpacity * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = dotOpacity,
                        onValueChange = { dotOpacity = it; prefsManager.dotOpacity = it },
                        valueRange = 0.1f..1f
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Physics & Tuning", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Sensitivity (Distance Travelled): ${String.format("%.1fx", tiltSensitivity)}", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = tiltSensitivity,
                        onValueChange = { tiltSensitivity = it; prefsManager.tiltSensitivity = it },
                        valueRange = 0.1f..4.0f
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("Smoothness (Response Speed): ${String.format("%.1fx", speedMultiplier)}", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = speedMultiplier,
                        onValueChange = { speedMultiplier = it; prefsManager.speedMultiplier = it },
                        valueRange = 0.1f..3.0f
                    )
                }
            }
        }
    }
}
