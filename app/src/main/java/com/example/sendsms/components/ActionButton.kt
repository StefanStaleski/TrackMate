package com.example.sendsms.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF1E88E5), // Primary Blue color
    contentColor: Color = Color.White,
    shape: Shape = RoundedCornerShape(16.dp) // More rounded corners for a dice-like shape
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor, contentColor = contentColor),
        modifier = modifier
            .padding(8.dp) // Space around the button
            .clip(shape)
            .fillMaxWidth() // Full width button
            .heightIn(min = 100.dp), // Minimum height for larger buttons
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp), // Add elevation for a floating effect
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp) // Internal padding
    ) {
        Text(
            text = text,
            fontSize = 18.sp, // Slightly larger font size for better readability
            color = contentColor
        )
    }
}
