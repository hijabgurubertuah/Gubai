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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
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
    onUndo: () -> Unit,
    onRedo: () -> Unit,
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

        // Floating Action Menu with uniform solid background color (no nested boxes or contrasting containers)
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
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .widthIn(min = 160.dp, max = 200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F766E)) // Uniform solid color throughout
                    .shadow(12.dp, RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                // Baris Atas: Undo dan Redo (ikon saja, sejajar)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    // Undo (ikon panah kiri saja)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable {
                                isMenuOpen = false
                                onUndo()
                            }
                            .testTag("action_undo")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Undo",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Pemisah jarum vertikal antara Undo dan Redo
                    Box(
                        modifier = Modifier
                            .height(18.dp)
                            .width(1.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.6f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Redo (ikon panah kanan saja)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable {
                                isMenuOpen = false
                                onRedo()
                            }
                            .testTag("action_redo")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Redo",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Pemisah seperti jarum
                NeedleDivider()

                // Baris Kedua: Kembali, Homepage, Muat Ulang (ketiganya icon saja, sejajar horizontal)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    // Kembali (ikon panah kembali saja)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable {
                                isMenuOpen = false
                                onGoHome()
                            }
                            .testTag("action_kembali")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Homepage (ikon rumah saja)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable {
                                isMenuOpen = false
                                onGoToHomepage()
                            }
                            .testTag("action_homepage")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Homepage",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Muat Ulang (ikon refresh saja)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable {
                                isMenuOpen = false
                                onReload()
                            }
                            .testTag("action_muat_ulang")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Muat Ulang",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Pemisah seperti jarum
                NeedleDivider()

                // Baris Ketiga: Kolom Tempel Link sama seperti sekarang
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
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
                                .padding(horizontal = 6.dp)
                                .testTag("input_tempel_link"),
                            decorationBox = { innerTextField ->
                                if (inputUrl.isEmpty()) {
                                    Text(
                                        text = "https://...",
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                                innerTextField()
                            }
                        )

                        // Tombol "Tempel" (berupa ikon saja)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
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
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Tombol "Buka" (berupa ikon saja)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F766E))
                                .clickable { handleOpenUrl(inputUrl) }
                                .testTag("btn_buka_link")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Buka",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NeedleDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = 4.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.2f),
                        Color.White.copy(alpha = 0.7f),
                        Color.White.copy(alpha = 0.2f),
                        Color.Transparent
                    )
                )
            )
    )
}
