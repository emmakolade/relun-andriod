package com.relun.app.ui.screens.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relun.app.data.model.PendingBonusDto
import com.relun.app.data.model.Person
import com.relun.app.data.model.Wallet
import com.relun.app.ui.components.Avatar
import com.relun.app.ui.components.CoinIcon
import com.relun.app.ui.components.LinkButton
import com.relun.app.ui.components.OutlineButton
import com.relun.app.ui.components.PrimaryButton
import com.relun.app.ui.components.RelunSheet
import com.relun.app.ui.theme.Relun
import com.relun.app.ui.theme.RelunColors
import com.relun.app.util.formatCoins
import java.util.Locale

@Composable
fun CoinsSheet(
    wallet: Wallet,
    prices: Map<String, String>,
    selected: Int,
    buying: Boolean,
    onSelect: (Int) -> Unit,
    onBuy: () -> Unit,
    onDismiss: () -> Unit,
) {
    val seg = Relun.segment
    RelunSheet(onDismiss) {
        Text("Get Coins", style = MaterialTheme.typography.headlineSmall)
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(RelunColors.CoinPanel).border(1.dp, RelunColors.CoinPanelBorder, RoundedCornerShape(20.dp)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CoinIcon(48.dp)
            Column(Modifier.weight(1f)) {
                Text("Your balance", style = MaterialTheme.typography.bodySmall, color = RelunColors.WarningText)
                Text(formatCoins(wallet.balance), style = MaterialTheme.typography.headlineMedium)
            }
            if (wallet.balance < wallet.chatUnlockCost) {
                Text("Low", color = RelunColors.Error, style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(RelunColors.ErrorFill).padding(horizontal = 10.dp, vertical = 4.dp))
            }
        }
        Text("Select a Package", style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp))
        wallet.packages.chunked(2).forEachIndexed { row, pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEachIndexed { col, pkg ->
                    val index = row * 2 + col
                    val on = index == selected
                    Box(Modifier.weight(1f)) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (on) seg.tint else Color.White)
                                .border(2.dp, if (on) seg.fill else RelunColors.Border, RoundedCornerShape(20.dp))
                                .clickable { onSelect(index) }
                                .padding(horizontal = 14.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                CoinIcon(20.dp)
                                Text(formatCoins(pkg.coins), style = MaterialTheme.typography.titleMedium)
                            }
                            // The Play price is localised; the USD figure is only a fallback.
                            Text(
                                prices[pkg.productId]?.ifBlank { null } ?: String.format(Locale.US, "$%.2f", pkg.priceUsd),
                                style = MaterialTheme.typography.bodyMedium,
                                color = RelunColors.Body,
                            )
                        }
                        if (pkg.best) {
                            Text(
                                "Best Value",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                modifier = Modifier.align(Alignment.TopEnd).offset(x = (-12).dp, y = (-10).dp)
                                    .clip(RoundedCornerShape(999.dp)).background(RelunColors.Ink).padding(horizontal = 9.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
                if (pair.size == 1) Box(Modifier.weight(1f))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Perk(Icons.Outlined.ChatBubbleOutline, "Start a chat with a match · ${wallet.chatUnlockCost} coins")
            Perk(Icons.Outlined.Visibility, "See Likes & Views · ${wallet.insightsCost} coins / 30 days")
        }
        val pkg = wallet.packages.getOrNull(selected)
        PrimaryButton(
            text = pkg?.let { "Buy ${formatCoins(it.coins)} coins · ${prices[it.productId]?.ifBlank { null } ?: String.format(Locale.US, "$%.2f", it.priceUsd)}" } ?: "Loading packages…",
            onClick = onBuy,
            enabled = pkg != null,
            loading = buying,
            loadingText = "Waiting for Google Play…",
        )
        Text(
            "Prices in your local currency. Charged through Google Play.",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = RelunColors.Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Perk(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = RelunColors.Body, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = RelunColors.Body)
    }
}

@Composable
fun UnlockChatSheet(person: Person, wallet: Wallet, unlocking: Boolean, onUnlock: () -> Unit, onDismiss: () -> Unit) {
    val enough = wallet.balance >= wallet.chatUnlockCost
    RelunSheet(onDismiss) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(person.mainPhotoUrl, person.id, person.initial, 80.dp)
            Text("Start Conversation?", style = MaterialTheme.typography.titleLarge)
            Text(
                "Unlock chat with ${person.firstName} for ${wallet.chatUnlockCost} coins. It stays open for good, for both of you.",
                style = MaterialTheme.typography.bodyMedium,
                color = RelunColors.Muted,
                textAlign = TextAlign.Center,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Balance ", style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = RelunColors.Body)
                Text(formatCoins(wallet.balance), style = MaterialTheme.typography.labelMedium)
                Text(" coins", style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = RelunColors.Body)
            }
            if (!enough) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(RelunColors.ErrorFill).padding(10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Error, null, tint = RelunColors.Error, modifier = Modifier.size(16.dp))
                    Text("  Not enough coins", color = RelunColors.Error, style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium))
                }
            }
            PrimaryButton(if (enough) "Unlock for ${wallet.chatUnlockCost} coins" else "Top Up", onUnlock, loading = unlocking)
            LinkButton("Not now", onDismiss, Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun InsightsSheet(wallet: Wallet, onUnlock: () -> Unit, onDismiss: () -> Unit) {
    val seg = Relun.segment
    RelunSheet(onDismiss) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(seg.tint), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Favorite, null, tint = seg.text, modifier = Modifier.size(30.dp))
            }
            Text("See who likes you", style = MaterialTheme.typography.titleLarge)
            Text("Unlock Who Liked You and Profile Views for 30 days.", style = MaterialTheme.typography.bodyMedium, color = RelunColors.Muted, textAlign = TextAlign.Center)
            PrimaryButton(
                if (wallet.balance >= wallet.insightsCost) "Unlock for ${wallet.insightsCost} coins" else "Get coins to unlock",
                onUnlock,
            )
            LinkButton("Not now", onDismiss, Modifier.fillMaxWidth())
        }
    }
}

