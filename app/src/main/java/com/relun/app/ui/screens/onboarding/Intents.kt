package com.relun.app.ui.screens.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** Opens this app's page in system Settings, where location can be turned back on. */
fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
