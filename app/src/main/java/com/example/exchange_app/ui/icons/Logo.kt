package com.example.exchange_app.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val AppLogo: ImageVector
    get() {
        if (_logo != null) return _logo!!
        _logo = ImageVector.Builder(
            name = "AppLogo",
            defaultWidth = 100.dp,
            defaultHeight = 100.dp,
            viewportWidth = 100f,
            viewportHeight = 100f
        ).path(
            fill = SolidColor(Color(0xFF3F51B5)), // Primary Color
            pathFillType = PathFillType.NonZero
        ) {
            // Background Circle
            moveTo(50f, 0f)
            curveTo(22.4f, 0f, 0f, 22.4f, 0f, 50f)
            curveTo(0f, 77.6f, 22.4f, 100f, 50f, 100f)
            curveTo(77.6f, 100f, 100f, 77.6f, 100f, 50f)
            curveTo(100f, 22.4f, 77.6f, 0f, 50f, 0f)
            close()
        }.path(
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.NonZero
        ) {
            // Top Arrow
            moveTo(30f, 40f)
            lineTo(70f, 40f)
            lineTo(70f, 30f)
            lineTo(85f, 45f)
            lineTo(70f, 60f)
            lineTo(70f, 50f)
            lineTo(30f, 50f)
            close()
            
            // Bottom Arrow
            moveTo(70f, 60f)
            lineTo(30f, 60f)
            lineTo(30f, 70f)
            lineTo(15f, 55f)
            lineTo(30f, 40f)
            lineTo(30f, 50f)
            lineTo(70f, 50f)
            close()
        }.build()
        return _logo!!
    }

private var _logo: ImageVector? = null
