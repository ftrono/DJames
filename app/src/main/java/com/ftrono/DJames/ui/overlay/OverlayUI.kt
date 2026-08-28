package com.ftrono.DJames.ui.overlay

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.MutableLiveData
import com.ftrono.DJames.R
import com.ftrono.DJames.application.ACTION_REC_STOP
import com.ftrono.DJames.application.clickAnimationCountdownTime
import com.ftrono.DJames.application.clickCounter
import com.ftrono.DJames.application.clockActive
import com.ftrono.DJames.application.currentHourMini
import com.ftrono.DJames.application.currentMinsMini
import com.ftrono.DJames.application.overlayBoxMax
import com.ftrono.DJames.application.overlayBoxMin
import com.ftrono.DJames.application.overlayBubbleSize
import com.ftrono.DJames.application.overlayOptionsStr
import com.ftrono.DJames.application.overlayTimeoutCenterWidth
import com.ftrono.DJames.application.overlayTimeoutToeWidth
import com.ftrono.DJames.application.overlayToeSize
import com.ftrono.DJames.application.raiseVolumeCountdownTime
import com.ftrono.DJames.application.isVolumeUpPreferenceSet
import com.ftrono.DJames.application.isVolumeUpUnlocked
import com.ftrono.DJames.application.overlayDocked
import com.ftrono.DJames.application.overlayPos
import com.ftrono.DJames.application.queryStatus
import com.ftrono.DJames.application.recordingMode
import com.ftrono.DJames.application.voiceQueryOn
import com.ftrono.DJames.ui.components.RoundedSign
import com.ftrono.DJames.ui.components.toPx
import com.ftrono.DJames.ui.theme.light_grey
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.String
import kotlin.math.cos
import kotlin.math.sin


// MAIN OverlayBubble UI wrapper:
@Composable
fun OverlayBubble(
    context: Context,
    overlay: Overlay,
    centerSize: Int = overlayBubbleSize,
    toeSize: Int = overlayToeSize,
    modifier: Modifier,
    preview: Boolean = false,
) {
    // States:
    val clockActiveState by clockActive.observeAsState()
    val clickCounterState by clickCounter.observeAsState()
    val isVolumeUpUnlockedState by isVolumeUpUnlocked.observeAsState()
    val overlayPosState by overlayPos.observeAsState()

    //CONTAINER:
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // MAIN:
        DJamesPads(
            context = context,
            queryStatus = queryStatus,
            overlayPosState = overlayPosState!!,
            clickCounterState = clickCounterState!!,
            clockActiveState = clockActiveState!!,
            centerSize = centerSize,
            toeSize = toeSize,
            onToesTapCommon = {
                if (!preview) overlay.onToesPadClick(context)
            },
            onCenterTap = {
                if (!preview) {
                    if (isVolumeUpUnlockedState!!) {
                        // Re-enable volume-up trigger:
                        isVolumeUpUnlocked.postValue(false)
                    } else {
                        if (!voiceQueryOn) {
                            // CENTER TAP:
                            overlay.onCenterPadClick(
                                context = context,
                                enable = clickCounterState == 0,
                            )
                        } else if (recordingMode) {
                            //EARLY STOP RECORDING:
                            Intent().also { intent ->
                                intent.setAction(ACTION_REC_STOP)
                                context.sendBroadcast(intent)
                            }
                        }
                    }
                }
            }
        )
    }
}


@Preview
@Composable
fun PadsPreview1() {
    val mContext = LocalContext.current
    val overlayPosState by remember { mutableStateOf("Right") }
    val clickCounterState by remember { mutableStateOf(1) }
    val clockActiveState by remember { mutableStateOf(false) }

    DJamesPads(
        context = mContext,
        queryStatus = MutableLiveData<String>("ready"),
        overlayPosState = overlayPosState,
        clickCounterState = clickCounterState,
        clockActiveState = clockActiveState,
    )
}

@Preview
@Composable
fun PadsPreview2() {
    val mContext = LocalContext.current
    val overlayPosState by remember { mutableStateOf("Left") }
    val clickCounterState by remember { mutableStateOf(2) }
    val clockActiveState by remember { mutableStateOf(false) }

    DJamesPads(
        context = mContext,
        queryStatus = MutableLiveData<String>("ready"),
        overlayPosState = overlayPosState,
        clickCounterState = clickCounterState,
        clockActiveState = clockActiveState,
    )
}

