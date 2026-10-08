package com.relun.app.ui.screens.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relun.app.data.repository.ContactMethod
import com.relun.app.ui.common.relunViewModel
import com.relun.app.ui.components.BackButton
import com.relun.app.ui.components.ErrorLine
import com.relun.app.ui.components.InkButton
import com.relun.app.ui.components.LinkButton
import com.relun.app.ui.components.RelunTextField
import com.relun.app.ui.components.SegmentedControl
import com.relun.app.ui.theme.Outfit
import com.relun.app.ui.theme.RelunColors

@Composable
fun CodeSignInScreen(initialMethod: String, onBack: () -> Unit) {
    val vm = relunViewModel { CodeSignInViewModel(it, initialMethod) }
    val s by vm.state.collectAsStateWithLifecycle()

    val back = { if (s.step == 2) vm.changeContact() else onBack() }
    BackHandler(onBack = back)

    Column(
        Modifier
            .fillMaxSize()
            .background(RelunColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp)
            .padding(top = 6.dp, bottom = 24.dp),
    ) {
        BackButton(back)
        Spacer(Modifier.height(24.dp))
        if (s.step == 1) ContactStep(s, vm) else CodeStep(s, vm)
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.ContactStep(
    s: CodeSignInState,
    vm: CodeSignInViewModel,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(s.method) { runCatching { focus.requestFocus() } }

    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            if (s.method == ContactMethod.Phone) "What’s your number?" else "What’s your email?",
            style = MaterialTheme.typography.headlineLarge,
        )
        Text("We’ll send you a 6-digit code. No password, ever.", style = MaterialTheme.typography.bodyMedium, color = RelunColors.Muted)
        // Phone sign-in is off for now; restore this toggle with it.
        // SegmentedControl(
        //     options = listOf(ContactMethod.Phone to "Phone", ContactMethod.Email to "Email"),
        //     selected = s.method,
        //     onSelect = vm::setMethod,
        // )
        val action = KeyboardActions(onDone = { vm.sendCode() })
        if (s.method == ContactMethod.Phone) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier
                        .height(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.5.dp, RelunColors.Border, RoundedCornerShape(16.dp))
                        .clickable(onClick = vm::nextCountryCode)
                        .padding(start = 14.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(s.countryCode, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
                    Icon(Icons.Rounded.KeyboardArrowDown, "Change country code", tint = RelunColors.Muted, modifier = Modifier.size(16.dp))
                }
                RelunTextField(
                    value = s.contact,
                    onValueChange = vm::setContact,
                    placeholder = "801 234 5678",
                    isError = s.contactError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                    keyboardActions = action,
                    modifier = Modifier.weight(1f).focusRequester(focus),
                )
            }
        } else {
            RelunTextField(
                value = s.contact,
                onValueChange = vm::setContact,
                placeholder = "you@example.com",
                isError = s.contactError != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                keyboardActions = action,
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        }
        (s.contactError ?: s.sendError)?.let { ErrorLine(it) }
    }
    InkButton(
        "Send Code",
        vm::sendCode,
        enabled = s.contactValid,
        loading = s.busy,
        loadingText = "Sending…",
    )
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.CodeStep(s: CodeSignInState, vm: CodeSignInViewModel) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Enter the code", style = MaterialTheme.typography.headlineLarge)
        Text(
            androidx.compose.ui.text.buildAnnotatedString {
                append("Sent to ")
                pushStyle(androidx.compose.ui.text.SpanStyle(color = RelunColors.Ink, fontWeight = FontWeight.SemiBold))
                append(s.sentTo)
                pop()
            },
            style = MaterialTheme.typography.bodyMedium,
            color = RelunColors.Muted,
        )

        // Six visible boxes over one invisible field, so paste and SMS autofill just work.
        Box(Modifier.padding(top = 8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(6) { i ->
                    val digit = s.code.getOrNull(i)?.toString().orEmpty()
                    val active = i == minOf(s.code.length, 5) && s.codeError == null
                    val border = when {
                        s.codeError != null -> RelunColors.Error
                        active -> RelunColors.Ink
                        digit.isNotEmpty() -> Color(0xFFBDBDBD)
                        else -> RelunColors.Border
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(5f / 6f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (s.codeError != null) RelunColors.ErrorFill else Color.White)
                            .border(2.dp, border, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(digit, fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, color = RelunColors.Ink)
                    }
                }
            }
            BasicTextField(
                value = s.code,
                onValueChange = vm::setCode,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { vm.verify() }),
                textStyle = TextStyle(color = Color.Transparent),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.Transparent),
                modifier = Modifier
                    .matchParentSize()
                    .focusRequester(focus)
                    .semantics { contentDescription = "6-digit code" },
            )
        }

        s.codeError?.let { ErrorLine(it) }

        Row(
            Modifier.fillMaxWidth().heightIn(min = 44.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (s.resendIn > 0) {
                Text(
                    "Resend code in 0:${s.resendIn.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                    color = RelunColors.Muted,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Didn’t receive it? ", style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = RelunColors.Muted)
                    LinkButton("Resend", vm::resend, underline = true, style = MaterialTheme.typography.labelMedium)
                }
            }
            LinkButton(
                if (s.method == ContactMethod.Phone) "Change number" else "Change email",
                vm::changeContact,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
    InkButton("Verify", vm::verify, enabled = s.code.length == 6, loading = s.busy, loadingText = "Verifying…")
}
