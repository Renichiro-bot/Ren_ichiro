package com.example.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.example.core.designsystem.theme.VellumShapes
import com.example.core.designsystem.theme.VellumSpacing

@Composable
fun VellumBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    testTag: String = "vellum_badge"
) {
    Box(
        modifier = modifier
            .testTag(testTag)
            .background(color = containerColor, shape = VellumShapes.cornerFull)
            .padding(horizontal = VellumSpacing.s, vertical = VellumSpacing.xxs)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}
