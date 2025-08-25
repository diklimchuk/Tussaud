package money.vivid.elmslie.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import com.google.gson.Gson
import money.vivid.elmslie.core.plot.Plot
import money.vivid.elmslie.core.plot.toCachedPlot
import kotlin.reflect.KClass

/**
 * In order to access previously saved state (via [saveState]) in [plotFactory] one must use
 * SavedStateHandle.get<Bundle>(StateBundleKey)
 */
@MainThread
inline fun <reified State : Any, Event : Any, Effect : Any> Fragment.elmPlot(
    key: String = this::class.java.canonicalName ?: this::class.java.simpleName,
    crossinline viewModelStoreOwner: () -> ViewModelStoreOwner = { this },
    crossinline savedStateRegistryOwner: () -> SavedStateRegistryOwner = { this },
    crossinline defaultArgs: () -> Bundle = { arguments ?: bundleOf() },
    saveState: Bundle.(State) -> Unit = {},
    noinline plotFactory: SavedStateHandle.() -> Plot<State, Event, Effect>,
): Lazy<Plot<State, Event, Effect>> = elmPlot(
    plotFactory = plotFactory,
    key = key,
    viewModelStoreOwner = viewModelStoreOwner,
    savedStateRegistryOwner = savedStateRegistryOwner,
    defaultArgs = defaultArgs,
    lifecycleOwner = this
)

/**
 * In order to access previously saved state (via [saveState]) in [plotFactory] one must use
 * SavedStateHandle.get<Bundle>(StateBundleKey)
 */
@MainThread
inline fun <reified State : Any, Event : Any, Effect : Any> ComponentActivity.elmPlot(
    key: String = this::class.java.canonicalName ?: this::class.java.simpleName,
    crossinline viewModelStoreOwner: () -> ViewModelStoreOwner = { this },
    crossinline savedStateRegistryOwner: () -> SavedStateRegistryOwner = { this },
    crossinline defaultArgs: () -> Bundle = { this.intent?.extras ?: bundleOf() },
    saveState: Bundle.(State) -> Unit = {},
    noinline plotFactory: SavedStateHandle.() -> Plot<State, Event, Effect>,
): Lazy<Plot<State, Event, Effect>> = elmPlot(
    plotFactory = plotFactory,
    key = key,
    viewModelStoreOwner = viewModelStoreOwner,
    savedStateRegistryOwner = savedStateRegistryOwner,
    defaultArgs = defaultArgs,
    lifecycleOwner = this
)

@MainThread
inline fun <reified State : Any, Event : Any, Effect : Any> elmPlot(
    key: String,
    crossinline viewModelStoreOwner: () -> ViewModelStoreOwner,
    crossinline savedStateRegistryOwner: () -> SavedStateRegistryOwner,
    crossinline defaultArgs: () -> Bundle,
    noinline plotFactory: SavedStateHandle.() -> Plot<State, Event, Effect>,
    lifecycleOwner: LifecycleOwner,
): Lazy<Plot<State, Event, Effect>> = lazy(LazyThreadSafetyMode.NONE) {
    val factory = retainedElmPlotFactory(
        stateRegistryOwner = savedStateRegistryOwner.invoke(),
        defaultArgs = defaultArgs.invoke(),
        plotFactory = plotFactory,
        lifecycleOwner = lifecycleOwner,
    )
    val provider = ViewModelProvider(viewModelStoreOwner.invoke(), factory)

    @Suppress("UNCHECKED_CAST")
    provider[key, RetainedElmStore::class.java].store as Plot<State, Event, Effect>
}

inline fun <reified State : Any, Event : Any, Effect : Any> retainedElmPlot(
    lifecycleOwner: LifecycleOwner,
    savedStateHandle: SavedStateHandle,
    noinline plotFactory: SavedStateHandle.() -> Plot<State, Event, Effect>,
) = RetainedElmStore(
    lifecycleOwner = lifecycleOwner,
    savedStateHandle = savedStateHandle,
    plotFactory = plotFactory,
    classOfState = State::class
)

class RetainedElmStore<State : Any, Event : Any, Effect : Any>(
    lifecycleOwner: LifecycleOwner,
    savedStateHandle: SavedStateHandle,
    plotFactory: SavedStateHandle.() -> Plot<State, Event, Effect>,
    classOfState: KClass<State>,
) : ViewModel() {

    val store = plotFactory.invoke(savedStateHandle).toCachedPlot()

    init {
        savedStateHandle.setSavedStateProvider(STATE_BUNDLE_KEY) {
            store.currentState
                ?.let { gson.toJson(it) }
                ?.let { bundleOf(STATE_KEY to it) }
                ?: bundleOf()
        }
        savedStateHandle.getLiveData<Bundle>(STATE_BUNDLE_KEY).observe(lifecycleOwner, object : Observer<Bundle> {
            override fun onChanged(value: Bundle) {
                store.currentState = gson.fromJson(value.getString(STATE_KEY), classOfState.java)
            }
        })
    }

    override fun onCleared() {
        store.stop()
    }

    companion object {
        private val gson = Gson()
        private const val STATE_KEY = "ELM_STORE_STATE_KEY"
        const val STATE_BUNDLE_KEY = "elm_store_state_bundle"
    }
}

inline fun <reified State : Any, Event : Any, Effect : Any> retainedElmPlotFactory(
    stateRegistryOwner: SavedStateRegistryOwner,
    defaultArgs: Bundle,
    lifecycleOwner: LifecycleOwner,
    noinline plotFactory: SavedStateHandle.() -> Plot<State, Event, Effect>,
) = RetainedElmStoreFactory(
    stateRegistryOwner = stateRegistryOwner,
    defaultArgs = defaultArgs,
    lifecycleOwner = lifecycleOwner,
    plotFactory = plotFactory,
    classOfState = State::class,
)

class RetainedElmStoreFactory<State : Any, Event : Any, Effect : Any>(
    stateRegistryOwner: SavedStateRegistryOwner,
    defaultArgs: Bundle,
    private val lifecycleOwner: LifecycleOwner,
    private val plotFactory: SavedStateHandle.() -> Plot<State, Event, Effect>,
    private val classOfState: KClass<State>,
) : AbstractSavedStateViewModelFactory(stateRegistryOwner, defaultArgs) {

    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle,
    ): T {
        @Suppress("UNCHECKED_CAST")
        return RetainedElmStore(
            savedStateHandle = handle,
            plotFactory = plotFactory,
            lifecycleOwner = lifecycleOwner,
            classOfState = classOfState,
        ) as T
    }
}
