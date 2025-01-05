package com.ftrono.DJames.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.MutableLiveData
import com.ftrono.DJames.R
import com.ftrono.DJames.application.overlayStatus
import com.ftrono.DJames.ui.theme.light_grey
import kotlinx.coroutines.launch
import kotlin.math.round


@Preview
@Composable
fun ClockButtonPreview() {
    ClockButton(
        bubbleSize = 100,
        currentTime = MutableLiveData<String>("00:00"),
        onTap = {offset -> }
    )
}


@Preview
@Composable
fun DJamesButtonPreview() {
    DJamesButton(
        bubbleSize = 100,
        overlayStatus = MutableLiveData<String>("ready"),
        onTap = {offset -> }
    )
}



@Composable
fun DJamesButton(
    bubbleSize: Int,
    overlayStatus: MutableLiveData<String>,
    onTap: (Offset) -> Unit
) {
    val overlayState by overlayStatus.observeAsState()
    //OVERLAY BUTTON:
    Box(
        modifier = Modifier
            .size(bubbleSize.dp)
            .clip(CircleShape)
            .background(
                color = when (overlayState) {
                    "busy" -> {
                        colorResource(id = R.color.colorBusy)
                    }

                    "processing" -> {
                        colorResource(id = R.color.faded_grey)
                    }

                    else -> {
                        colorResource(id = R.color.colorPrimary)
                    }
                }
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    //ON SINGLE TAP:
                    onTap = onTap
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (overlayState == "processing") {
                CircularProgressIndicator(
                    modifier = Modifier.width(40.dp),
                    color = colorResource(id = R.color.light_grey),
                    trackColor = colorResource(id = R.color.dark_grey),
                    strokeWidth = 8.dp
                )
            } else if (overlayState == "busy") {
                PulsatingWaveform()
            } else {
                Image(
                    modifier = Modifier
                        .size(50.dp),
                    painter = painterResource(id = R.drawable.djames),
                    contentDescription = "DJames Overlay Bubble"
                )
            }
        }
    }
}


@Composable
fun ClockButton(
    bubbleSize: Int,
    currentTime: MutableLiveData<String>,
    onTap: (Offset) -> Unit
) {
    val currentTimeState by currentTime.observeAsState()
    //OVERLAY BUTTON:
    Card(
        modifier = Modifier
            .padding(bottom = 8.dp)
            .width(round(bubbleSize.toDouble() / 1.5).dp)
            .wrapContentHeight()
            .pointerInput(Unit) {
                detectTapGestures(
                    //ON SINGLE TAP:
                    onTap = onTap
                )
            },
        border = BorderStroke(0.5.dp, colorResource(id = R.color.faded_grey)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors (
            containerColor = colorResource(id = R.color.black)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text (
                modifier = Modifier
                    .padding(top=8.dp),
                text = currentTimeState!!,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.light_grey)
            )
            Text (
                modifier = Modifier
                    .padding(bottom=8.dp),
                text = "CLOCK",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.mid_grey)
            )
        }
    }

}


@Preview
@Composable
fun OverlayClose(
) {
    OutlinedButton(
        modifier= Modifier
            .padding(bottom = 25.dp)
            .size(50.dp)
            .zIndex(1f),  //avoid the oval shape
        shape = CircleShape,
        border= BorderStroke(2.dp, colorResource(id = R.color.mid_grey)),
        contentPadding = PaddingValues(0.dp),  //avoid the little icon
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colorResource(id = R.color.transparent_grey),
            contentColor = colorResource(id = R.color.mid_grey)
        ),
        onClick = { }
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close area")
    }
}


@Composable
fun PulsatingWaveform() {
    // Define 4 independent Animatable heights
    val bars = List(4) { remember { Animatable(30f) } }
    val colors = listOf(light_grey, Color.White, Color.White, light_grey)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // Launch staggered animations for each bar
        bars.forEachIndexed { index, animatable ->
            scope.launch {
                while (true) {
                    animatable.animateTo(
                        targetValue = (10..30).random().toFloat(),
                        animationSpec = tween(durationMillis = 500)
                    )
                    animatable.animateTo(
                        targetValue = 80f,
                        animationSpec = tween(durationMillis = 500)
                    )
                }
            }
            // Delay between each bar animation start
            kotlinx.coroutines.delay(100L)
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val barWidth = 8.dp.toPx()
        val spacing = 4.dp.toPx()
        val totalWidth = (barWidth + spacing) * bars.size
        val cornerRadius = 4.dp.toPx()

        // Center the waveform horizontally
        val startX = (size.width - totalWidth + spacing) / 2

        bars.forEachIndexed { index, animatable ->
            val xOffset = startX + index * (barWidth + spacing)
            drawRoundRect(
                color = colors[index],
                topLeft = Offset(xOffset, size.height / 2 - animatable.value / 2),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
                size = androidx.compose.ui.geometry.Size(barWidth, animatable.value)
            )
        }
    }
}

// Extension function to convert Dp to Px
@Composable
fun androidx.compose.ui.unit.Dp.toPx(): Float {
    return with(LocalDensity.current) { this@toPx.toPx() }
}