@Preview
@Composable
fun PadsPreview3() {
    val mContext = LocalContext.current
    val overlayPosState by remember { mutableStateOf("Right") }
    val clickCounterState by remember { mutableStateOf(2) }
    val clockActiveState by remember { mutableStateOf(false) }

    DJamesPads(
        context = mContext,
        queryStatus = MutableLiveData<String>("ready"),
        previewDocked = true,
        overlayPosState = overlayPosState,
        clickCounterState = clickCounterState,
        clockActiveState = clockActiveState,
    )
}

@Preview
@Composable
fun PadsPreview4() {
    val mContext = LocalContext.current
    val overlayPosState by remember { mutableStateOf("Right") }
    val clickCounterState by remember { mutableStateOf(0) }
    val clockActiveState by remember { mutableStateOf(false) }

    DJamesPads(
        context = mContext,
        queryStatus = MutableLiveData<String>("ready"),
        overlayPosState = overlayPosState,
        clickCounterState = clickCounterState,
        clockActiveState = clockActiveState,
    )
}

@Preview
@Composable
fun PadsPreview5() {
    val mContext = LocalContext.current
    val overlayPosState by remember { mutableStateOf("Right") }
    val clickCounterState by remember { mutableStateOf(0) }
    val clockActiveState by remember { mutableStateOf(false) }

    DJamesPads(
        context = mContext,
        queryStatus = MutableLiveData<String>("ready"),
        overlayPosState = overlayPosState,
        clickCounterState = clickCounterState,
        clockActiveState = clockActiveState,
        previewVolume = true,
    )
}

@Preview
@Composable
fun PadsPreview6() {
    val mContext = LocalContext.current
    val overlayPosState by remember { mutableStateOf("Right") }
    val clickCounterState by remember { mutableStateOf(0) }
    val clockActiveState by remember { mutableStateOf(false) }

    DJamesPads(
        context = mContext,
        queryStatus = MutableLiveData<String>("ready"),
        overlayPosState = overlayPosState,
        clickCounterState = clickCounterState,
        clockActiveState = clockActiveState,
        previewDocked = true,
    )
}


