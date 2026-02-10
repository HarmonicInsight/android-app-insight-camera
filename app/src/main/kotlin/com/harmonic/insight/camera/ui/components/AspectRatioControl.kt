package com.harmonic.insight.camera.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harmonic.insight.camera.camera.InsightAspectRatio
import com.harmonic.insight.camera.ui.theme.InsightWhite

@Composable
fun AspectRatioButton(
    aspectRatio: InsightAspectRatio,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggle() }
            .padding(8.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.AspectRatio,
            contentDescription = "Aspect Ratio ${aspectRatio.label}",
            tint = InsightWhite.copy(alpha = 0.8f),
            modifier = Modifier.size(26.dp),
        )
        Text(
            text = aspectRatio.label,
            color = InsightWhite.copy(alpha = 0.8f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
