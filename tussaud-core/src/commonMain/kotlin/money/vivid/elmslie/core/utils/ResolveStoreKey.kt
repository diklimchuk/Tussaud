package money.vivid.elmslie.core.utils

import money.vivid.elmslie.core.plot.ElmScheme

internal expect fun resolvePlotKey(
    reducer: ElmScheme<*, *, *, *>
): String
