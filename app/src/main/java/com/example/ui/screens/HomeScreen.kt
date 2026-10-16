package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Tombol
import com.example.ui.components.AddTombolCard
import com.example.ui.components.CardMenuDialog
import com.example.ui.components.FormTombolDialog
import com.example.ui.components.ReorderDialog
import com.example.ui.components.TombolCard

@Composable
fun HomeScreen(
    tombolList: List<Tombol>,
    activeSessionIds: Set<String> = emptySet(),
    onOpenBrowser: (String) -> Unit,
    onSaveTombol: (Tombol) -> Unit,
    onDeleteTombol: (Tombol) -> Unit,
    onResetTombolData: (Tombol) -> Unit,
    onCloseSession: (String) -> Unit = {},
    onSaveReorder: (List<Tombol>) -> Unit,
    modifier: Modifier = Modifier
) {
    var showFormDialog by remember { mutableStateOf(false) }
    var editingTombol by remember { mutableStateOf<Tombol?>(null) }
    var selectedMenuTombol by remember { mutableStateOf<Tombol?>(null) }
    var deletingTombol by remember { mutableStateOf<Tombol?>(null) }
    var resettingTombol by remember { mutableStateOf<Tombol?>(null) }
    var showReorderDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header: Judul kecil "GubAI" di bagian atas
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp)
            ) {
                Text(
                    text = "GubAI",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    ),
                    modifier = Modifier.testTag("app_title_gubai")
                )
            }

            if (tombolList.isEmpty()) {
                // Keadaan kosong "Belum ada situs" dengan tombol "Tambah Situs"
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.padding(bottom = 20.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(90.dp)
                                    .padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = "Belum ada situs",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(52.dp)
                                )
                            }
                        }

                        Text(
                            text = "Belum ada situs",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                editingTombol = null
                                showFormDialog = true
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                            modifier = Modifier.testTag("button_tambah_situs_empty")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(text = "Tambah Situs", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                // Kisi 3 kolom kartu tombol situs + kartu "+" di akhir daftar
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("tombol_grid")
                ) {
                    items(tombolList, key = { it.id }) { tombol ->
                        val isSessionActive = activeSessionIds.contains(tombol.id)
                        TombolCard(
                            tombol = tombol,
                            isActive = isSessionActive,
                            onClick = { onOpenBrowser(tombol.id) },
                            onLongClick = { selectedMenuTombol = tombol },
                            onCloseSession = { onCloseSession(tombol.id) }
                        )
                    }

                    // Kartu "+" berbingkai putus-putus berada di akhir daftar
                    item {
                        AddTombolCard(
                            onClick = {
                                editingTombol = null
                                showFormDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Form Tambah / Ubah Tombol
    if (showFormDialog) {
        FormTombolDialog(
            initialTombol = editingTombol,
            onDismissRequest = {
                showFormDialog = false
                editingTombol = null
            },
            onSaveTombol = { savedTombol ->
                showFormDialog = false
                editingTombol = null
                onSaveTombol(savedTombol)
            }
        )
    }

    // Context Menu Dialog (Ubah, Reset Data Tombol, Atur Urutan, Hapus)
    selectedMenuTombol?.let { tombol ->
        CardMenuDialog(
            tombol = tombol,
            isActive = activeSessionIds.contains(tombol.id),
            onDismissRequest = { selectedMenuTombol = null },
            onEditClicked = {
                editingTombol = tombol
                showFormDialog = true
            },
            onDeleteClicked = {
                deletingTombol = tombol
            },
            onResetClicked = {
                resettingTombol = tombol
            },
            onReorderClicked = {
                showReorderDialog = true
            },
            onCloseSessionClicked = {
                onCloseSession(tombol.id)
            }
        )
    }

    // Reorder Dialog
    if (showReorderDialog) {
        ReorderDialog(
            initialList = tombolList,
            onDismissRequest = { showReorderDialog = false },
            onSaveReorder = onSaveReorder
        )
    }

    // Hapus Confirmation Dialog
    deletingTombol?.let { tombol ->
        AlertDialog(
            onDismissRequest = { deletingTombol = null },
            title = { Text("Hapus Tombol") },
            text = { Text("Hapus tombol ini beserta data browsernya?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDelete = tombol
                        deletingTombol = null
                        onDeleteTombol(toDelete)
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingTombol = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Reset Data Tombol Confirmation Dialog
    resettingTombol?.let { tombol ->
        AlertDialog(
            onDismissRequest = { resettingTombol = null },
            title = { Text("Reset Data Tombol") },
            text = { Text("Hapus login, cache, dan history tombol ini?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toReset = tombol
                        resettingTombol = null
                        onResetTombolData(toReset)
                    }
                ) {
                    Text("Reset", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { resettingTombol = null }) {
                    Text("Batal")
                }
            }
        )
    }
}
