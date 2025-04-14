package com.ftrono.DJames.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ftrono.DJames.R


// STREET UI GENERAL DIALOG
@Composable
fun GeneralDialog(
    dialogOnState: MutableState<Boolean>,
    backgroundColor: Color,
    title: String,
    content:  @Composable() (ColumnScope.() -> Unit) = {},
    dismissText: String = "",
    onDismiss: () -> Unit = { dialogOnState.value = false },
    confirmText: String = "",
    onConfirm: () -> Unit = {}
) {
    if (dialogOnState.value) {
        Dialog(
            onDismissRequest = {
                onDismiss()
            }
        ) {
            //CONTAINER:
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = backgroundColor
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .wrapContentWidth()
                        .wrapContentHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start,
                ) {
                    //TITLE:
                    Text(
                        text = title,
                        modifier = Modifier.padding(top=8.dp, bottom=16.dp),
                        color = colorResource(id = R.color.light_grey),
                        textAlign = TextAlign.Start,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    //CONTENT:
                    content()

                    //BUTTONS ROW:
                    Row(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        if (dismissText != "") {
                            AssistChip(
                                border = BorderStroke(1.dp, backgroundColor),
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = backgroundColor
                                ),
                                label = {
                                    Text(
                                        text = dismissText,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colorResource(id = R.color.light_grey)
                                    )
                                },
                                onClick = {
                                    onDismiss()
                                }
                            )
                        }
                        if (confirmText != "") {
                            AssistChip(
                                border = BorderStroke(1.dp, backgroundColor),
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = backgroundColor
                                ),
                                label = {
                                    Text(
                                        text = confirmText,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colorResource(id = R.color.light_grey)
                                    )
                                },
                                onClick = {
                                    onConfirm()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}