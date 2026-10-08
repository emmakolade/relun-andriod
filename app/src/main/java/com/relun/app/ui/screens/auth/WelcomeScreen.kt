package com.relun.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relun.app.ui.components.InkButton
import com.relun.app.ui.components.OutlineButton
import com.relun.app.ui.components.RingsMark
import com.relun.app.ui.theme.Pacifico
import com.relun.app.ui.theme.RelunColors

@Composable
fun WelcomeScreen(onPhone: () -> Unit, onEmail: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        HeroArt(Modifier.weight(1f))
        Column(
            Modifier
                .offset(y = (-32).dp)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(Color.White)
                .padding(start = 24.dp, end = 24.dp, top = 28.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Find Your Match, Your Way", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Connect with people who share your relationship intentions.",
                style = MaterialTheme.typography.bodyMedium,
                color = RelunColors.Muted,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            // Phone sign-in is off until SMS delivery (Termii DND sender) is sorted.
            // InkButton("Continue with Phone", onPhone, leadingIcon = Icons.Outlined.Call)
            // OutlineButton("Continue with Email", onEmail, leadingIcon = Icons.Outlined.Email)
            InkButton("Continue with Email", onEmail, leadingIcon = Icons.Outlined.Email)
            Text(
                buildAnnotatedString {
                    append("By continuing you agree to our ")
                    withStyle(SpanStyle(color = RelunColors.Ink, fontWeight = FontWeight.SemiBold)) { append("Terms of Service") }
                    append(" and ")
                    withStyle(SpanStyle(color = RelunColors.Ink, fontWeight = FontWeight.SemiBold)) { append("Privacy Policy") }
                    append(".")
                },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = RelunColors.Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun HeroArt(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .background(RelunColors.HeroGradient)
            .background(Brush.radialGradient(listOf(RelunColors.HeroGlow, Color.Transparent), radius = 900f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.statusBarsPadding().padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(
                Modifier
                    .size(112.dp)
                    .shadow(24.dp, RoundedCornerShape(30.dp), ambientColor = Color(0x66461E14), spotColor = Color(0x66461E14))
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) { RingsMark(76.dp) }
            Text("relun", fontFamily = Pacifico, fontSize = 64.sp, color = Color.White)
        }
    }
}
