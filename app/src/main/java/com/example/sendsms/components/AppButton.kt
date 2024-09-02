package com.example.sendsms.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.RectangleShape

@Composable
fun AppButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color = Color(0xFF4CAF50),
    contentColor: Color = Color.White,
    icon: ImageVector? = null,
    shape: Shape = RoundedCornerShape(12.dp),
    enabled: Boolean = true
) {
    Button(
        onClick = { onClick?.invoke() },
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor, contentColor = contentColor),
        modifier = modifier
            .padding(8.dp) // Padding around the button
            .clip(shape)
            .widthIn(max = 200.dp) // Adjusted maximum width for a narrower button
            .heightIn(min = 48.dp), // Adjusted minimum height for a thinner button
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), // Adjusted internal padding
        enabled = enabled
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(align = Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp).padding(end = 8.dp) // Icon size and padding
                )
            }
            Text(
                text = text,
                fontSize = 16.sp, // Font size
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}