@Composable
fun DJamesPads(
    context: Context,
    modifier: Modifier = Modifier,
    queryStatus: MutableLiveData<String>,
    overlayPosState: String,
    clickCounterState: Int,
    clockActiveState: Boolean,
    centerSize: Int = overlayBubbleSize,
    toeSize: Int = overlayToeSize,
    targetRadius: Dp = 44.dp,   // distance from center pad to toes
    interval: Float = 50f,   // distance between each toe angle
    previewVolume: Boolean = false,
    previewDocked: Boolean = false,
    onToesTapCommon: (Offset) -> Unit = { offset -> },
    onCenterTap: (Offset) -> Unit = { offset -> },
) {
    // Parameters & states:
    val mContext = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape by remember { mutableStateOf(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) }

    val currentHourState by currentHourMini.observeAsState()
    val currentMinsState by currentMinsMini.observeAsState()
    val overlayOptionsState by overlayOptionsStr.observeAsState()
    val isDocked by overlayDocked.observeAsState()

    // Colours:
    val colorBgActive = colorResource(R.color.colorAccentMid)
    val colorBgInactive = colorResource(R.color.dark_grey)
    val colorTimeoutActive = colorResource(R.color.light_grey)

    // Animate radius based on expansion state:
    val animatedRadius by animateDpAsState(
        targetValue = if (clickCounterState > 0) targetRadius else 0.dp,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "toeRadius"
    )

    LaunchedEffect(clockActiveState, isLandscape) {
        if ((isDocked!! || previewDocked) && isLandscape) {
            overlayOptionsStr.postValue("speak, save, volume, pos")
        } else if (isDocked!! || previewDocked || clockActiveState) {
            overlayOptionsStr.postValue("speak, save")
        } else {
            overlayOptionsStr.postValue("speak, save, clock")
        }
    }

    // Bounding box:
    Box(
        modifier = if (isDocked!! || previewDocked) {
            modifier
                .fillMaxWidth()
                .height(if (clickCounterState > 0) overlayBoxMin.dp else centerSize.dp)
        } else if (clickCounterState > 0) {
            modifier
                .padding(
                    start = if (overlayPosState == "Right") 0.dp else 4.dp,
                    end = if (overlayPosState == "Right") 4.dp else 0.dp,
                )
                .width(overlayBoxMin.dp)
                .height(overlayBoxMax.dp)
        } else modifier,
        contentAlignment = if ((isDocked!! || previewDocked) && !isLandscape) {
            Alignment.BottomCenter
        } else if (overlayPosState == "Right") {
            Alignment.CenterEnd
        } else {
            Alignment.CenterStart
        },
    ) {

        // TOES PADS:
        // Calculate toes position (min 2 toes, max 5 toes):
        val overlayOptions = overlayOptionsState!!.split(", ")
        var angles = getToesPositions(
            size = overlayOptions.size,
            interval = interval,
            posRight = overlayPosState == "Right",
            bottomDocked = (isDocked!! || previewDocked) && !isLandscape,
        )

        // Place N toes along a semi-circle on the left:
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
                    currentHourState = currentHourState!!,
                    currentMinsState = currentMinsState!!,
                )

                Row(
                    modifier = Modifier
                        .absoluteOffset(x = x, y = y),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                )
                {
                    TimeoutButton (
                        modifier = Modifier
                            .zIndex(1f),
                        isActive = isActive,
                        title = curAction.title,
                        backgroundColor = if (isActive) colorBgActive else colorBgInactive,
                        itemColor = colorResource(R.color.light_grey),
                        timeoutColor = colorTimeoutActive,
                        bubbleSize = toeSize.dp,
                        timeoutWidth = overlayTimeoutToeWidth.dp,
                        onTap = {
                            clickCounter.postValue(isState)
                            onToesTapCommon(it)
                        }
                    ) {
                        curAction.content()
                    }
                }
            }
        }

        // CENTER PAW PAD:
        if (isDocked!! || previewDocked) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                CenterPad(
                    context = context,
                    bubbleSize = centerSize,
                    toeSize = toeSize + 4,
                    isDocked = true,
                    queryStatus = queryStatus,
                    clickCounterState = clickCounterState,
                    clockActiveState = clockActiveState,
                    currentHourState = currentHourState!!,
                    currentMinsState = currentMinsState!!,
                    colorBgActive = colorBgActive,
                    colorBgInactive = colorBgInactive,
                    colorTimeout = colorTimeoutActive,
                    previewVolume = previewVolume,
                    onCenterTap = {
                        onCenterTap(it)
                    }
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CenterPad(
                    context = context,
                    bubbleSize = centerSize,
                    toeSize = toeSize + 4,
                    isDocked = false,
                    queryStatus = queryStatus,
                    clickCounterState = clickCounterState,
                    clockActiveState = clockActiveState,
                    currentHourState = currentHourState!!,
                    currentMinsState = currentMinsState!!,
                    colorBgActive = colorBgActive,
                    colorBgInactive = colorBgInactive,
                    colorTimeout = colorTimeoutActive,
                    previewVolume = previewVolume,
                    onCenterTap = {
                        onCenterTap(it)
                    }
                )
            }
        }
    }
}


