package money.vivid.elmslie.android.renderer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withCreated
import androidx.savedstate.SavedStateRegistryOwner
import kotlinx.coroutines.launch
import money.vivid.elmslie.android.elmPlot
import money.vivid.elmslie.core.plot.Plot

@Suppress("OptionalUnit")
interface ElmRendererDelegate<State : Any, Effect : Any> {

    fun render(
        state: State
    )

    fun handleEffect(
        effect: Effect
    ): Unit? = Unit

    fun mapList(
        state: State
    ): List<Any> = emptyList()

    fun renderList(
        state: State,
        list: List<Any>
    ): Unit = Unit
}

/**
 * The function makes a connection between the store and the lifecycle owner by collecting states
 * and effects and calling corresponds callbacks.
 *
 * Store creates and connects all required entities when given lifecycle reached CREATED state.
 *
 * In order to access previously saved state (via [saveState]) in [plotFactory] one must use
 * SavedStateHandle.get<Bundle>(StateBundleKey)
 *
 * NOTE: If you implement your own ElmRendererDelegate, you should also implement the following
 * interfaces: [ViewModelStoreOwner], [SavedStateRegistryOwner], [LifecycleOwner].
 */
@Suppress("LongParameterList")
@MainThread
inline fun <reified State : Any, Event : Any, Effect : Any> ElmRendererDelegate<State, Effect>.androidElmPlot(
    key: String = this::class.java.canonicalName ?: this::class.java.simpleName,
    crossinline defaultArgs: () -> Bundle = {
        val args =
            when (this) {
                is Fragment -> arguments
                is ComponentActivity -> intent.extras
                else -> null
            }
        args ?: bundleOf()
    },
    noinline plotFactory: SavedStateHandle.() -> Plot<State, Event, Effect>,
): Lazy<Plot<State, Event, Effect>> {
    require(this is ViewModelStoreOwner) { "Should implement [ViewModelStoreOwner]" }
    require(this is SavedStateRegistryOwner) { "Should implement [SavedStateRegistryOwner]" }
    return androidElmPlot(
        key = key,
        viewModelStoreOwner = { this },
        savedStateRegistryOwner = { this },
        defaultArgs = defaultArgs,
        plotFactory = plotFactory,
    )
}

@Suppress("LongParameterList")
@MainThread
inline fun <reified State : Any, Event : Any, Effect : Any> ElmRendererDelegate<State, Effect>.androidElmPlot(
    key: String = this::class.java.canonicalName ?: this::class.java.simpleName,
    crossinline viewModelStoreOwner: () -> ViewModelStoreOwner,
    crossinline savedStateRegistryOwner: () -> SavedStateRegistryOwner,
    crossinline defaultArgs: () -> Bundle = {
        val args =
            when (this) {
                is Fragment -> arguments
                is ComponentActivity -> intent.extras
                else -> null
            }
        args ?: bundleOf()
    },
    noinline plotFactory: SavedStateHandle.() -> Plot<State, Event, Effect>,
): Lazy<Plot<State, Event, Effect>> {
    require(this is LifecycleOwner) { "Should implement [LifecycleOwner]" }
    val lazyStore = elmPlot(
        plotFactory = plotFactory,
        key = key,
        viewModelStoreOwner = viewModelStoreOwner,
        savedStateRegistryOwner = savedStateRegistryOwner,
        defaultArgs = defaultArgs,
        lifecycleOwner = this
    )
    with(this) {
        lifecycleScope.launch {
            withCreated {
                ElmRenderer(internalPlot = lazyStore.value, delegate = this@with, lifecycle = lifecycle)
            }
        }
    }
    return lazyStore
}
