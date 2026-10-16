package com.example.ui.components

import android.webkit.URLUtil
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun KioskFloatingMenu(
    onReload: () -> Unit,
    onGoHome: () -> Unit,
    onGoToHomepage: () -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current

    var isMenuOpen by remember { mutableStateOf(false) }
    var inputUrl by remember { mutableStateOf("") }

    val handleOpenUrl: (String) -> Unit = { rawUrl ->
        var trimmed = rawUrl.trim()
        if (trimmed.isNotBlank()) {
            if (!trimmed.startsWith("http://", ignoreCase = true) &&
                !trimmed.startsWith("https://", ignoreCase = true)
            ) {
                trimmed = "https://$trimmed"
            }

            if (!URLUtil.isValidUrl(trimmed) || !trimmed.contains(".")) {
                Toast.makeText(context, "Alamat web tidak valid", Toast.LENGTH_SHORT).show()
            } else {
                focusManager.clearFocus()
                isMenuOpen = false
                inputUrl = ""
                onOpenUrl(trimmed)
            }
        } else {
            Toast.makeText(context, "Alamat web tidak valid", Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Outside scrim detector to close menu on tap outside
        if (isMenuOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isMenuOpen = false
                        focusManager.clearFocus()
                    }
            )
        }

        // Fixed Vertical Handle on Left Edge, exactly vertically centered
        // Height 120 dp, Width 25 dp, translucent toska, rounded right corners, single hamburger icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .height(120.dp)
                .width(25.dp)
                .shadow(4.dp, RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp))
                .clip(RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp))
                .background(Color(0xFF0F766E).copy(alpha = 0.85f))
                .clickable {
                    isMenuOpen = !isMenuOpen
                    if (!isMenuOpen) focusManager.clearFocus()
                }
                .testTag("kiosk_menu_handle")
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        // Floating Action Menu Buttons (appearing near the handle when open)
        AnimatedVisibility(
            visible = isMenuOpen,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { -it / 2 }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { -it / 2 }),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 32.dp, end = 16.dp)
                .imePadding()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF042F2E).copy(alpha = 0.92f))
                    .padding(14.dp)
                    .shadow(12.dp, RoundedCornerShape(20.dp))
            ) {
                // 1. Muat Ulang
                KioskActionButton(
                    icon = Icons.Default.Refresh,
                    label = "Muat Ulang",
                    testTag = "action_muat_ulang",
                    onClick = {
                        isMenuOpen = false
                        onReload()
                    }
                )

                // 2. Halaman Utama (Kembali ke Beranda) -> ikon kembali
                KioskActionButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    label = "Halaman Utama",
                    testTag = "action_halaman_utama",
                    onClick = {
                        isMenuOpen = false
                        onGoHome()
                    }
                )

                // 3. Homepage (Alamat Web Tombol) -> ikon rumah
                KioskActionButton(
                    icon = Icons.Default.Home,
                    label = "Homepage",
                    testTag = "action_homepage",
                    onClick = {
                        isMenuOpen = false
                        onGoToHomepage()
                    }
                )

                // 4. Kolom Tempel Link (tombol Tempel dan Buka berupa ikon saja)
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        BasicTextField(
                            value = inputUrl,
                            onValueChange = { inputUrl = it },
                            singleLine = true,
                            cursorBrush = SolidColor(Color(0xFF0F766E)),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(onGo = { handleOpenUrl(inputUrl) }),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                                .testTag("input_tempel_link"),
                            decorationBox = { innerTextField ->
                                if (inputUrl.isEmpty()) {
                                    Text(
                                        text = "https://...",
                                        color = Color.Gray,
                                        fontSize = 13.sp
                                    )
                                }
                                innerTextField()
                            }
                        )

                        // Tombol "Tempel" (berupa ikon saja)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0F2FE))
                                .clickable {
                                    val clipText = clipboardManager.getText()?.text
                                    if (!clipText.isNullOrBlank()) {
                                        inputUrl = clipText
                                    } else {
                                        Toast.makeText(context, "Papan klip kosong", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .testTag("btn_tempel_link")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Tempel",
                                tint = Color(0xFF0369A1),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Tombol "Buka" (berupa ikon saja)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F766E))
                                .clickable { handleOpenUrl(inputUrl) }
                                .testTag("btn_buka_link")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Buka",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KioskActionButton(
    icon: ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF14B8A6))
                .shadow(4.dp, CircleShape)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}
