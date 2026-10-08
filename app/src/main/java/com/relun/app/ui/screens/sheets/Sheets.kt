package com.relun.app.ui.screens.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.HeartBroken
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.ui.platform.LocalUriHandler
import com.relun.app.data.model.PlusPlanDto
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.MarkChatUnread
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
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
import com.relun.app.ui.components.RelunTextField
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
            if (wallet.balance < wallet.messageRequestCost) {
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
            Perk(Icons.Outlined.MarkChatUnread, "Message someone without matching · ${wallet.messageRequestCost} coins")
            Perk(Icons.Outlined.Visibility, "See Likes & Views · ${wallet.insights7Cost} coins / 7 days")
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

/**
 * Messaging someone without a match. Free while the weekly (or Plus monthly)
 * allowance lasts; after that it costs coins, and says plainly that the coins
 * are gone even if they decline or never answer.
 */
@Composable
fun MessageRequestSheet(
    person: Person,
    wallet: Wallet,
    draft: String,
    sending: Boolean,
    onDraft: (String) -> Unit,
    onSend: () -> Unit,
    onPlus: () -> Unit,
    onDismiss: () -> Unit,
) {
    val name = person.firstName
    val cost = wallet.messageRequestCost
    val freeLeft = wallet.requests.left ?: 0
    val free = freeLeft > 0
    val enough = free || wallet.balance >= cost
    RelunSheet(onDismiss) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(person.mainPhotoUrl, person.id, person.initial, 80.dp)
            Text("Message $name without matching?", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(
                "You can send up to 3 messages. If $name replies, you match and keep chatting for free.",
                style = MaterialTheme.typography.bodyMedium,
                color = RelunColors.Muted,
                textAlign = TextAlign.Center,
            )
        }
        RelunTextField(
            value = draft,
            onValueChange = onDraft,
            placeholder = "Say something to $name…",
            singleLine = false,
            minLines = 3,
        )
        val (fill, ink) = if (free) RelunColors.SuccessFill to RelunColors.SuccessText else RelunColors.WarningFill to RelunColors.WarningText
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(fill).padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(if (free) Icons.Rounded.Redeem else Icons.Rounded.Info, null, tint = ink, modifier = Modifier.padding(top = 1.dp).size(17.dp))
            Text(
                when {
                    !free -> "This costs $cost coins. Coins are not refunded if $name declines or doesn’t reply."
                    wallet.plus != null -> "Free with Relun Plus · $freeLeft of ${wallet.requests.limit} left this month."
                    else -> "Free: you get 1 free message request a week."
                },
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = ink,
            )
        }
        if (!free) {
            // The allowance resets on Lagos time: Monday for the free one, the 1st for Plus.
            val reset = wallet.requests.resetAt?.atZone(ZoneId.of("Africa/Lagos"))
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (wallet.plus != null) {
                        "You’ve used your ${wallet.requests.limit ?: wallet.plusPerks.monthlyRequests} Plus message requests this month." +
                            (reset?.let { " More on ${it.format(shortDate)}." } ?: "")
                    } else {
                        "You’ve used your free message request this week." +
                            (reset?.let { " Your next free one is on ${it.format(DateTimeFormatter.ofPattern("EEEE"))}." } ?: "")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = RelunColors.Body,
                )
                if (wallet.plus == null && wallet.plans.isNotEmpty()) {
                    Text(
                        "Get ${wallet.plusPerks.monthlyRequests} free message requests a month with Relun Plus",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline),
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onPlus),
                    )
                }
            }
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!free) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Balance ", style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = RelunColors.Body)
                    Text(formatCoins(wallet.balance), style = MaterialTheme.typography.labelMedium)
                    Text(" coins", style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = RelunColors.Body)
                }
            }
            PrimaryButton(
                when {
                    free -> "Send for free"
                    enough -> "Send for $cost coins"
                    else -> "Top Up"
                },
                onSend,
                enabled = !enough || draft.isNotBlank(),
                loading = sending,
            )
            LinkButton("Not now", onDismiss, Modifier.fillMaxWidth())
        }
    }
}

