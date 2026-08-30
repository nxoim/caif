package com.nxoim.sample.ui.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

internal sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>

    data class Content<T>(val value: T) : LoadState<T>

    data object NotFound : LoadState<Nothing>

    data class Error(val cause: Throwable) : LoadState<Nothing>
}

internal fun <T> Flow<T?>.asLoadState(): Flow<LoadState<T>> = map { value ->
    if (value == null) {
        LoadState.NotFound
    } else {
        LoadState.Content(value)
    }
}.catch { cause ->
    emit(LoadState.Error(cause))
}
