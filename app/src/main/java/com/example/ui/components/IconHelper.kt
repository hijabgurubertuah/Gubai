package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

data class IconOption(val key: String, val label: String, val icon: ImageVector)

object IconHelper {
    val availableIcons: List<IconOption> = listOf(
        IconOption("Language", "Bahasa/Web", Icons.Default.Language),
        IconOption("School", "Pendidikan", Icons.Default.School),
        IconOption("Movie", "Video", Icons.Default.Movie),
        IconOption("Chat", "Pesan", Icons.Default.Chat),
        IconOption("Mail", "Email", Icons.Default.Email),
        IconOption("ShoppingCart", "Belanja", Icons.Default.ShoppingCart),
        IconOption("Newspaper", "Berita", Icons.Default.Newspaper),
        IconOption("Folder", "Berkas", Icons.Default.Folder),
        IconOption("Work", "Pekerjaan", Icons.Default.Work),
        IconOption("Star", "Bintang", Icons.Default.Star),
        IconOption("Public", "Publik", Icons.Default.Public),
        IconOption("SportsEsports", "Game", Icons.Default.SportsEsports)
    )

    fun getIconByKey(key: String?): ImageVector? {
        if (key.isNullOrBlank()) return null
        return availableIcons.find { it.key.equals(key, ignoreCase = true) }?.icon
    }
}
