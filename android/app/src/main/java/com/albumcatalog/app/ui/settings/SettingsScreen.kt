package com.albumcatalog.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VinylBlack)
            .padding(top = 44.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

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
