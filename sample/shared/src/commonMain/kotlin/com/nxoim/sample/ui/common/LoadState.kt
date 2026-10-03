package com.nxoim.sample.ui.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

internal sealed interface LoadState<out T, I> {
    val id: I

    data class Loading<I>(override val id: I) : LoadState<Nothing, I>

    data class Content<T, I>(val value: T, override val id: I) : LoadState<T, I>

    data class NotFound<I>(override val id: I) : LoadState<Nothing, I>

    data class Error<I>(val cause: Throwable, override val id: I) : LoadState<Nothing, I>
}

internal fun <T, I> Flow<T?>.asLoadState(id: I): Flow<LoadState<T, I>> = map { value ->
    if (value == null) {
        LoadState.NotFound(id)
    } else {
        LoadState.Content(value, id)
    }
}.catch { cause ->
    emit(LoadState.Error(cause, id))
}
