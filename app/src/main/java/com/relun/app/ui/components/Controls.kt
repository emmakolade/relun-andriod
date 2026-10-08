package com.relun.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.relun.app.ui.common.Toast
import com.relun.app.ui.common.ToastKind
import com.relun.app.ui.theme.Relun
import com.relun.app.ui.theme.RelunColors
import kotlinx.coroutines.delay

/** Grey track with a white pill on the selected option. */
@Composable
fun <T> SegmentedControl(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(RelunColors.Track)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (value, label) ->
            val on = value == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .then(if (on) Modifier.shadow(1.dp, RoundedCornerShape(11.dp)) else Modifier)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (on) Color.White else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(value) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (on) RelunColors.Ink else RelunColors.Muted,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp),
                )
            }
        }
    }
}

/** White rounded card holding settings-style rows. */
@Composable
fun RowGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x29461E14), spotColor = Color(0x29461E14))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White),
        content = content,
    )
}

@Composable
fun ListRow(
    title: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    subtitle: String? = null,
    value: String? = null,
    showChevron: Boolean = onClick != null,
    titleColor: Color = RelunColors.Ink,
    divider: Boolean = true,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Column {
        Row(
            modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            leading?.invoke()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = titleColor, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium))
                if (subtitle != null) Text(subtitle, color = RelunColors.Muted, style = MaterialTheme.typography.bodySmall)
            }
            if (value != null) Text(value, color = RelunColors.Muted, style = MaterialTheme.typography.bodyMedium)
            trailing?.invoke(this)
            if (showChevron) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = RelunColors.Faint, modifier = Modifier.size(20.dp))
        }
        if (divider) Box(Modifier.fillMaxWidth().height(1.dp).background(RelunColors.Divider))
    }
}

@Composable
fun ToggleRow(title: String, checked: Boolean, onToggle: () -> Unit, subtitle: String? = null, divider: Boolean = true) {
    ListRow(title = title, subtitle = subtitle, onClick = onToggle, showChevron = false, divider = divider) {
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedTrackColor = Relun.segment.fill,
                checkedThumbColor = Color.White,
                checkedBorderColor = Color.Transparent,
                uncheckedTrackColor = Color(0xFFD6D6D6),
                uncheckedThumbColor = Color.White,
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

data class DialogSpec(
    val title: String,
    val body: String,
    val confirm: String,
    val destructive: Boolean = false,
    val icon: ImageVector? = null,
    val onConfirm: () -> Unit,
)

@Composable
fun ConfirmDialog(spec: DialogSpec, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (spec.icon != null) {
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(RelunColors.SuccessFill),
                    contentAlignment = Alignment.Center,
                ) { Icon(spec.icon, null, tint = RelunColors.SuccessText, modifier = Modifier.size(28.dp)) }
            }
            Text(spec.title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(spec.body, style = MaterialTheme.typography.bodyMedium, color = RelunColors.Body, textAlign = TextAlign.Center)
            Spacer(Modifier.height(2.dp))
            PrimaryButton(
                text = spec.confirm,
                onClick = { onDismiss(); spec.onConfirm() },
                height = 50.dp,
                container = if (spec.destructive) RelunColors.Error else Relun.segment.fill,
                content = if (spec.destructive) Color.White else Relun.segment.onFill,
            )
            LinkButton("Cancel", onDismiss, Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
        }
    }
}

/** Bottom sheet with the design's 32dp corners and drag handle. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelunSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = Color.White,
        scrimColor = RelunColors.Scrim,
        dragHandle = {
            Box(Modifier.padding(top = 10.dp, bottom = 6.dp).size(width = 40.dp, height = 5.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFDADADA)))
        },
        contentWindowInsets = { BottomSheetDefaults.windowInsets },
    ) {
        Column(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

@Composable
fun OfflineBanner(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(RelunColors.Ink)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Rounded.CloudOff, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Text(text, color = Color.White, style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp))
    }
}

@Composable
fun InfoNote(text: String, icon: ImageVector, modifier: Modifier = Modifier, background: Color = Color(0xFFF1F1F1), color: Color = RelunColors.Body) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(17.dp).padding(top = 1.dp))
        Text(text, color = color, style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp))
    }
}

/** Dark banner that drops in from the top for 2.8s. */
@Composable
fun ToastHost(toast: Toast?, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf<Toast?>(null) }
    LaunchedEffect(toast) {
        if (toast != null) {
            visible = toast
            delay(2800)
            if (visible?.id == toast.id) visible = null
        }
    }
    AnimatedVisibility(
        visible = visible != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier,
    ) {
        val t = visible ?: return@AnimatedVisibility
        val (icon, tint) = when (t.kind) {
            ToastKind.Success -> Icons.Rounded.CheckCircle to Color(0xFF3DDC84)
            ToastKind.Error -> Icons.Rounded.Error to Color(0xFFFF8A80)
            ToastKind.Warning -> Icons.Rounded.Warning to Color(0xFFFFC94D)
            ToastKind.Info -> Icons.Rounded.Info to Color(0xFF8AB4FF)
        }
        Row(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(RelunColors.Ink)
                .clickable { visible = null }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(t.text, color = Color.White, style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, lineHeight = 19.sp))
        }
    }
}
