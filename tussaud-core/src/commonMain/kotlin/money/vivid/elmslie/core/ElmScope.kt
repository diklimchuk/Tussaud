package money.vivid.elmslie.core

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import money.vivid.elmslie.core.config.ElmslieConfig
import kotlin.coroutines.CoroutineContext

@Suppress("detekt.FunctionNaming")
fun ElmScope(
    name: String
) = CoroutineScope(
    context = ElmslieConfig.elmDispatcher +
            SupervisorJob() +
            CoroutineExceptionHandler { context, exception ->
                ElmslieConfig.logger.fatal("Unhandled error: $exception")
            } +
            CoroutineName(name)
)
