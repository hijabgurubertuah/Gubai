package com.example.ui.components

import android.webkit.URLUtil
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BrowserTopBar(
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

    var showPasteBar by remember { mutableStateOf(false) }
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
                showPasteBar = false // Sembunyikan otomatis setelah dijalankan
                inputUrl = ""
                onOpenUrl(trimmed)
            }
        } else {
            Toast.makeText(context, "Alamat web tidak valid", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF042F2E))
    ) {
        // 1 Bar Berisi Tombol-Tombol di Bagian Atas
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            // Undo (ikon panah melengkung)
            TopBarIconButton(
                icon = Icons.Default.RotateLeft,
                contentDescription = "Undo",
                testTag = "action_undo",
                onClick = onUndo
            )

            // Redo (ikon panah melengkung)
            TopBarIconButton(
                icon = Icons.Default.RotateRight,
                contentDescription = "Redo",
                testTag = "action_redo",
                onClick = onRedo
            )

            // Kembali ke Beranda (ikon saja)
            TopBarIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Kembali",
                testTag = "action_kembali",
                onClick = onGoHome
            )

            // Homepage / Alamat Web Tombol (ikon rumah saja)
            TopBarIconButton(
                icon = Icons.Default.Home,
                contentDescription = "Homepage",
                testTag = "action_homepage",
                onClick = onGoToHomepage
            )

            // Muat Ulang (ikon refresh saja)
            TopBarIconButton(
                icon = Icons.Default.Refresh,
                contentDescription = "Muat Ulang",
                testTag = "action_muat_ulang",
                onClick = onReload
            )

            // Tombol Link (bila ditekan memunculkan kolom paste link di bawahnya)
            TopBarIconButton(
                icon = Icons.Default.Link,
                contentDescription = "Link",
                testTag = "action_toggle_link",
                onClick = {
                    showPasteBar = !showPasteBar
                    if (!showPasteBar) {
                        focusManager.clearFocus()
                    }
                }
            )
        }

        // Kolom paste link yang muncul di bawahnya jika tombol link ditekan
        AnimatedVisibility(
            visible = showPasteBar,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF032221))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF042F2E))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    BasicTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        singleLine = true,
                        cursorBrush = SolidColor(Color.White),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(onGo = { handleOpenUrl(inputUrl) }),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .testTag("input_tempel_link"),
                        decorationBox = { innerTextField ->
                            if (inputUrl.isEmpty()) {
                                Text(
                                    text = "https://...",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 13.sp
                                )
                            }
                            innerTextField()
                        }
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Tombol Paste (icon saja)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F766E))
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
                            contentDescription = "Paste",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Tombol Jalankan (icon saja)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF14B8A6))
                            .clickable { handleOpenUrl(inputUrl) }
                            .testTag("btn_jalankan_link")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Jalankan",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBarIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}
