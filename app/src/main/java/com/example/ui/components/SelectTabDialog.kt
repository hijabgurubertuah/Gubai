package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Tombol

@Composable
fun SelectTabDialog(
    allTombolList: List<Tombol>,
    activeSessionIds: Set<String>,
    currentTombolId: String,
    onDismissRequest: () -> Unit,
    onSelectTombol: (String) -> Unit,
    onAddNewTombol: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Buka Tab Situs",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // List of all sites
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    items(allTombolList, key = { it.id }) { tombol ->
                        val isActive = activeSessionIds.contains(tombol.id)
                        val isCurrent = tombol.id == currentTombolId

                        val buttonColor = try {
                            Color(android.graphics.Color.parseColor(tombol.warna))
                        } catch (e: Exception) {
                            Color(0xFF14B8A6)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    onDismissRequest()
                                    onSelectTombol(tombol.id)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("select_tab_item_${tombol.id}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(buttonColor)
                            ) {
                                val icon = IconHelper.getIconByKey(tombol.ikon)
                                if (icon != null) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Text(
                                        text = tombol.nama.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tombol.nama,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = tombol.alamat,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (isActive) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF042F2E))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = if (isCurrent) "Aktif" else "Latar",
                                        color = Color(0xFF00FF66),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tombol Tambah Situs Baru
                OutlinedButton(
                    onClick = {
                        onDismissRequest()
                        onAddNewTombol()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tambah Situs Baru")
                }
            }
        }
    }
}
