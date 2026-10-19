package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Tombol
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealPrimary

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TombolCard(
    tombol: Tombol,
    isActive: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onCloseSession: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val buttonColor = remember(tombol.warna) {
        try {
            Color(android.graphics.Color.parseColor(tombol.warna))
        } catch (e: Exception) {
            TealAccent
        }
    }
    val iconVector = remember(tombol.ikon) {
        IconHelper.getIconByKey(tombol.ikon)
    }
    val firstLetter = remember(tombol.nama) {
        if (tombol.nama.isNotBlank()) tombol.nama.trim().take(1).uppercase() else "G"
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFF042F2E) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isActive) 6.dp else 2.dp,
            pressedElevation = 8.dp
        ),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .testTag("tombol_card_${tombol.id}")
            .clip(RoundedCornerShape(20.dp))
            .drawWithContent {
                drawContent()
                if (isActive) {
                    val strokeWidth = 4.dp.toPx()
                    val halfStroke = strokeWidth / 2f
                    drawRoundRect(
                        color = Color(0xFF00FF66), // Hijau stabilo (Neon lime)
                        topLeft = Offset(halfStroke, halfStroke),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        style = Stroke(width = strokeWidth),
                        cornerRadius = CornerRadius(20.dp.toPx() - halfStroke, 20.dp.toPx() - halfStroke)
                    )
                }
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Close session button at Top-Start (muncul jika halaman sedang aktif berjalan di latar)
            if (isActive) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 6.dp, start = 6.dp)
                        .size(30.dp)
                        .shadow(3.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .clickable { onCloseSession?.invoke() }
                        .testTag("btn_close_session_${tombol.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup Sesi",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Mode Badge in top-right corner
            val isDesktop = tombol.mode.equals("Desktop", ignoreCase = true)
            val badgeBg = if (isDesktop) Color(0xFF1E40AF) else Color(0xFF14B8A6)
            val badgeText = if (isDesktop) "Desktop" else "Mobile"
            val badgeIcon = if (isDesktop) Icons.Default.DesktopWindows else Icons.Default.PhoneAndroid

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeBg)
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = badgeText,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 11.sp
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 8.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))
                // Circle with custom color
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(buttonColor)
                ) {
                    if (iconVector != null) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = tombol.nama,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Text(
                            text = firstLetter,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Text(
                    text = tombol.nama,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    ),
                    color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun AddTombolCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .testTag("add_tombol_card")
            .clip(RoundedCornerShape(20.dp))
            .drawBehind {
                val stroke = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 16f), 0f)
                )
                drawRoundRect(
                    color = borderColor,
                    cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                    style = stroke
                )
            }
            .clickable(onClick = onClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah Situs",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = "Tambah",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}
