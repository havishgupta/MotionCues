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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

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

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                var showSettings by remember { mutableStateOf(false) }
                
                if (showSettings) {
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
        var hasPerms = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                       ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                       
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPerms = hasPerms && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }
        return hasPerms
    }

    private fun requestAllPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
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
                Toast.makeText(this, "Please grant all permissions in Settings first", Toast.LENGTH_LONG).show()
                requestAllPermissions()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onOpenSettings: () -> Unit,
    onToggleService: () -> Unit
) {
    // Poll the service state
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
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(32.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Motion Cues",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (isServiceRunning) "Active" else "Inactive",
                fontSize = 20.sp,
                color = if (isServiceRunning) Color.Green else Color.Gray
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Big Start/Stop Button
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(if (isServiceRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    .clickable { onToggleService() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isServiceRunning) "STOP" else "START",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
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

    var locationGranted by remember { mutableStateOf(checkPermissions()) }
    var overlayGranted by remember { mutableStateOf(checkOverlay()) }
    
    LaunchedEffect(Unit) {
        while(true) {
            locationGranted = checkPermissions()
            overlayGranted = checkOverlay()
            kotlinx.coroutines.delay(1000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Permissions Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Permissions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(if (locationGranted) Color.Green else Color.Red))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (locationGranted) "Location: Granted" else "Location: Missing", fontWeight = FontWeight.Medium)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(if (overlayGranted) Color.Green else Color.Red))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (overlayGranted) "Overlay: Granted" else "Overlay: Missing", fontWeight = FontWeight.Medium)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(onClick = onRequestPermissions, modifier = Modifier.fillMaxWidth()) {
                        Text("Grant Required Permissions")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Appearance Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Dot Color", style = MaterialTheme.typography.bodyMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        presetColors.forEach { colorInt ->
                            val isSelected = dotColor == colorInt
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorInt))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
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
                    
                    Text("Dot Size: ${dotSize.toInt()}", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = dotSize,
                        onValueChange = { dotSize = it; prefsManager.dotSize = it },
                        valueRange = 8f..30f
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("Dot Spacing: ${dotSpacing.toInt()}", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = dotSpacing,
                        onValueChange = { dotSpacing = it; prefsManager.dotSpacing = it },
                        valueRange = 40f..150f
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
            
            // Physics Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Physics Tuning", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Tilt Sensitivity: ${String.format("%.1fx", tiltSensitivity)}", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = tiltSensitivity,
                        onValueChange = { tiltSensitivity = it; prefsManager.tiltSensitivity = it },
                        valueRange = 0.1f..3.0f
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("Speed Multiplier: ${String.format("%.1fx", speedMultiplier)}", style = MaterialTheme.typography.bodyMedium)
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
