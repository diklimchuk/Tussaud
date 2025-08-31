package money.vivid.elmslie.android.logger

import money.vivid.elmslie.android.logger.strategy.AndroidLog
import money.vivid.elmslie.android.logger.strategy.Crash
import money.vivid.elmslie.core.config.TussaudConfig
import money.vivid.elmslie.core.logger.strategy.IgnoreLog

fun TussaudConfig.defaultReleaseLogger() = logger {
  fatal(Crash)
  nonfatal(IgnoreLog)
  debug(IgnoreLog)
}

fun TussaudConfig.defaultDebugLogger() = logger {
  fatal(Crash)
  nonfatal(AndroidLog.E)
  debug(AndroidLog.E)
}
