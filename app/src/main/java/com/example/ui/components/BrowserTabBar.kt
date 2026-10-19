package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
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
import com.example.data.model.Tombol

@Composable
fun BrowserTabBar(
    activeTabs: List<Tombol>,
    currentTombolId: String,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onAddNewTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Color(0xFF021C1B))
            .padding(horizontal = 6.dp)
            .horizontalScroll(scrollState)
            .testTag("browser_tab_bar")
    ) {
        // Daftar Tab Aktif
        activeTabs.forEach { tabTombol ->
            val isSelected = tabTombol.id == currentTombolId

            val tabBg = if (isSelected) Color(0xFF0F766E) else Color(0xFF032827)
            val tabTextColor = if (isSelected) Color.White else Color(0xFF99F6E4).copy(alpha = 0.7f)
            val tabDotColor = try {
                Color(android.graphics.Color.parseColor(tabTombol.warna))
            } catch (e: Exception) {
                Color(0xFF14B8A6)
            }

            Box(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                    .background(tabBg)
                    .clickable { onSelectTab(tabTombol.id) }
                    .testTag("tab_item_${tabTombol.id}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    // Dot warna tombol situs
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(tabDotColor)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Nama situs / Judul tab
                    Text(
                        text = tabTombol.nama,
                        color = tabTextColor,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 120.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Tombol tutup tab '✕'
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { onCloseTab(tabTombol.id) }
                            .testTag("btn_close_tab_${tabTombol.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup Tab ${tabTombol.nama}",
                            tint = if (isSelected) Color.White else Color(0xFF99F6E4).copy(alpha = 0.6f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // Indikator garis bawah hijau stabilo jika tab sedang aktif
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .align(Alignment.BottomCenter)
                            .background(Color(0xFF00FF66))
                    )
                }
            }
        }

        // Tombol Tambah Tab (+)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color(0xFF042F2E))
                .clickable(onClick = onAddNewTab)
                .testTag("btn_add_new_tab")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Buka Tab Baru",
                tint = Color(0xFF2DD4BF),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
