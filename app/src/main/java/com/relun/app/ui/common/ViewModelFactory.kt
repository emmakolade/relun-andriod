package com.relun.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.relun.app.RelunApp
import com.relun.app.di.AppContainer

@Composable
fun appContainer(): AppContainer = (LocalContext.current.applicationContext as RelunApp).container

/** `viewModel` with access to the [AppContainer]. */
@Composable
inline fun <reified VM : ViewModel> relunViewModel(
    owner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current),
    key: String? = null,
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = appContainer()
    return viewModel(
        viewModelStoreOwner = owner,
        key = key,
        factory = viewModelFactory { initializer { create(container) } },
    )
}
