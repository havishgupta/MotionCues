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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefsManager = PreferencesManager(this)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Vehicle Motion Cues", fontWeight = FontWeight.Bold) },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MotionCuesScreen(
                            prefsManager = prefsManager,
                            onStartService = { startMotionService() },
                            onStopService = { stopMotionService() },
                            onRequestPermissions = { requestAllPermissions() },
                            checkPermissions = { hasRequiredPermissions() },
                            checkOverlay = { Settings.canDrawOverlays(this) }
                        )
                    }
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

    private fun startMotionService() {
        if (Settings.canDrawOverlays(this) && hasRequiredPermissions()) {
            val serviceIntent = Intent(this, MotionService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    startForegroundService(serviceIntent)
                    Toast.makeText(this, "Cues Started", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error starting service: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            } else {
                startService(serviceIntent)
                Toast.makeText(this, "Cues Started", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Please grant all permissions (including Location and Overlay) first", Toast.LENGTH_LONG).show()
            requestAllPermissions()
        }
    }

    private fun stopMotionService() {
        val serviceIntent = Intent(this, MotionService::class.java)
        stopService(serviceIntent)
        Toast.makeText(this, "Cues Stopped", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun MotionCuesScreen(
    prefsManager: PreferencesManager,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onRequestPermissions: () -> Unit,
    checkPermissions: () -> Boolean,
    checkOverlay: () -> Boolean
) {
    val scrollState = rememberScrollState()

    var dotSize by remember { mutableStateOf(prefsManager.dotSize) }
    var dotSpacing by remember { mutableStateOf(prefsManager.dotSpacing) }
    var dotOpacity by remember { mutableStateOf(prefsManager.dotOpacity) }
    var dotColor by remember { mutableStateOf(prefsManager.dotColor) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Setup & Controls", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onStartService, 
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Start Cues")
                    }
                    Button(
                        onClick = onStopService, 
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Stop Cues")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Appearance Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                // Color Picker
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
                
                // Dot Size
                Text("Dot Size: ${dotSize.toInt()}", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = dotSize,
                    onValueChange = { 
                        dotSize = it
                        prefsManager.dotSize = it
                    },
                    valueRange = 8f..30f
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Dot Spacing
                Text("Dot Spacing: ${dotSpacing.toInt()}", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = dotSpacing,
                    onValueChange = { 
                        dotSpacing = it
                        prefsManager.dotSpacing = it
                    },
                    valueRange = 40f..150f
                )

                Spacer(modifier = Modifier.height(8.dp))
                
                // Opacity
                Text("Opacity: ${(dotOpacity * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = dotOpacity,
                    onValueChange = { 
                        dotOpacity = it
                        prefsManager.dotOpacity = it
                    },
                    valueRange = 0.1f..1f
                )
            }
        }
    }
}