/** Naira from minor units, for when Google Play hasn't sent its own price. */
private fun moneyOf(amount: Int, currency: String): String =
    if (currency == "NGN") "₦" + formatCoins(amount / 100) else "$currency ${formatCoins(amount / 100)}"

private fun PlusPlanDto.period() = if (id == "weekly") "week" else "month"

/**
 * Likes & Views: Relun Plus (cheaper, and more), or a coin pass for people who
 * would rather not subscribe.
 */
@Composable
fun InsightsSheet(
    wallet: Wallet,
    plusPrices: Map<String, String>,
    buying: Boolean,
    onPlus: () -> Unit,
    onBuy: (days: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val seg = Relun.segment
    val weekly = wallet.plans.firstOrNull { it.id == "weekly" }
    val weeklyPrice = weekly?.let { plusPrices[it.playBasePlanId]?.takeIf(String::isNotBlank) ?: moneyOf(it.amount, it.currency) }
    RelunSheet(onDismiss) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(seg.tint), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Favorite, null, tint = seg.text, modifier = Modifier.size(30.dp))
            }
            Text("See who likes you", style = MaterialTheme.typography.titleLarge)
            Text("See who liked you and who viewed your profile.", style = MaterialTheme.typography.bodyMedium, color = RelunColors.Muted, textAlign = TextAlign.Center)
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).border(2.dp, seg.fill, RoundedCornerShape(20.dp))
                .background(seg.tint).clickable(onClick = onPlus).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Rounded.WorkspacePremium, null, tint = seg.text, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Get Relun Plus", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Likes & Views, unlimited likes and free message requests" + (weeklyPrice?.let { " · from $it/week" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = RelunColors.Body,
                )
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = seg.text, modifier = Modifier.size(22.dp))
        }
        Text("Or use coins", style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(7 to wallet.insights7Cost, 30 to wallet.insights30Cost).forEach { (days, cost) ->
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).border(1.5.dp, RelunColors.Border, RoundedCornerShape(18.dp))
                        .background(Color.White).clickable(enabled = !buying) { onBuy(days) }.padding(vertical = 14.dp, horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("$days days", style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        CoinIcon(18.dp)
                        Text(formatCoins(cost), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Balance ", style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = RelunColors.Body)
                Text(formatCoins(wallet.balance), style = MaterialTheme.typography.labelMedium)
                Text(" coins", style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = RelunColors.Body)
            }
            LinkButton("Not now", onDismiss, Modifier.fillMaxWidth())
        }
    }
}


private val shortDate = DateTimeFormatter.ofPattern("d MMM")

/**
 * Relun Plus through Google Play. Without it: the perks and the two plans. With
 * it: what it is and when it renews or ends; Google Play manages cancelling.
 */
