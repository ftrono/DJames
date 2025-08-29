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
fun libIconSelector(
    cat: String
): Painter {
    return when (cat) {
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


//GUIDE:
@Composable
fun guideIconSelector(
    cat: String
): Painter {
    return when (cat) {
        "calls" -> painterResource(R.drawable.icon_phone)
        "messages" -> painterResource(R.drawable.icon_message)
        "places" -> painterResource(R.drawable.icon_place)
        "car" -> painterResource(R.drawable.icon_car)
        "play" -> painterResource(R.drawable.icon_headphones)
        else -> painterResource(R.drawable.icon_help)
    }
}


//MESSAGES:
@Composable
fun messagesIconSelector(
    cat: String
): Painter {
    return when {
        cat == "CallRequest" -> painterResource(R.drawable.icon_phone)
        cat == "MessageRequest" -> painterResource(R.drawable.icon_message)
        cat == "DriveRequest" -> painterResource(R.drawable.icon_place)
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
        cat == "CallRequest" -> Icons.Default.Call
        cat == "MessageRequest" -> Icons.Default.Edit
        cat == "DriveRequest" -> Icons.AutoMirrored.Filled.ArrowForward
        cat.contains("Play") -> Icons.Default.PlayArrow
        else -> Icons.AutoMirrored.Filled.ArrowForward
    }
}
