package choliver.xomad

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CoroutineUtils {
  suspend fun <R> io(block: () -> R) = withContext(Dispatchers.IO) { block() }
}
