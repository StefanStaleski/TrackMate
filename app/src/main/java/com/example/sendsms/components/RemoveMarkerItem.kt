package com.example.sendsms.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RemoveMarkerItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconColor = Color(0xFF4CAF50)
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Delete,
            contentDescription = "Remove All Markers",
            tint = iconColor,
            modifier = Modifier
                .size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Remove M",
            style = MaterialTheme.typography.body1.copy(color = iconColor, fontSize = 12.sp)
        )
    }
}