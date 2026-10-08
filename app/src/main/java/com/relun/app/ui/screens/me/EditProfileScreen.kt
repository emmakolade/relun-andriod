package com.relun.app.ui.screens.me

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relun.app.data.model.UpdateProfileBody
import com.relun.app.ui.common.appContainer
import com.relun.app.ui.components.BackButton
import com.relun.app.ui.components.InfoNote
import com.relun.app.ui.components.LabeledField
import com.relun.app.ui.components.PrimaryButton
import com.relun.app.ui.components.RelunTextField
import com.relun.app.ui.navigation.LocalAppActions
import com.relun.app.ui.theme.RelunColors
import kotlinx.coroutines.launch

/** Name, bio, work, school, city and interests. Age and segment are fixed at signup. */
@Composable
fun EditProfileScreen() {
    val container = appContainer()
    val me by container.profile.me.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val scope = rememberCoroutineScope()

    val start = me
    var name by remember(start) { mutableStateOf(start?.name.orEmpty()) }
    var bio by remember(start) { mutableStateOf(start?.bio.orEmpty()) }
    var job by remember(start) { mutableStateOf(start?.occupation.orEmpty()) }
    var school by remember(start) { mutableStateOf(start?.education.orEmpty()) }
    var city by remember(start) { mutableStateOf(start?.city.orEmpty()) }
    var interests by remember(start) { mutableStateOf(start?.interests.orEmpty().joinToString(", ")) }
    var saving by remember { mutableStateOf(false) }

    val words = KeyboardOptions(capitalization = KeyboardCapitalization.Words)

    Column(Modifier.fillMaxSize().background(RelunColors.Background).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BackButton(actions.back)
            Text("Edit profile", style = MaterialTheme.typography.headlineSmall)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            LabeledField("Full name") { RelunTextField(name, { name = it.take(100) }, Modifier.fillMaxWidth(), keyboardOptions = words) }
            LabeledField("Bio", trailingLabel = "${bio.length}/150") {
                RelunTextField(bio, { bio = it.take(150) }, Modifier.fillMaxWidth(), placeholder = "Something true and a little specific…", singleLine = false, minLines = 3,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp))
            }
            LabeledField("Work", trailingLabel = "Optional") { RelunTextField(job, { job = it.take(80) }, Modifier.fillMaxWidth(), placeholder = "e.g. Architect", keyboardOptions = words) }
            LabeledField("Education", trailingLabel = "Optional") { RelunTextField(school, { school = it.take(80) }, Modifier.fillMaxWidth(), placeholder = "e.g. University of Lagos", keyboardOptions = words) }
            LabeledField("City", trailingLabel = "Optional") { RelunTextField(city, { city = it.take(80) }, Modifier.fillMaxWidth(), placeholder = "e.g. Lekki, Lagos", keyboardOptions = words) }
            LabeledField("Interests", trailingLabel = "Comma separated") {
                RelunTextField(interests, { interests = it.take(200) }, Modifier.fillMaxWidth(), placeholder = "Jazz, Hiking, Cooking")
            }
            InfoNote("Your date of birth and what you’re looking for can’t be changed.", Icons.Rounded.Lock)
        }
        PrimaryButton(
            "Save",
            onClick = {
                saving = true
                scope.launch {
                    container.profile.updateDetails(
                        UpdateProfileBody(
                            fullName = name.trim().ifBlank { null },
                            bio = bio.trim(),
                            occupation = job.trim(),
                            education = school.trim(),
                            city = city.trim(),
                            interests = interests.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(10),
                        )
                    ).onSuccess {
                        container.messenger.success("Profile saved")
                        actions.back()
                    }.onFailure { container.messenger.error(it.message ?: "Couldn’t save your profile.") }
                    saving = false
                }
            },
            enabled = name.isNotBlank(),
            loading = saving,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        )
    }
}
