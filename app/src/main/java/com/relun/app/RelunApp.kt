package com.relun.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.relun.app.di.AppContainer
import com.relun.app.push.createNotificationChannels
import com.relun.app.telemetry.Telemetry

class RelunApp : Application(), SingletonImageLoader.Factory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        Telemetry.init(this)
        createNotificationChannels(this)
        container = AppContainer(this)
        container.session.current.userId?.let(Telemetry::identify)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { container.imageHttpClient })) }
            .crossfade(true)
            .build()
}
