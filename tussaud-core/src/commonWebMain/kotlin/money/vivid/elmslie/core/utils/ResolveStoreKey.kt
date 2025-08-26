package money.vivid.elmslie.core.utils

import money.vivid.elmslie.core.plot.ElmScheme

internal actual fun resolvePlotKey(reducer: ElmScheme<*, *, *, *>): String =
  reducer::class.simpleName.orEmpty().replace("Scheme", "Plot")
