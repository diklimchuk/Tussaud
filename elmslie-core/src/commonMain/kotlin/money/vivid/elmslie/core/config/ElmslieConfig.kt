package money.vivid.elmslie.core.config

import kotlinx.coroutines.CoroutineDispatcher
import money.vivid.elmslie.core.logger.ElmslieLogConfiguration
import money.vivid.elmslie.core.logger.ElmslieLogger
import money.vivid.elmslie.core.logger.strategy.IgnoreLog
import money.vivid.elmslie.core.utils.ElmDispatcher
import kotlin.concurrent.Volatile

object ElmslieConfig {

    @Volatile
    var logger: ElmslieLogger = ElmslieLogConfiguration().apply { always(IgnoreLog) }.build()
        private set

    @Volatile
    var elmDispatcher: CoroutineDispatcher = ElmDispatcher

    @Volatile
    var shouldStopOnProcessDeath: Boolean = true

    fun logger(config: (ElmslieLogConfiguration.() -> Unit)) {
        ElmslieLogConfiguration().apply(config).build().also { logger = it }
    }
}
