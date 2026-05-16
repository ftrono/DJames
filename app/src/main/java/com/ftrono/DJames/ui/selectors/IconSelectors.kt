package com.ftrono.DJames.ui.selectors

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import com.ftrono.DJames.R

//ICONS SELECTORS:

//LIBRARY:
@Composable
fun iconSelector(
    cat: String
): Painter {
    return when (cat) {
        // Modes:
        "mobile" -> painterResource(R.drawable.icon_mobile)
        "car" -> painterResource(R.drawable.icon_car)
        // Guide:
        "info" -> painterResource(R.drawable.icon_fork)
        "music" -> painterResource(R.drawable.logo_spotify)
        "phone" -> painterResource(R.drawable.icon_phone)
        "messages" -> painterResource(R.drawable.icon_message)
        "gmaps" -> painterResource(R.drawable.logo_gmaps)
        // Library:
        "spotify" -> painterResource(R.drawable.logo_spotify)
        "track" -> painterResource(R.drawable.icon_note)
        "artist" -> painterResource(R.drawable.icon_people)
        "album" -> painterResource(R.drawable.icon_disc)
        "playlist" -> painterResource(R.drawable.icon_headphones)
        "podcast" -> painterResource(R.drawable.icon_broadcast)
        "episode" -> painterResource(R.drawable.icon_mic)
        "contact" -> painterResource(R.drawable.icon_phone)
        "place" -> painterResource(R.drawable.icon_place)
        else -> painterResource(R.drawable.icon_help)
    }
}


//MESSAGES:
@Composable
fun messagesIconSelector(
    cat: String
): Painter {
    return when {
        cat.contains("Call") -> painterResource(R.drawable.icon_phone)
        cat.contains("Message") -> painterResource(R.drawable.icon_message)
        cat.contains("Drive") -> painterResource(R.drawable.icon_place)
        cat.contains("Play") -> painterResource(R.drawable.icon_headphones)
        else -> painterResource(R.drawable.icon_help)
    }
}

//ACTIONS:
@Composable
fun actionsIconSelector(
    cat: String
): ImageVector {
    return when {
        cat.contains("Call") -> Icons.Default.Call
        cat.contains("Message") -> Icons.Default.Edit
        cat.contains("Drive") -> Icons.AutoMirrored.Filled.ArrowForward
        cat.contains("Play") -> Icons.Default.PlayArrow
        else -> Icons.AutoMirrored.Filled.ArrowForward
    }
}
