package com.ftrono.DJames.ui.overlay

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.MutableLiveData
import com.ftrono.DJames.R
import com.ftrono.DJames.application.autoStopQueriesState
import com.ftrono.DJames.application.clickAnimationCountdownTime
import com.ftrono.DJames.application.clickCounter
import com.ftrono.DJames.application.overlayOptionsStr
import com.ftrono.DJames.application.sourceIsVolume
import com.ftrono.DJames.be.models.QuickAction
import com.ftrono.DJames.ui.components.RoundedSign
import com.ftrono.DJames.ui.theme.light_grey
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.String
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin


@Preview
@Composable
fun PadsPreview1() {
    val overlayPosState by remember { mutableStateOf("Right") }
    val clickCounterState by remember { mutableStateOf(1) }
    val clockActiveState by remember { mutableStateOf(false) }
    DJamesPads(
        queryStatus = MutableLiveData<String>("ready"),
        overlayPosState = overlayPosState,
        clickCounterState = clickCounterState,
        clockActiveState = clockActiveState,
        currentTime = MutableLiveData<String>("00:00"),
    )
}

@Preview
@Composable
fun PadsPreview2() {
    val overlayPosState by remember { mutableStateOf("Right") }
    val clickCounterState by remember { mutableStateOf(2) }
    val clockActiveState by remember { mutableStateOf(false) }
    DJamesPads(
        queryStatus = MutableLiveData<String>("ready"),
        overlayPosState = overlayPosState,
        clickCounterState = clickCounterState,
        clockActiveState = clockActiveState,
        currentTime = MutableLiveData<String>("00:00"),
    )
}

@Preview
@Composable
fun PadsPreview3() {
    val overlayPosState by remember { mutableStateOf("Right") }
    val clickCounterState by remember { mutableStateOf(0) }
    val clockActiveState by remember { mutableStateOf(false) }
    DJamesPads(
        queryStatus = MutableLiveData<String>("ready"),
        overlayPosState = overlayPosState,
        clickCounterState = clickCounterState,
        clockActiveState = clockActiveState,
        currentTime = MutableLiveData<String>("00:00"),
    )
}


