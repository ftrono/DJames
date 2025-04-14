package com.ftrono.DJames.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.MutableLiveData
import com.ftrono.DJames.R
import com.ftrono.DJames.application.autoStopQueriesState
import com.ftrono.DJames.ui.theme.light_grey
import kotlinx.coroutines.delay
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
        clickCounterState = 2,
        onTap = {offset -> }
    )
}



@Composable
fun DJamesButton(
    bubbleSize: Int,
    overlayStatus: MutableLiveData<String>,
    clickCounterState: Int,
    onTap: (Offset) -> Unit
) {
    val overlayState by overlayStatus.observeAsState()
    val autoStopQueriesState by autoStopQueriesState.observeAsState()
    //OVERLAY BUTTON:
    Box(
        modifier = Modifier
            .size(bubbleSize.dp)
            .clip(CircleShape)
            .background(
                color = if (clickCounterState > 0) {
                    colorResource(id = R.color.greenSignLight)
                } else {
                    when (overlayState) {
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
            if (clickCounterState > 0) {
                //COUNTER TRACKER:
                when (clickCounterState) {
                    1 -> Icon(
                        modifier = Modifier
                            .size(50.dp),
                        painter = painterResource(id = R.drawable.icon_speak),
                        tint = colorResource(id = R.color.colorPrimaryDark),
                        contentDescription = "Voice request"
                    )
                    2 -> Row (
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            modifier = Modifier
                                .offset(x=6.dp)
                                .size(40.dp),
                            painter = painterResource(R.drawable.logo_spotify),
                            tint = colorResource(id = R.color.colorPrimaryDark),
                            contentDescription = "Save track to Spotify"
                        )
                        Icon(
                            modifier = Modifier
                                .offset(x=0.dp, y=14.dp)
                                .size(34.dp),
                            imageVector = Icons.Default.Add,
                            tint = colorResource(id = R.color.colorPrimaryDark),
                            contentDescription = "Save track to Spotify"
                        )
                    }
                    3 -> Icon(
                        modifier = Modifier
                            .size(50.dp)
                            .graphicsLayer {
                                rotationY = if (autoStopQueriesState!!) 0f else 180f
                            },
                        painter = if (autoStopQueriesState!!) painterResource(id = R.drawable.icon_hearing_off) else painterResource(id = R.drawable.icon_hearing),
                        tint = colorResource(id = R.color.colorPrimaryDark),
                        contentDescription = "Voice request"
                    )
                    else -> Icon(
                        modifier = Modifier
                            .size(50.dp),
                        imageVector = Icons.Default.Close,
                        tint = colorResource(id = R.color.colorPrimaryDark),
                        contentDescription = "Voice request"
                    )
                }
            } else if (overlayState == "processing") {
                //PROCESSING:
                CircularProgressIndicator(
                    modifier = Modifier.width(40.dp),
                    color = colorResource(id = R.color.light_grey),
                    trackColor = colorResource(id = R.color.dark_grey),
                    strokeWidth = 8.dp
                )
            } else if (overlayState == "busy") {
                //BUSY:
                PulsatingWaveform()
            } else {
                //READY:
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
fun Row(content: @Composable () -> Unit) {
    TODO("Not yet implemented")
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
            delay(100L)
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
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                size = Size(barWidth, animatable.value)
            )
        }
    }
}

// Extension function to convert Dp to Px
@Composable
fun Dp.toPx(): Float {
    return with(LocalDensity.current) { this@toPx.toPx() }
}