@Composable
fun PlusSheet(
    wallet: Wallet,
    plusPrices: Map<String, String>,
    buying: Boolean,
    onBuy: (basePlanId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val seg = Relun.segment
    val plus = wallet.plus
    val uri = LocalUriHandler.current
    var planId by remember { mutableStateOf("monthly") }
    val plan = wallet.plans.firstOrNull { it.id == planId } ?: wallet.plans.firstOrNull()
    val priceOf = { p: PlusPlanDto -> plusPrices[p.playBasePlanId]?.takeIf(String::isNotBlank) ?: moneyOf(p.amount, p.currency) }

    RelunSheet(onDismiss) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(seg.fill), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.WorkspacePremium, null, tint = seg.onFill, modifier = Modifier.size(32.dp))
            }
            Text("Relun Plus", style = MaterialTheme.typography.titleLarge)
        }
        if (plus != null) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(RelunColors.SuccessFill).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("You’re on Plus · ${if (plus.plan == "weekly") "Weekly" else "Monthly"}", style = MaterialTheme.typography.titleSmall, color = RelunColors.SuccessText)
                val date = plus.until.atZone(ZoneId.systemDefault()).format(shortDate)
                Text(if (plus.autoRenew) "Renews $date" else "Ends $date", style = MaterialTheme.typography.bodySmall, color = RelunColors.SuccessText)
            }
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Perk(Icons.Outlined.FavoriteBorder, "Unlimited likes")
            Perk(Icons.Outlined.Visibility, "See who likes you and who viewed you")
            Perk(Icons.Outlined.MarkChatUnread, "${wallet.plusPerks.monthlyRequests} free message requests a month")
            Perk(Icons.Outlined.Event, "Up to ${wallet.plusPerks.activeDates} date plans live at once")
        }
        if (plus == null && plan != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                wallet.plans.forEach { p ->
                    val on = p.id == plan.id
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                            .border(2.dp, if (on) seg.fill else RelunColors.Border, RoundedCornerShape(18.dp))
                            .background(if (on) seg.tint else Color.White)
                            .clickable { planId = p.id }
                            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (p.id == "monthly") {
                            Text(
                                "Best value",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = seg.onFill,
                                modifier = Modifier.clip(RoundedCornerShape(50)).background(seg.fill).padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        } else {
                            Spacer(Modifier.size(18.dp))
                        }
                        Text(if (p.id == "weekly") "Weekly" else "Monthly", style = MaterialTheme.typography.labelMedium, color = RelunColors.Body)
                        Text(priceOf(p), style = MaterialTheme.typography.titleMedium)
                        Text("per ${p.period()}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = RelunColors.Muted)
                    }
                }
            }
            Text(
                "Renews each ${plan.period()} through Google Play. Cancel anytime.",
                style = MaterialTheme.typography.bodySmall,
                color = RelunColors.Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton("Subscribe · ${priceOf(plan)}/${plan.period()}", { onBuy(plan.playBasePlanId) }, loading = buying)
        }
        if (plus?.source == "play") {
            OutlineButton(
                "Manage in Google Play",
                { uri.openUri("https://play.google.com/store/account/subscriptions?sku=${plan?.playProductId ?: "relun_plus"}&package=com.relun.app") },
                height = 48.dp,
            )
        }
        LinkButton(if (plus != null) "Close" else "Not now", onDismiss, Modifier.fillMaxWidth())
    }
}

/** Out of free likes for today: when they come back, and Plus for unlimited. */
@Composable
fun LikeLimitSheet(wallet: Wallet, onPlus: () -> Unit, onDismiss: () -> Unit) {
    val seg = Relun.segment
    val resetAt = wallet.likes.resetAt
    val minutesLeft = resetAt?.let { java.time.Duration.between(java.time.Instant.now(), it).toMinutes() } ?: 0
    val whenText = when {
        resetAt == null || minutesLeft <= 0 -> "soon"
        minutesLeft >= 60 -> "in ${minutesLeft / 60}h ${minutesLeft % 60}m"
        else -> "in ${minutesLeft}m"
    }
    RelunSheet(onDismiss) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(seg.tint), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.HeartBroken, null, tint = seg.text, modifier = Modifier.size(30.dp))
            }
            Text("You’re out of likes for today", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(
                "You get ${wallet.likes.limit ?: 15} free likes a day. New ones arrive $whenText.",
                style = MaterialTheme.typography.bodyMedium,
                color = RelunColors.Muted,
                textAlign = TextAlign.Center,
            )
            PrimaryButton("Get unlimited likes with Plus", onPlus, leadingIcon = Icons.Rounded.WorkspacePremium)
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
        CoinUse(Icons.Outlined.MarkChatUnread, "Message without matching", "Send someone a message request before you match. ${wallet.messageRequestCost} coins.")
        CoinUse(Icons.Outlined.Visibility, "See who likes you", "Who liked you and who viewed your profile. ${wallet.insights7Cost} coins for 7 days.")
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
