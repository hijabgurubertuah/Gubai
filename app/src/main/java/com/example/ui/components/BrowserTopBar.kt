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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.example.ui.theme.BrowserThemeColors
import com.example.ui.theme.calculateBrowserThemeColors

@Composable
fun BrowserTopBar(
    browserColors: BrowserThemeColors = calculateBrowserThemeColors("#14B8A6"),
    canGoBack: Boolean = false,
    canGoForward: Boolean = false,
    isAddTabOpen: Boolean = false,
    onToggleAddTab: () -> Unit = {},
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onReload: () -> Unit,
    onGoHome: () -> Unit,
    onGoToHomepage: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onAddNewTab: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current

    var showPasteBar by remember { mutableStateOf(false) }
    var inputUrl by remember { mutableStateOf("") }

    var newTabInput by remember { mutableStateOf("") }
    val newTabFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isAddTabOpen) {
        if (isAddTabOpen) {
            newTabFocusRequester.requestFocus()
        }
    }

    val handleOpenCurrentUrl: (String) -> Unit = { rawUrl ->
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
                showPasteBar = false
                inputUrl = ""
                onOpenUrl(trimmed)
            }
        } else {
            Toast.makeText(context, "Alamat web tidak valid", Toast.LENGTH_SHORT).show()
        }
    }

    val handleAddNewTabAction: () -> Unit = {
        val trimmed = newTabInput.trim()
        if (trimmed.isNotBlank()) {
            focusManager.clearFocus()
            onToggleAddTab()
            newTabInput = ""
            onAddNewTab(trimmed)
        } else {
            Toast.makeText(context, "Ketik pencarian atau alamat web", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(browserColors.topBarBackground)
    ) {
        // 1 Bar Berisi Tombol-Tombol Navigasi & Aksi
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            // Undo (Aktif/Redup sesuai canGoBack pada tab aktif)
            TopBarIconButton(
                icon = Icons.Default.RotateLeft,
                contentDescription = "Undo",
                testTag = "action_undo",
                enabled = canGoBack,
                onClick = onUndo
            )

            // Redo (Aktif/Redup sesuai canGoForward pada tab aktif)
            TopBarIconButton(
                icon = Icons.Default.RotateRight,
                contentDescription = "Redo",
                testTag = "action_redo",
                enabled = canGoForward,
                onClick = onRedo
            )

            // Kembali ke Beranda (kisi tombol) - tetap hidup di background
            TopBarIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Kembali ke Beranda",
                testTag = "action_kembali",
                enabled = true,
                onClick = onGoHome
            )

            // Homepage (hanya pada tab aktif)
            TopBarIconButton(
                icon = Icons.Default.Home,
                contentDescription = "Homepage",
                testTag = "action_homepage",
                enabled = true,
                onClick = onGoToHomepage
            )

            // Muat Ulang (hanya pada tab aktif)
            TopBarIconButton(
                icon = Icons.Default.Refresh,
                contentDescription = "Muat Ulang",
                testTag = "action_muat_ulang",
                enabled = true,
                onClick = onReload
            )

            // Tambah Tab Baru (+)
            TopBarIconButton(
                icon = Icons.Default.Add,
                contentDescription = "Tambah Tab",
                testTag = "action_tambah_tab",
                enabled = true,
                tint = if (isAddTabOpen) Color.White else browserColors.accent,
                onClick = {
                    if (!isAddTabOpen && showPasteBar) {
                        showPasteBar = false
                    }
                    onToggleAddTab()
                }
            )

            // Tombol Link (hanya pada tab aktif)
            TopBarIconButton(
                icon = Icons.Default.Link,
                contentDescription = "Link",
                testTag = "action_toggle_link",
                enabled = true,
                tint = if (showPasteBar) browserColors.accent else Color.White,
                onClick = {
                    showPasteBar = !showPasteBar
                    if (showPasteBar && isAddTabOpen) {
                        onToggleAddTab()
                    }
                    if (!showPasteBar) {
                        focusManager.clearFocus()
                    }
                }
            )
        }

        // 2. Bar Input Tambah Tab Baru yang muncul saat tombol + ditekan
        AnimatedVisibility(
            visible = isAddTabOpen,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(browserColors.subBarBackground)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(browserColors.topBarBackground)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Cari",
                        tint = browserColors.accent,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    BasicTextField(
                        value = newTabInput,
                        onValueChange = { newTabInput = it },
                        singleLine = true,
                        cursorBrush = SolidColor(browserColors.accent),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontSize = 13.sp
                        ),
                        keyboardOptions = KeyboardOptions(
                            autoCorrect = false,
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = { handleAddNewTabAction() },
                            onSearch = { handleAddNewTabAction() }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(newTabFocusRequester)
                            .testTag("input_tab_baru"),
                        decorationBox = { innerTextField ->
                            if (newTabInput.isEmpty()) {
                                Text(
                                    text = "Cari di Google atau ketik alamat web",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 13.sp
                                )
                            }
                            innerTextField()
                        }
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Tombol Paste
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(browserColors.primary)
                            .clickable {
                                val clipText = clipboardManager.getText()?.text
                                if (!clipText.isNullOrBlank()) {
                                    newTabInput = clipText
                                } else {
                                    Toast.makeText(context, "Papan klip kosong", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .testTag("btn_paste_tab_baru")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Tombol Cari / Buka Tab
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(browserColors.accent)
                            .clickable { handleAddNewTabAction() }
                            .testTag("btn_buka_tab_baru")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Buka Tab",
                            tint = browserColors.topBarBackground,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Tombol Batal
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable {
                                focusManager.clearFocus()
                                onToggleAddTab()
                                newTabInput = ""
                            }
                            .testTag("btn_batal_tab_baru")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Batal",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 3. Kolom Paste Link Biasa (pada tab aktif sekarang)
        AnimatedVisibility(
            visible = showPasteBar,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(browserColors.subBarBackground)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(browserColors.topBarBackground)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    BasicTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        singleLine = true,
                        cursorBrush = SolidColor(Color.White),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontSize = 13.sp
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(onGo = { handleOpenCurrentUrl(inputUrl) }),
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
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(browserColors.primary)
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
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Tombol Jalankan (icon saja)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(browserColors.accent)
                            .clickable { handleOpenCurrentUrl(inputUrl) }
                            .testTag("btn_jalankan_link")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Jalankan",
                            tint = browserColors.topBarBackground,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBarIconButton(
    icon: ImageVector,
    contentDescription: String,
    testTag: String,
    enabled: Boolean = true,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    val effectiveTint = if (enabled) tint else Color.White.copy(alpha = 0.28f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .then(
                if (enabled) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = effectiveTint,
            modifier = Modifier.size(19.dp)
        )
    }
}
