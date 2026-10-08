package com.relun.app.ui.common

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class ToastKind { Success, Error, Warning, Info }

data class Toast(val kind: ToastKind, val text: String, val id: Long = System.nanoTime())

/** App-wide toasts. Any view model can post; the root screen shows them. */
class Messenger {
    private val _toasts = MutableSharedFlow<Toast>(extraBufferCapacity = 8)
    val toasts: SharedFlow<Toast> = _toasts.asSharedFlow()

    fun success(text: String) = _toasts.tryEmit(Toast(ToastKind.Success, text))
    fun error(text: String) = _toasts.tryEmit(Toast(ToastKind.Error, text))
    fun info(text: String) = _toasts.tryEmit(Toast(ToastKind.Info, text))
    fun warning(text: String) = _toasts.tryEmit(Toast(ToastKind.Warning, text))
}
