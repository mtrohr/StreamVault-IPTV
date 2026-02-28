package com.streamvault.app.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Typography
import com.streamvault.app.R

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

// ── 10-Foot UI TV Typography System ──────────────────────────────────
// Rules: No text smaller than 14sp (Label Large). 
val StreamVaultTypography = Typography(
    displayLarge  = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold,     fontSize = 48.sp, lineHeight = 56.sp),
    headlineLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold,     fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium= TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    titleLarge    = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 30.sp),
    titleMedium   = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 26.sp),
    titleSmall    = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium,   fontSize = 16.sp, lineHeight = 24.sp),
    bodyLarge     = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal,   fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium    = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal,   fontSize = 16.sp, lineHeight = 24.sp),
    bodySmall     = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal,   fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge    = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium,   fontSize = 16.sp, lineHeight = 24.sp),
    labelMedium   = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium,   fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall    = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium,   fontSize = 14.sp, lineHeight = 20.sp), // Overridden to prevent tiny TV text
)
