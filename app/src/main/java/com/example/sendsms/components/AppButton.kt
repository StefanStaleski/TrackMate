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
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.RectangleShape

@Composable
fun AppButton(
    text: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), // Lighter color
    contentColor: Color = Color.White,
    icon: ImageVector? = null,
    shape: Shape = RectangleShape // Less border-radius
) {
    Button(
        onClick = { onClick?.invoke() },
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor, contentColor = contentColor),
        modifier = modifier
            .padding(4.dp) // Padding around the button
            .clip(shape)
            .widthIn(max = 180.dp) // Adjusted maximum width for a narrower button
            .heightIn(min = 36.dp), // Adjusted minimum height for a thinner button
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp) // Adjusted internal padding
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
                    modifier = Modifier.size(20.dp).padding(end = 4.dp) // Icon size and padding
                )
            }
            Text(
                text = text,
                fontSize = 14.sp, // Font size
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}
