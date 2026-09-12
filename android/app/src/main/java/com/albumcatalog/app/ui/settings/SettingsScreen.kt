package com.albumcatalog.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.albumcatalog.app.VinylCatalogApp
import com.albumcatalog.app.data.repository.VinylRepository
import com.albumcatalog.app.ui.theme.TextPrimary
import com.albumcatalog.app.ui.theme.TextSecondary
import com.albumcatalog.app.ui.theme.VinylBlack
import com.albumcatalog.app.ui.theme.VinylGold
import com.albumcatalog.app.ui.theme.VinylGreen
import com.albumcatalog.app.ui.theme.VinylRed
import com.albumcatalog.app.util.FeedbackManager
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    repository: VinylRepository = remember { VinylRepository() }
) {
    val coroutineScope = rememberCoroutineScope()
    var urlInput by remember { mutableStateOf(VinylCatalogApp.instance.getBaseUrl()) }
    var isChecking by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf<Boolean?>(null) }
    var statusMessage by remember { mutableStateOf("") }

    var isHapticsEnabled by remember { mutableStateOf(VinylCatalogApp.instance.isHapticsEnabled()) }
    var isAudioEnabled by remember { mutableStateOf(VinylCatalogApp.instance.isAudioFeedbackEnabled()) }
    val feedbackManager = remember { FeedbackManager() }

    DisposableEffect(Unit) {
        onDispose {
            feedbackManager.release()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VinylBlack)
            .verticalScroll(rememberScrollState())
            .padding(top = 44.dp, start = 16.dp, end = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        // --- SENSORY FEEDBACK SECTION ---
        Text(
            text = "Sensory Feedback (Audio & Haptics)",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = VinylGold
        )

        Text(
            text = "Real-time non-visual cues for high-throughput scanning. Confirm locks, matches, and misses without staring at the screen.",
            fontSize = 13.sp,
            color = TextSecondary
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E24))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Haptic Vibration Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = null,
                        tint = if (isHapticsEnabled) VinylGold else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Haptic Feedback",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tactile mechanical clicks on scan lock, pulse on match, double-tap on miss",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
                Switch(
                    checked = isHapticsEnabled,
                    onCheckedChange = {
                        isHapticsEnabled = it
                        VinylCatalogApp.instance.setHapticsEnabled(it)
                        if (it) {
                            feedbackManager.onScanLocked()
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = VinylBlack,
                        checkedTrackColor = VinylGold,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = Color(0xFF2E2E36)
                    )
                )
            }

            HorizontalDivider(color = Color(0xFF2A2A34), thickness = 1.dp)

            // Audio Chimes Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = if (isAudioEnabled) VinylGold else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Audio Chimes",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tone notifications on match success and miss alerts",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
                Switch(
                    checked = isAudioEnabled,
                    onCheckedChange = {
                        isAudioEnabled = it
                        VinylCatalogApp.instance.setAudioFeedbackEnabled(it)
                        if (it) {
                            feedbackManager.onMatchSuccess()
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = VinylBlack,
                        checkedTrackColor = VinylGold,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = Color(0xFF2E2E36)
                    )
                )
            }
        }

        // Test Buttons
        Text(
            text = "Test Sensory Cues",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { feedbackManager.onScanLocked() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF81D4FA)),
                border = BorderStroke(1.dp, Color(0xFF0288D1))
            ) {
                Text("Lock Click", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                onClick = { feedbackManager.onMatchSuccess() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VinylGreen),
                border = BorderStroke(1.dp, VinylGreen)
            ) {
                Text("Match Tone", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                onClick = { feedbackManager.onMatchMiss() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VinylRed),
                border = BorderStroke(1.dp, VinylRed)
            ) {
                Text("Miss Alert", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(color = Color(0xFF2E2E36), thickness = 1.dp)
        Spacer(modifier = Modifier.height(2.dp))

        // --- BACKEND CONNECTION SECTION ---
        Text(
            text = "Backend Server Connection",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = VinylGold
        )

        Text(
            text = "Enter the address of your Debian workstation running the FastAPI backend service (default port is 8000).",
            fontSize = 13.sp,
            color = TextSecondary
        )

        OutlinedTextField(
            value = urlInput,
            onValueChange = {
                urlInput = it
                VinylCatalogApp.instance.setBaseUrl(it)
                connectionStatus = null
            },
            label = { Text("Backend Base URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Presets
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    val emu = "http://10.0.2.2:8000/"
                    urlInput = emu
                    VinylCatalogApp.instance.setBaseUrl(emu)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Emulator (10.0.2.2)", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = {
                isChecking = true
                coroutineScope.launch {
                    val result = repository.checkHealth()
                    isChecking = false
                    result.onSuccess {
                        connectionStatus = true
                        statusMessage = "Successfully connected to FastAPI backend!"
                    }.onFailure { err ->
                        connectionStatus = false
                        statusMessage = "Connection failed: ${err.localizedMessage}"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = VinylGold, contentColor = Color.Black),
            shape = RoundedCornerShape(10.dp)
        ) {
            if (isChecking) {
                CircularProgressIndicator(color = Color.Black, modifier = Modifier.padding(end = 8.dp))
            } else {
                Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            }
            Text("Test Connection", fontWeight = FontWeight.Bold)
        }

        connectionStatus?.let { connected ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (connected) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (connected) VinylGreen else VinylRed,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = statusMessage,
                    color = if (connected) VinylGreen else VinylRed,
                    fontSize = 13.sp
                )
            }
        }
    }
}
