package com.nfctools.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nfctools.app.viewmodel.NFCViewModel
import com.nfctools.app.ui.theme.NfcGreen
import com.nfctools.app.ui.theme.NfcBlue
import com.nfctools.app.ui.theme.NfcPurple
import com.nfctools.app.ui.theme.NfcOrange
import com.nfctools.app.ui.theme.NfcCyan
import com.nfctools.app.ui.theme.NfcYellow

@Composable
fun ScanScreen(viewModel: NFCViewModel) {
    val currentTag by viewModel.currentTag
    val isLoading by viewModel.isLoading
    val nfcEnabled by viewModel.nfcEnabled
    val nfcSupported by viewModel.nfcSupported

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!nfcSupported) {
            NfcNotSupported()
        } else if (!nfcEnabled) {
            NfcDisabled(viewModel)
        } else {
            AnimatedContent(
                targetState = currentTag != null,
                transitionSpec = { fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300)) }
            ) { hasTag ->
                if (hasTag) {
                    TagQuickView(viewModel)
                } else {
                    ScanningIndicator()
                }
            }
        }
    }
}

@Composable
fun ScanningIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "alpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Animated NFC rings
        Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
            // Outer ring
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(scale)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = alpha * 0.3f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0f)
                            )
                        ),
                        shape = CircleShape
                    )
            )
            // Middle ring
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale * 0.85f)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = alpha * 0.5f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0f)
                            )
                        ),
                        shape = CircleShape
                    )
            )
            // NFC Icon
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Nfc,
                        contentDescription = "NFC",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Text("Ready to Scan", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Text(
            "Approach an NFC tag to read its information",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Supported tags card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.DevicesOther, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Supported Tags", style = MaterialTheme.typography.titleMedium)
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TagChip("MIFARE Classic (1K/4K)", NfcGreen)
                    TagChip("MIFARE Ultralight / NTAG", NfcBlue)
                    TagChip("MIFARE DESFire / ISO-DEP", NfcPurple)
                    TagChip("NFC Forum Type 1-5", NfcOrange)
                    TagChip("FeliCa / NFC-F", NfcCyan)
                    TagChip("ISO 15693 / NFC-V", NfcYellow)
                }
            }
        }
    }
}

@Composable
fun TagChip(text: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color.copy(alpha = 0.9f)
        )
    }
}

@Composable
fun TagQuickView(viewModel: NFCViewModel) {
    val tag = viewModel.currentTag.value ?: return

    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Success animation
        val scaleAnim = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            scaleAnim.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        }

        Surface(
            modifier = Modifier.size(100.dp).scale(scaleAnim.value),
            shape = CircleShape,
            color = NfcGreen.copy(alpha = 0.15f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Tag found",
                    modifier = Modifier.size(56.dp),
                    tint = NfcGreen
                )
            }
        }

        Text("Tag Detected!", style = MaterialTheme.typography.headlineMedium, color = NfcGreen)

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoRow("Type", tag.type)
                InfoRow("ID", tag.hexId.take(16) + "...")
                InfoRow("Size", "${tag.size} bytes")
                InfoRow("Writable", if (tag.writable) "Yes ✓" else "No ✗")
            }
        }

        OutlinedButton(
            onClick = { viewModel.clearCurrentTag() },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.Clear, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Clear Tag")
        }
    }
}

@Composable
fun NfcNotSupported() {
    StatusMessage(
        icon = Icons.Default.Error,
        title = "NFC Not Supported",
        message = "This device does not have NFC hardware",
        iconTint = MaterialTheme.colorScheme.error
    )
}

@Composable
fun NfcDisabled(viewModel: NFCViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StatusMessage(
            icon = Icons.Default.Nfc,
            title = "NFC is Disabled",
            message = "Please enable NFC in settings to use this app",
            iconTint = MaterialTheme.colorScheme.error
        )
        Button(onClick = { viewModel.openNfcSettings() }, shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Open NFC Settings")
        }
    }
}

@Composable
fun StatusMessage(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, message: String, iconTint: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(modifier = Modifier.size(80.dp), shape = CircleShape, color = iconTint.copy(alpha = 0.12f)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(40.dp), tint = iconTint)
            }
        }
        Text(title, style = MaterialTheme.typography.headlineMedium, color = iconTint)
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
    }
}
