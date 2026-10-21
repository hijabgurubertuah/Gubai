package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Language
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
import com.example.data.model.TabEntity
import com.example.ui.theme.BrowserThemeColors
import com.example.ui.theme.calculateBrowserThemeColors

@Composable
fun BrowserTabBar(
    tabs: List<TabEntity>,
    activeTabId: String,
    browserColors: BrowserThemeColors = calculateBrowserThemeColors("#14B8A6"),
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onAddNewTabClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(browserColors.tabBarBackground)
            .padding(horizontal = 6.dp)
            .horizontalScroll(scrollState)
            .testTag("browser_tab_bar")
    ) {
        // Daftar Tab pada Browser ini
        tabs.forEach { tab ->
            val isSelected = tab.tabId == activeTabId

            val tabBg = if (isSelected) browserColors.tabSelectedBackground else browserColors.tabUnselectedBackground
            val tabTextColor = if (isSelected) browserColors.tabSelectedText else browserColors.tabUnselectedText

            Box(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                    .background(tabBg)
                    .clickable { onSelectTab(tab.tabId) }
                    .testTag("tab_item_${tab.tabId}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = if (isSelected) browserColors.accent else browserColors.accent.copy(alpha = 0.7f),
                        modifier = Modifier.size(13.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Judul Tab
                    val displayTitle = tab.title.ifBlank { "Tab Baru" }
                    Text(
                        text = displayTitle,
                        color = tabTextColor,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 140.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Tombol tutup tab '✕'
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White.copy(alpha = 0.18f) else Color.Transparent)
                            .clickable { onCloseTab(tab.tabId) }
                            .testTag("btn_close_tab_${tab.tabId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup Tab $displayTitle",
                            tint = if (isSelected) Color.White else browserColors.tabUnselectedText,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }

                // Indikator garis bawah warna aksen tombol jika tab sedang aktif
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .align(Alignment.BottomCenter)
                            .background(browserColors.accent)
                    )
                }
            }
        }

        // Tombol Tambah Tab (+) di ujung tab bar
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(browserColors.topBarBackground)
                .clickable(onClick = onAddNewTabClick)
                .testTag("btn_tab_bar_add")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Tambah Tab",
                tint = browserColors.accent,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
