package money.vivid.elmslie.android.logger

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.startup.Initializer
import money.vivid.elmslie.core.config.TussaudConfig

class DefaultLoggerInitializer : Initializer<Unit> {

    override fun create(
        context: Context
    ) {
        val isDebug = 0 != context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE
        if (isDebug) TussaudConfig.defaultDebugLogger() else TussaudConfig.defaultReleaseLogger()
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> = mutableListOf()
}