@Composable
fun DJamesPads(
    modifier: Modifier = Modifier,
    queryStatus: MutableLiveData<String>,
    overlayPosState: String,
    clickCounterState: Int,
    clockActiveState: Boolean,
    currentTime: MutableLiveData<String>,
    centerSize: Int = 100,
    toeSize: Int = 70,
    targetRadius: Dp = 40.dp,   // distance from center pad to toes
    interval: Float = 50f,   // distance between each toe angle
    onToesTapCommon: (Offset) -> Unit = { offset -> },
    onCenterTap: (Offset) -> Unit = { offset -> },
) {
    // Parameters & states:
    val mContext = LocalContext.current
    val currentTimeState by currentTime.observeAsState()
    val sourceIsVolumeState by sourceIsVolume.observeAsState()
    val overlayOptionsState by overlayOptionsStr.observeAsState()
    val overlayOptions = overlayOptionsState!!.split(", ")
    val autoStopQueriesState by autoStopQueriesState.observeAsState()

    // Colours:
    val colorBgActive = colorResource(R.color.colorAccentMid)
    val colorBgInactive = colorResource(R.color.faded_grey)
    val colorIconActive = colorResource(R.color.light_grey)
    val colorIconInactive = colorResource(R.color.light_grey)
    val colorCardActive = colorResource(R.color.colorPrimary)
    val colorTimeoutActive = colorResource(R.color.light_grey)

    // Animate radius based on expansion state:
    val animatedRadius by animateDpAsState(
        targetValue = if (clickCounterState > 0) targetRadius else 0.dp,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "toeRadius"
    )

    // Bounding box:
    Box(
        modifier = if (clickCounterState > 0) {
            modifier
                .padding(
                    start = if (overlayPosState == "Right") 0.dp else 4.dp,
                    end = if (overlayPosState == "Right") 4.dp else 0.dp,
                )
                .width(
                    if (overlayPosState == "Right" && sourceIsVolumeState!!) 370.dp else 300.dp
                )
                .height(300.dp)
        } else modifier,
        contentAlignment = if (overlayPosState == "Right") Alignment.CenterEnd else Alignment.CenterStart,
    ) {

        // TOES PADS:
        // Calculate toes position (min 2 toes, max 5 toes):
        var angles = getToesPositions(
            size = overlayOptions.size,
            interval = interval,
            posRight = overlayPosState == "Right"
        )

        // Place 4 toes along a semi-circle on the left:
        angles.forEachIndexed { index, angle ->
            val rad = Math.toRadians(angle.toDouble())
            val x = (cos(rad) * animatedRadius.toPx()).dp
            val y = (sin(rad) * animatedRadius.toPx()).dp

            AnimatedVisibility(
                visible = clickCounterState > 0,
                 enter = fadeIn(
                     animationSpec = tween(durationMillis = 250)
                 ) + slideIn(
                     animationSpec = tween(durationMillis = 250),
                     initialOffset = { IntOffset(x = 0, y = 0) }
                 ),
                 exit = fadeOut(
                     animationSpec = tween(durationMillis = 250)
                 ) + slideOut(
                     animationSpec = tween(durationMillis = 250),
                     targetOffset = { IntOffset(x = 0, y = 0) }
                 )
            ) {

                // BUTTONS:
                val isState = index + 2
                val isActive = clickCounterState == isState
                val curActionName = overlayOptions[index]
                val curAction = getQuickAction(
                    name = curActionName,
                    isActive = isActive,
                    colorActive = colorIconActive,
                    colorInactive = colorIconInactive,
                    currentTimeState = currentTimeState!!,
                    autoStopQueriesState = autoStopQueriesState!!,
                )

                Row(
                    modifier = Modifier
                        .absoluteOffset(x = x, y = y),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                )
                {
                    if (isActive && overlayPosState == "Right") {
                        QuickActionDescription(
                            curAction = curAction,
                            backgroundColor = colorCardActive,
                            textColor = colorIconInactive,
                            posRight = true,
                        )
                    }

                    TimeoutButton (
                        modifier = Modifier
                            .zIndex(1f),
                        isActive = isActive,
                        backgroundColor = if (isActive) colorBgActive else colorBgInactive,
                        timeoutColor = colorTimeoutActive,
                        bubbleSize = toeSize.dp,
                        timeoutWidth = 7.dp,
                        onTap = {
                            clickCounter.postValue(isState)
                            onToesTapCommon(it)
                        }
                    ) {
                        curAction.content()
                    }

                    if (isActive && overlayPosState == "Left") {
                        QuickActionDescription(
                            curAction = curAction,
                            backgroundColor = colorCardActive,
                            textColor = colorIconInactive,
                            posRight = false,
                        )
                    }
                }
            }
        }

        // CENTER PAW PAD:
        CenterPad(
            bubbleSize = centerSize,
            queryStatus = queryStatus,
            clickCounterState = clickCounterState,
            clockActiveState = clockActiveState,
            currentTimeState = currentTimeState!!,
            colorIconActive = colorIconActive,
            colorIconInactive = colorIconInactive,
            colorBgActive = colorBgActive,
            colorBgInactive = colorBgInactive,
            colorTimeout = colorTimeoutActive,
            onClockTap = {
                getQuickActionOnTap(context = mContext, name = "clock")()
            },
            onCenterTap = {
                onCenterTap(it)
            }
        )
    }
}


@Composable
fun QuickActionDescription(
    curAction: QuickAction,
    backgroundColor: Color,
    textColor: Color,
    posRight: Boolean
) {
    Card(
        modifier = Modifier
            .offset(x = if (posRight) 6.dp else (-6).dp),
        shape = RoundedCornerShape(
            topStart = if (posRight) 20.dp else 0.dp,
            bottomStart = if (posRight) 20.dp else 0.dp,
            topEnd = if (posRight) 0.dp else 20.dp,
            bottomEnd = if (posRight) 0.dp else 20.dp,
        ),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Text(
            modifier = Modifier
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    top = 8.dp,
                    bottom = 8.dp
                ),
            text = curAction.description.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = textColor
        )
    }
}


@Composable
fun TimeoutButton(
    modifier: Modifier = Modifier,
    isActive: Boolean,
    bubbleSize: Dp = 100.dp,
    timeoutWidth: Dp = 8.dp,
    backgroundColor: Color,
    timeoutColor: Color,
    isCenter: Boolean = false,
    onTap: (Offset) -> Unit = { offset -> },
    icon: @Composable () -> Unit = {}
) {
    // States:
    val sweepAngle = remember { Animatable(360f) }
    val scope = rememberCoroutineScope()
    var countdownJob by remember { mutableStateOf<Job?>(null) }

    // React to external isRunning changes
    LaunchedEffect(isActive) {
        if (isActive) {
            countdownJob?.cancel()

            countdownJob = scope.launch {
                sweepAngle.snapTo(360f)

                sweepAngle.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = clickAnimationCountdownTime,
                        easing = LinearEasing
                    )
                )
            }
        } else {
            // Cancel if set to false externally
            countdownJob?.cancel()
            countdownJob = null
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(if (!isCenter && isActive) bubbleSize + 10.dp else bubbleSize)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onTap(it)
                    }
                )
            }
    ) {
        // Background circle
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(color = backgroundColor)
        }

        // Countdown arc
        if (isActive) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawArc(
                    color = timeoutColor,
                    startAngle = -90f,
                    sweepAngle = -sweepAngle.value,
                    useCenter = false,
                    style = Stroke(width = timeoutWidth.toPx(), cap = StrokeCap.Round)
                )
            }
        }
        icon()
    }
}


