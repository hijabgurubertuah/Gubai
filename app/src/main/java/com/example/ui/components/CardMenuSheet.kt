package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Tombol

@Composable
fun CardMenuDialog(
    tombol: Tombol,
    isActive: Boolean = false,
    onDismissRequest: () -> Unit,
    onEditClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onResetClicked: () -> Unit,
    onReorderClicked: () -> Unit,
    onCloseSessionClicked: (() -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = tombol.nama,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Text(
                    text = tombol.alamat,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                MenuItemRow(
                    icon = Icons.Default.Edit,
                    label = "Ubah",
                    tint = MaterialTheme.colorScheme.onSurface,
                    testTag = "menu_ubah",
                    onClick = {
                        onDismissRequest()
                        onEditClicked()
                    }
                )

                MenuItemRow(
                    icon = Icons.Default.Refresh,
                    label = "Bersihkan Cache",
                    tint = MaterialTheme.colorScheme.onSurface,
                    testTag = "menu_reset",
                    onClick = {
                        onDismissRequest()
                        onResetClicked()
                    }
                )

                MenuItemRow(
                    icon = Icons.Default.SwapVert,
                    label = "Atur Urutan",
                    tint = MaterialTheme.colorScheme.onSurface,
                    testTag = "menu_atur_urutan",
                    onClick = {
                        onDismissRequest()
                        onReorderClicked()
                    }
                )

                if (isActive) {
                    MenuItemRow(
                        icon = Icons.Default.Close,
                        label = "Akhiri Sesi",
                        tint = Color(0xFFEF4444),
                        testTag = "menu_tutup_sesi",
                        onClick = {
                            onDismissRequest()
                            onCloseSessionClicked?.invoke()
                        }
                    )
                }

                MenuItemRow(
                    icon = Icons.Default.Delete,
                    label = "Hapus",
                    tint = MaterialTheme.colorScheme.error,
                    testTag = "menu_hapus",
                    onClick = {
                        onDismissRequest()
                        onDeleteClicked()
                    }
                )
            }
        }
    }
}

@Composable
private fun MenuItemRow(
    icon: ImageVector,
    label: String,
    tint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(vertical = 12.dp, horizontal = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            ),
            color = tint
        )
    }
}
