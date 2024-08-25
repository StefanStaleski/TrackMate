package com.example.sendsms.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.sendsms.R

@Composable
fun BottomNavigationButton(
    navController: NavHostController,
    route: String,
    label: String? = null, // Make label optional
    isSelected: Boolean,
    iconResId: Int? = null
) {
    Button(
        onClick = { navController.navigate(route) },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF6200EE) else Color(0xFFDDDDDD),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp), // Use RoundedCornerShape for consistent radius
        elevation = ButtonDefaults.elevatedButtonElevation(8.dp),
        modifier = Modifier
            .width(100.dp) // Set a fixed width for each button
            .padding(horizontal = 4.dp) // Add padding for spacing between buttons
            .background(color = if (isSelected) Color(0xFF6200EE) else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
            ) // Highlight background
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (iconResId != null) {
                Icon(
                    painter = painterResource(id = iconResId),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp) // Adjust the size of the icon as needed
                )
            }
            if (label != null) {
                Spacer(modifier = Modifier.width(8.dp)) // Add space between icon and text
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BottomNavigationButtonPreview() {
    BottomNavigationButton(
        navController = rememberNavController(),
        route = "sampleRoute",
        label = "Sample",
        isSelected = true,
        iconResId = R.drawable.person_24px // Replace with an actual drawable resource for preview
    )
}