@Composable
fun CenterPad(
    bubbleSize: Int,
    queryStatus: MutableLiveData<String>,
    clickCounterState: Int,
    clockActiveState: Boolean,
    currentTimeState: String,
    colorIconActive: Color,
    colorIconInactive: Color,
    colorBgActive: Color,
    colorBgInactive: Color,
    colorTimeout: Color,
    onClockTap: (Offset) -> Unit,
    onCenterTap: (Offset) -> Unit
) {
    val queryState by queryStatus.observeAsState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {

        //CLOCK BUTTON:
        if (!clockActiveState && clickCounterState == 0) {
            ClockButton(
                bubbleSize = bubbleSize,
                currentTimeState = currentTimeState,
                onTap = onClockTap,
            )
        }

        // CENTER PAW PAD:
        TimeoutButton(
            isActive = clickCounterState == 1,
            bubbleSize = bubbleSize.dp,
            isCenter = true,
            timeoutWidth = 8.dp,
            backgroundColor = if (clickCounterState == 1) {
                colorBgActive   // Center pad active
            } else if (clickCounterState > 0) {
                colorBgInactive   // Toes active
            } else {
                when (queryState) {
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
            },
            timeoutColor = colorTimeout,
            onTap = {
//            if (clickCounterState != 1) {
//                clickCounter.postValue(1)
//            } else {
//                clickCounter.postValue(0)
//            }
                onCenterTap(it)
            }
        ) {
            if (clickCounterState > 0) {
                // EXPANDED:
                Icon(
                    modifier = Modifier
                        .size(50.dp),
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel",
                    tint = if (clickCounterState == 1) colorIconActive else colorIconInactive,
                )
            } else if (queryState == "processing") {
                //PROCESSING:
                CircularProgressIndicator(
                    modifier = Modifier.width(40.dp),
                    color = colorResource(id = R.color.light_grey),
                    trackColor = colorResource(id = R.color.dark_grey),
                    strokeWidth = 8.dp
                )
            } else if (queryState == "busy") {
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
fun ClockButton(
    bubbleSize: Int,
    currentTimeState: String,
    onTap: (Offset) -> Unit
) {
    //OVERLAY BUTTON:
    Card(
        modifier = Modifier
            .padding(bottom = 8.dp)
            .width(round(bubbleSize.toDouble() / 1.5).dp)
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
                text = currentTimeState,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = colorResource(id = R.color.light_grey)
            )
            Text (
                modifier = Modifier
                    .padding(bottom=8.dp),
                text = "CLOCK",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = colorResource(id = R.color.mid_grey)
            )
        }
    }

}


@Preview
@Composable
fun OverlayClose(
) {
    RoundedSign(
        modifier = Modifier
            .padding(bottom = 25.dp)
            .zIndex(1f),  //avoid the oval shape
        signSize = 60.dp,
        contentSize = 40,
        backgroundColor = colorResource(R.color.transparent_grey),
        borderColor = colorResource(id = R.color.light_grey),
        contentColor = colorResource(id = R.color.light_grey),
        borderWidth = 2.5.dp,
        iconPainter = painterResource(R.drawable.arrow_down),
    )
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


@Composable
fun TypingIndicator(
    dotSize: Dp = 8.dp,
    dotColor: Color = Color.Gray,
    spaceBetween: Dp = 6.dp,
    animationDelay: Int = 1000
) {
    val dotCount = 3
    val infiniteTransition = rememberInfiniteTransition()

    val animations = List(dotCount) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = animationDelay * dotCount
                    0.3f at (index * animationDelay)
                    1f at (index * animationDelay + animationDelay / 2)
                    0.3f at (index * animationDelay + animationDelay)
                },
                repeatMode = RepeatMode.Restart
            )
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(spaceBetween),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
    ) {
        animations.forEach { anim ->
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .scale(anim.value)
                    .alpha(anim.value)
                    .background(color = dotColor, shape = CircleShape)
            )
        }
    }
}

