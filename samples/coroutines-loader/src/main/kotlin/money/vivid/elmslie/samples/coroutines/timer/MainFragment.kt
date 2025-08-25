package money.vivid.elmslie.samples.coroutines.timer

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import money.vivid.elmslie.android.renderer.ElmRendererDelegate
import money.vivid.elmslie.android.renderer.androidElmPlot
import money.vivid.elmslie.samples.coroutines.timer.elm.Effect
import money.vivid.elmslie.samples.coroutines.timer.elm.Event
import money.vivid.elmslie.samples.coroutines.timer.elm.State
import money.vivid.elmslie.samples.coroutines.timer.elm.plotFactory

internal class MainFragment : Fragment(R.layout.fragment_main), ElmRendererDelegate<State, Effect> {

    companion object {
        private const val ARG = "ARG"

        fun newInstance(id: String): Fragment = MainFragment().apply { arguments = bundleOf(ARG to id) }
    }

    private val plot by androidElmPlot {
        plotFactory(id = get(ARG)!!)
    }

    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var currentValueText: TextView
    private lateinit var screenIdText: TextView
    private lateinit var generatedIdText: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startButton = view.findViewById(R.id.start)
        stopButton = view.findViewById(R.id.stop)
        currentValueText = view.findViewById(R.id.currentValue)
        screenIdText = view.findViewById(R.id.screenId)
        generatedIdText = view.findViewById(R.id.generatedID)

        startButton.setOnClickListener { plot.accept(Event.Input.Start) }
        stopButton.setOnClickListener { plot.accept(Event.Input.Stop) }
    }

    @SuppressLint("SetTextI18n")
    override fun render(state: State) {
        screenIdText.text = state.id
        generatedIdText.text = state.generatedId
        startButton.visibility = if (state.isStarted) GONE else VISIBLE
        stopButton.visibility = if (state.isStarted) VISIBLE else GONE
        currentValueText.text = "Seconds passed: ${state.secondsPassed}"
    }

    override fun handleEffect(effect: Effect) =
        when (effect) {
            is Effect.Error ->
                Snackbar.make(requireView().findViewById(R.id.content), "Error!", Snackbar.LENGTH_SHORT)
                    .show()
        }
}