/**
 * "You got coins": shown once on the main screen after signup (or a gift), with
 * what coins are for. Closing it in any way marks the bonus as seen.
 */
@Composable
fun WelcomeCoinsSheet(bonus: PendingBonusDto, wallet: Wallet, onDismiss: () -> Unit) {
    val coins = formatCoins(bonus.coins)
    val signup = bonus.kind == "signup"
    RelunSheet(onDismiss) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(88.dp).clip(RoundedCornerShape(28.dp)).background(RelunColors.CoinPanel)
                    .border(1.dp, RelunColors.CoinPanelBorder, RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center,
            ) { CoinIcon(56.dp) }
            Text(
                if (signup) "Welcome gift: $coins coins" else "You got $coins coins",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                if (signup) "Here are $coins coins on us to help you get started. They’re already in your balance."
                else "A gift from Relun. They’re already in your balance.",
                style = MaterialTheme.typography.bodyMedium,
                color = RelunColors.Muted,
                textAlign = TextAlign.Center,
            )
        }
        Text("What coins are for", style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp), modifier = Modifier.padding(top = 4.dp))
        CoinUse(Icons.Outlined.ChatBubbleOutline, "Start a chat", "Unlock a conversation with a match. ${wallet.chatUnlockCost} coins, open for good.")
        CoinUse(Icons.Outlined.Visibility, "See who likes you", "Who liked you and who viewed your profile, for 30 days. ${wallet.insightsCost} coins.")
        PrimaryButton("Start exploring", onDismiss, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun CoinUse(icon: ImageVector, title: String, body: String) {
    val seg = Relun.segment
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(RelunColors.ChipFill).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(seg.tint), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = seg.text, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodySmall, color = RelunColors.Body)
        }
    }
}

@Composable
fun MoreSheet(person: Person, onReport: () -> Unit, onBlock: () -> Unit, onDismiss: () -> Unit) {
    RelunSheet(onDismiss) {
        Column {
            SheetAction(Icons.Outlined.Flag, "Report ${person.firstName}", RelunColors.Ink, onReport)
            SheetAction(Icons.Outlined.Block, "Block ${person.firstName}", RelunColors.Error, onBlock)
        }
        OutlineButton("Cancel", onDismiss, height = 52.dp)
    }
}

@Composable
private fun SheetAction(icon: ImageVector, text: String, color: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        Text(text, color = color, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
    }
}
