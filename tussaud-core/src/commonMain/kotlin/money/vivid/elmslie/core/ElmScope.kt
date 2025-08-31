package money.vivid.elmslie.core

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import money.vivid.elmslie.core.config.TussaudConfig

@Suppress("detekt.FunctionNaming")
fun ElmScope(
    name: String
) = CoroutineScope(
    context = TussaudConfig.elmDispatcher +
            SupervisorJob() +
            CoroutineExceptionHandler { context, exception ->
                TussaudConfig.logger.fatal("Unhandled error: $exception")
            } +
            CoroutineName(name)
)
