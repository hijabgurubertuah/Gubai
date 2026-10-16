package com.example.ui.components

import android.webkit.URLUtil
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Tombol
import com.example.ui.theme.ButtonColorOptions
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealPrimary

@Composable
fun FormTombolDialog(
    initialTombol: Tombol? = null,
    onDismissRequest: () -> Unit,
    onSaveTombol: (Tombol) -> Unit
) {
    val isEditMode = initialTombol != null

    var nama by remember { mutableStateOf(initialTombol?.nama ?: "") }
    var alamat by remember { mutableStateOf(initialTombol?.alamat ?: "") }
    var selectedColor by remember { mutableStateOf(initialTombol?.warna ?: "#14B8A6") }
    var selectedIcon by remember { mutableStateOf(initialTombol?.ikon) }
    var selectedMode by remember { mutableStateOf(initialTombol?.mode ?: "Mobile") }

    var namaError by remember { mutableStateOf<String?>(null) }
    var alamatError by remember { mutableStateOf<String?>(null) }
    var showUnsavedDialog by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    // Check if form has changed from initial
    val isDirty = remember(nama, alamat, selectedColor, selectedIcon, selectedMode) {
        if (initialTombol == null) {
            nama.isNotBlank() || alamat.isNotBlank() || selectedColor != "#14B8A6" || selectedIcon != null || selectedMode != "Mobile"
        } else {
            nama != initialTombol.nama ||
                    alamat != initialTombol.alamat ||
                    selectedColor != initialTombol.warna ||
                    selectedIcon != initialTombol.ikon ||
                    selectedMode != initialTombol.mode
        }
    }

    val handleDismiss = {
        if (isDirty) {
            showUnsavedDialog = true
        } else {
            onDismissRequest()
        }
    }

    val validateAndSave = {
        val trimmedNama = nama.trim()
        var trimmedAlamat = alamat.trim()

        var hasError = false
        if (trimmedNama.isEmpty()) {
            namaError = "Nama wajib diisi"
            hasError = true
        } else {
            namaError = null
        }

        if (trimmedAlamat.isEmpty()) {
            alamatError = "Alamat web tidak valid"
            hasError = true
        } else {
            // Auto-prepend https:// if missing
            if (!trimmedAlamat.startsWith("http://", ignoreCase = true) &&
                !trimmedAlamat.startsWith("https://", ignoreCase = true)
            ) {
                trimmedAlamat = "https://$trimmedAlamat"
            }

            if (!URLUtil.isValidUrl(trimmedAlamat) || !trimmedAlamat.contains(".")) {
                alamatError = "Alamat web tidak valid"
                hasError = true
            } else {
                alamatError = null
            }
        }

        if (!hasError) {
            val savedTombol = if (initialTombol != null) {
                initialTombol.copy(
                    nama = trimmedNama,
                    alamat = trimmedAlamat,
                    warna = selectedColor,
                    ikon = selectedIcon,
                    mode = selectedMode,
                    // If address changed, reset last url to new address
                    alamatTerakhir = if (trimmedAlamat != initialTombol.alamat) trimmedAlamat else initialTombol.alamatTerakhir
                )
            } else {
                Tombol(
                    id = java.util.UUID.randomUUID().toString(),
                    nama = trimmedNama,
                    alamat = trimmedAlamat,
                    warna = selectedColor,
                    ikon = selectedIcon,
                    urutan = 0,
                    alamatTerakhir = trimmedAlamat,
                    mode = selectedMode
                )
            }
            onSaveTombol(savedTombol)
        }
    }

    Dialog(
        onDismissRequest = handleDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = if (isEditMode) "Ubah Tombol" else "Tambah Tombol",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Nama Field
                OutlinedTextField(
                    value = nama,
                    onValueChange = {
                        nama = it
                        if (namaError != null && it.isNotBlank()) namaError = null
                    },
                    label = { Text("Nama") },
                    isError = namaError != null,
                    supportingText = namaError?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_nama_tombol")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Alamat Web Field
                OutlinedTextField(
                    value = alamat,
                    onValueChange = {
                        alamat = it
                        if (alamatError != null && it.isNotBlank()) alamatError = null
                    },
                    label = { Text("Alamat Web") },
                    placeholder = { Text("contoh: google.com") },
                    isError = alamatError != null,
                    supportingText = alamatError?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_alamat_tombol")
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Pilihan Mode (Mobile / Desktop)
                Text(
                    text = "Mode Tampilan",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp)
                ) {
                    val isMobile = selectedMode.equals("Mobile", ignoreCase = true)
                    val isDesktop = selectedMode.equals("Desktop", ignoreCase = true)

                    // Segment Mobile
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isMobile) Color(0xFF14B8A6) else Color.Transparent)
                            .clickable { selectedMode = "Mobile" }
                            .padding(vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = if (isMobile) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mobile",
                            fontWeight = FontWeight.Bold,
                            color = if (isMobile) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }

                    // Segment Desktop
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDesktop) Color(0xFF1E40AF) else Color.Transparent)
                            .clickable { selectedMode = "Desktop" }
                            .padding(vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DesktopWindows,
                            contentDescription = null,
                            tint = if (isDesktop) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Desktop",
                            fontWeight = FontWeight.Bold,
                            color = if (isDesktop) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Color Selection
                Text(
                    text = "Pilihan Warna",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    ButtonColorOptions.forEach { (hex, _) ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = selectedColor.equals(hex, ignoreCase = true)

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hex }
                                .then(
                                    if (isSelected) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    } else Modifier
                                )
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Terpilih",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Icon Selection
                Text(
                    text = "Pilihan Ikon",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Option: "Huruf Pertama Nama"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { selectedIcon = null }
                        .background(
                            if (selectedIcon == null) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = if (nama.isNotBlank()) nama.trim().take(1).uppercase() else "A",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Huruf pertama nama",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (selectedIcon == null) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Icons List
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                ) {
                    items(IconHelper.availableIcons) { iconOpt ->
                        val isSelected = selectedIcon.equals(iconOpt.key, ignoreCase = true)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable { selectedIcon = iconOpt.key }
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            2.dp,
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(12.dp)
                                        )
                                    } else Modifier
                                )
                        ) {
                            Icon(
                                imageVector = iconOpt.icon,
                                contentDescription = iconOpt.label,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = handleDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("button_batal_form")
                    ) {
                        Text("Batal")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = validateAndSave,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("button_simpan_form")
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Perubahan Belum Disimpan") },
            text = { Text("Ada perubahan yang belum disimpan. Yakin ingin keluar?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnsavedDialog = false
                        onDismissRequest()
                    }
                ) {
                    Text("Keluar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
