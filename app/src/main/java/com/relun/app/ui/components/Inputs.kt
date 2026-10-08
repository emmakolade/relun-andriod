package com.relun.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relun.app.ui.theme.Outfit
import com.relun.app.ui.theme.RelunColors

/**
 * The one text input: 54dp, 16dp corners, grey border that turns ink on focus
 * and red on error.
 */
@Composable
fun RelunTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isError: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    textAlign: TextAlign = TextAlign.Start,
    minHeight: Dp = 54.dp,
    fill: Color = Color.White,
    radius: Dp = 16.dp,
    textStyle: TextStyle = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Medium, fontSize = 17.sp),
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val border = when {
        isError -> RelunColors.Error
        focused -> RelunColors.Ink
        else -> RelunColors.Border
    }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interaction,
        cursorBrush = SolidColor(RelunColors.Ink),
        textStyle = textStyle.copy(color = RelunColors.Ink, textAlign = textAlign),
        modifier = modifier,
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .background(fill, RoundedCornerShape(radius))
                    .border(1.5.dp, border, RoundedCornerShape(radius))
                    .padding(horizontal = 16.dp, vertical = if (singleLine) 0.dp else 14.dp),
                verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                leading?.invoke()
                Box(Modifier.weight(1f), contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            style = textStyle.copy(color = RelunColors.Faint, fontWeight = FontWeight.Normal, textAlign = textAlign),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    inner()
                }
                trailing?.invoke()
            }
        },
    )
}

@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = RelunColors.Ink)
        if (trailing != null) Text(trailing, style = MaterialTheme.typography.bodySmall, color = RelunColors.Muted)
    }
}

@Composable
fun ErrorLine(text: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Rounded.Error, null, tint = RelunColors.Error, modifier = Modifier.size(16.dp))
        Text(text, color = RelunColors.Error, style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp))
    }
}

@Composable
fun LabeledField(
    label: String,
    modifier: Modifier = Modifier,
    trailingLabel: String? = null,
    error: String? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel(label, trailing = trailingLabel)
        content()
        if (!error.isNullOrBlank()) ErrorLine(error)
    }
}
