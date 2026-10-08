package com.relun.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relun.app.ui.theme.Outfit
import com.relun.app.ui.theme.Relun
import com.relun.app.ui.theme.RelunColors

/** Filled 54dp button. Defaults to the segment accent; pass colours for ink or white variants. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    loadingText: String? = null,
    container: Color = Relun.segment.fill,
    content: Color = Relun.segment.onFill,
    leadingIcon: ImageVector? = null,
    height: Dp = 54.dp,
) {
    val active = enabled && !loading
    val bg = if (active) container else RelunColors.Disabled
    val fg = if (active) content else RelunColors.DisabledText
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(enabled = active, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(18.dp), color = fg, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
            } else if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = if (loading && loadingText != null) loadingText else text,
                color = fg,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/** Black button used before a segment exists (sign-in) and on neutral screens. */
@Composable
fun InkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    loadingText: String? = null,
    leadingIcon: ImageVector? = null,
) = PrimaryButton(
    text, onClick, modifier, enabled, loading, loadingText,
    container = RelunColors.Ink, content = Color.White, leadingIcon = leadingIcon,
)

/** White button with a 1.5dp border. */
@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    height: Dp = 54.dp,
    textColor: Color = RelunColors.Ink,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(height),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.5.dp, RelunColors.Border),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, null, tint = textColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
            }
            Text(text, color = textColor, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Plain text action, 44dp tall for touch. */
@Composable
fun LinkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = RelunColors.Ink,
    underline: Boolean = false,
    style: TextStyle = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = color,
            style = style.copy(textDecoration = if (underline) TextDecoration.Underline else null),
        )
    }
}

enum class CircleStyle { Raised, Outlined, Glass, Flat }

/** 44dp round icon button: back, settings, more, close. */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: CircleStyle = CircleStyle.Outlined,
    size: Dp = 44.dp,
    iconSize: Dp = 20.dp,
    tint: Color = if (style == CircleStyle.Glass) Color.White else RelunColors.Ink,
) {
    val base = modifier
        .size(size)
        .clip(CircleShape)
    val styled = when (style) {
        CircleStyle.Raised -> base.background(Color.White).border(0.5.dp, RelunColors.BorderSoft, CircleShape)
        CircleStyle.Outlined -> base.background(Color.White).border(1.dp, Color(0xFFEDEDED), CircleShape)
        CircleStyle.Glass -> base.background(Color.Black.copy(alpha = 0.35f))
        CircleStyle.Flat -> base
    }
    Box(
        modifier = styled.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(),
            role = Role.Button,
            onClick = onClick,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier, style: CircleStyle = CircleStyle.Raised) =
    CircleIconButton(
        Icons.AutoMirrored.Rounded.ArrowBackIos,
        "Back",
        onClick,
        modifier,
        style = style,
        iconSize = 18.dp,
    )
