package com.example.readproplus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

private val DarkTeal = Color(0xFF006064)
private val BlueAccent = Color(0xFF4FC3F7)
private val DialogBg = Color(0xFF1B1B2F)
private val CardBg = Color(0xFF272744)

@Composable
fun TtsDurationDialog(
    mainTextOnly: Boolean,
    onMainTextOnlyChange: (Boolean) -> Unit,
    onGenerate: (minutes: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedMinutes by remember { mutableIntStateOf(1) }
    val durationOptions = listOf(
        1 to "1 Minute",
        5 to "5 Minutes",
        10 to "10 Minutes",
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DialogBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                Text(
                    text = "Generate Kokoro Audio",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Audio generation will start at the top of the text you can see.",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                Spacer(Modifier.height(12.dp))

                durationOptions.forEach { (minutes, label) ->
                    val isSelected = selectedMinutes == minutes
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(
                                color = if (isSelected) CardBg else Color.Transparent,
                                shape = RoundedCornerShape(10.dp),
                            )
                            .clickable { selectedMinutes = minutes }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedMinutes = minutes },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = BlueAccent,
                                unselectedColor = Color.White.copy(alpha = 0.6f),
                            ),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = label,
                            color = if (isSelected) BlueAccent else Color.White,
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMainTextOnlyChange(!mainTextOnly) }
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Main text only",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Skip page numbers, repeated headers and footers, captions, and standalone links.",
                            color = Color.White.copy(alpha = 0.68f),
                            fontSize = 12.sp,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = mainTextOnly,
                        onCheckedChange = onMainTextOnlyChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = BlueAccent,
                        ),
                    )
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.4f))
                        ),
                    ) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.8f))
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = { onGenerate(selectedMinutes) },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkTeal),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("Generate", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