@Composable
fun TimeoutButton(
    modifier: Modifier = Modifier,
    isActive: Boolean,
    isCircle: Boolean = false,
    title: String = "",
    bubbleSize: Dp = overlayBubbleSize.dp,
    timeoutWidth: Dp = overlayTimeoutCenterWidth.dp,
    backgroundColor: Color,
    itemColor: Color? = null,
    timeoutColor: Color,
    timeoutMs: Int = clickAnimationCountdownTime,
    onTap: (Offset) -> Unit = { offset -> },
    onTimeout: () -> Unit = {},
    cornerRadius: Dp = 20.dp,
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
                        durationMillis = timeoutMs,
                        easing = LinearEasing
                    )
                )

                onTimeout()
            }
        } else {
            // Cancel if set to false externally
            countdownJob?.cancel()
            countdownJob = null
            onTimeout()
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(bubbleSize)
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
            if (isCircle) {
                drawCircle(color = backgroundColor)
            } else {
                drawRoundRect(
                    color = backgroundColor,
                    cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                )
            }
        }

        // Countdown arc
        if (isActive) {
            Canvas(
                modifier = if (isCircle) {
                    Modifier
                        .fillMaxSize()
                        .padding(5.dp)
                } else {
                    Modifier.matchParentSize()
                }
            ) {
                if (isCircle) {
                    // Circle arc:
                    drawArc(
                        color = timeoutColor,
                        startAngle = -90f,
                        sweepAngle = -sweepAngle.value,
                        useCenter = false,
                        style = Stroke(width = timeoutWidth.toPx(), cap = StrokeCap.Round)
                    )

                } else {
                    // RoundedRect arc:
                    val strokeWidth = timeoutWidth.toPx()
                    val inset = strokeWidth / 2f

                    val rect = Rect(
                        left = inset,
                        top = inset,
                        right = size.width - inset,
                        bottom = size.height - inset
                    )

                    val radius = cornerRadius.toPx().coerceAtMost(
                        minOf(rect.width, rect.height) / 2f
                    )

                    val path = Path().apply {
                        addRoundRect(
                            RoundRect(
                                rect = rect,
                                cornerRadius = CornerRadius(radius, radius)
                            )
                        )
                    }

                    val pathMeasure = PathMeasure()
                    pathMeasure.setPath(path, true)

                    val progressPath = Path()
                    val progress = sweepAngle.value / 360f
                    val length = pathMeasure.length * progress

                    pathMeasure.getSegment(
                        startDistance = 0f,
                        stopDistance = length,
                        destination = progressPath,
                        startWithMoveTo = true
                    )

                    drawPath(
                        path = progressPath,
                        color = timeoutColor,
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }

        // Item visuals:
        Column(
            modifier = Modifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            icon()
            if (title != "" && title.lowercase() != "clock" && itemColor != null) {
                Text(
                    modifier = Modifier
                        .padding(top=6.dp),
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = itemColor
                )
            }
        }
    }
}


@Composable
fun CenterPad(
    context: Context,
    bubbleSize: Int,
    toeSize: Int,
    isDocked: Boolean = false,
    queryStatus: MutableLiveData<String>,
    clickCounterState: Int,
    clockActiveState: Boolean,
    currentHourState: String,
    currentMinsState: String,
    colorBgActive: Color,
    colorBgInactive: Color,
    colorTimeout: Color,
    onCenterTap: (Offset) -> Unit,
    previewVolume: Boolean = false,
) {
    val verticalPad = 8.dp
    val horizontalPad = 20.dp
    val queryState by queryStatus.observeAsState()
    val isVolumeUpPreferenceSetState by isVolumeUpPreferenceSet.observeAsState()
    val isVolumeUpUnlockedState by isVolumeUpUnlocked.observeAsState()

    // Actions:
    val clockAction = getQuickAction(
        name = "clock",
        currentHourState = currentHourState,
        currentMinsState = currentMinsState,
    )
    val volumeAction = getQuickAction(
        name = "volume",
    )
    val undockAction = getQuickAction(
        name = "undock",
    )

    LaunchedEffect(queryState) {
        if (queryState == "busy") {
            Toast.makeText(context, "Speak now!", Toast.LENGTH_SHORT).show()
        }
    }

    //CLOCK BUTTON:
    if ((!clockActiveState && clickCounterState == 0)) {
        FixedButton(
            modifier = if (isDocked) {
                Modifier
                    .padding(end=horizontalPad)
            } else {
                Modifier
                    .padding(bottom=verticalPad)
            },
            size = toeSize,
            backgroundColor = colorResource(R.color.black),
            // showBorder = isDocked,
            onTap = {
                getQuickActionOnTap(context = context, name = "clock")()
            },
        ) {
            clockAction.content()
        }
    } else if (isDocked) {
        if (clockActiveState && clickCounterState == 0) {
            FixedButton(
                modifier = Modifier
                    .padding(end=horizontalPad),
                size = toeSize,
                backgroundColor = colorResource(R.color.dark_grey),
                onTap = {
                    getQuickActionOnTap(context = context, name = "undock")()
                },
            ){
                undockAction.content()
            }
        } else {
            // Placeholder:
            Box(
                modifier = Modifier
                    .padding(end = horizontalPad)
                    .size((toeSize - 10).dp)
                    .background(colorResource(R.color.transparent_full))
            )
        }
    }

    // CENTER PAW PAD:
    TimeoutButton(
        modifier = Modifier
            .zIndex(1f),
        isActive = (clickCounterState == 1 || isVolumeUpUnlockedState!! || previewVolume),
        isCircle = true,
        bubbleSize = bubbleSize.dp,
        timeoutWidth = overlayTimeoutCenterWidth.dp,
        backgroundColor = if (clickCounterState == 1) {
            colorBgActive   // Center pad active
        } else if (clickCounterState > 0) {
            colorBgInactive   // Toes active
        } else {
            when {
                (isVolumeUpUnlockedState!! || previewVolume) -> {
                    colorResource(id = R.color.dark_grey)
                }

                (queryState == "busy") -> {
                    colorResource(id = R.color.colorBusy)
                }

                (queryState == "processing") -> {
                    colorResource(id = R.color.faded_grey)
                }

                else -> {
                    colorResource(id = R.color.colorPrimary)
                }
            }
        },
        timeoutMs = if (isVolumeUpUnlockedState!!) raiseVolumeCountdownTime else clickAnimationCountdownTime,
        timeoutColor = colorTimeout,
        onTap = { onCenterTap(it) },
        onTimeout = {
            if (isVolumeUpUnlockedState!!) {
                // Re-enable volume-up trigger:
                isVolumeUpUnlocked.postValue(false)
            }
        }
    ) {
        Column(
            modifier = Modifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (clickCounterState > 0) {
                // EXPANDED:
                Icon(
                    modifier = Modifier
                        .size(50.dp),
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel",
                    tint = colorResource(R.color.light_grey),
                )

            } else if (isVolumeUpUnlockedState!! || previewVolume) {
                // RAISE VOLUME:
                Text (
                    modifier = Modifier,
                    text = buildAnnotatedString {
                        append("RAISE\nVOLUME\n")
                        withStyle(SpanStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )) {
                            append("NOW")
                        }
                    },
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    color = colorResource(id = R.color.light_grey)
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

    //VOLUME BUTTON:
    if (isVolumeUpPreferenceSetState!! && clickCounterState == 0 && !isVolumeUpUnlockedState!!) {
        FixedButton(
            modifier = if (isDocked) {
                Modifier
                    .padding(start=horizontalPad)
            } else {
                Modifier
                    .padding(top=verticalPad)
            },
            size = toeSize,
            backgroundColor = colorResource(R.color.dark_grey),
            onTap = {
                getQuickActionOnTap(context = context, name = "volume")()
            },
        ) {
            volumeAction.content()
        }
    } else if (isDocked) {
        // Placeholder:
        Box(
            modifier = Modifier
                .padding(start = horizontalPad)
                .size((toeSize - 10).dp)
                .background(colorResource(R.color.transparent_full))
        )
    }
}


@Composable
fun FixedButton(
    modifier: Modifier,
    size: Int = overlayToeSize,
    onTap: (Offset) -> Unit,
    backgroundColor: Color,
    showBorder: Boolean = false,
    content: @Composable () -> Unit = {}
) {
    //OVERLAY BUTTON:
    Card(
        modifier = modifier
            .size((size - 10).dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    //ON SINGLE TAP:
                    onTap = onTap
                )
            },
        // border = BorderStroke(1.dp, colorResource(id = R.color.faded_grey)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors (
            containerColor = backgroundColor
        ),
        border = if (showBorder) {
            BorderStroke(0.5.dp, colorResource(id = R.color.dark_grey_background))
        } else null,
    ) {
        content()
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
        signSize = 80.dp,
        contentSize = 40,
        backgroundColor = colorResource(R.color.greenSignLight),
        borderColor = colorResource(id = R.color.greenSignLight),
        contentColor = colorResource(id = R.color.colorPrimaryDark),
        borderWidth = 2.5.dp,
        iconVector = Icons.Default.Close,
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