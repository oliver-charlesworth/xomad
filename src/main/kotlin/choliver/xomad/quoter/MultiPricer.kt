package choliver.xomad.quoter

import choliver.xomad.Address
import choliver.xomad.ConsulUtils.acquireLock
import choliver.xomad.ConsulUtils.mySessionId
import choliver.xomad.ConsulUtils.renewSession
import choliver.xomad.ConsulUtils.updatesFor
import choliver.xomad.StreamId
import com.orbitz.consul.model.kv.Value
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import org.slf4j.LoggerFactory

class MultiPricer(private val address: Address) {
  private val logger = LoggerFactory.getLogger(javaClass)

  private val queries = Channel<Query>()
  private var mine = emptySet<StreamId>()
  private var unowned = emptySet<StreamId>()

  private data class Query(
    val streamId: StreamId,
    val result: CompletableDeferred<String?>,
  )

  suspend fun getPrice(streamId: StreamId): String? {
    val result = CompletableDeferred<String?>()
    queries.send(Query(streamId, result))
    return result.await()
  }

  fun CoroutineScope.start() = launch {
    val sessionRenewalTicker = ticker(SESSION_RENEWAL_PERIOD_MILLIS)
    val streamGrabTicker = ticker(STREAM_GRAB_PERIOD_MILLIS)
    val updates = updatesFor(KEY_PREFIX)

    while (true) {
      select<Unit> {
        sessionRenewalTicker.onReceive { renewSession() }
        streamGrabTicker.onReceive { attemptStreamGrab() }
        updates.onReceive { handleUpdates(it) }
        queries.onReceive { handleQuery(it) }
      }
    }
  }

  private suspend fun attemptStreamGrab() {
    val numToGrab = minOf(CAPACITY - mine.size, unowned.size, BATCH_SIZE)
    if (numToGrab > 0) {
      val targets = unowned.shuffled().take(numToGrab)  // Randomise selection
      targets.forEach { acquireLock("$KEY_PREFIX$it", "$address") }
    }
  }

  private fun handleUpdates(values: List<Value>) {
    val asMap = values.associate { StreamId(it.key.removePrefix(KEY_PREFIX)) to it.session.orElse(null) }

    val minePrevious = mine
    mine = asMap.filterValues { it == mySessionId }.keys
    unowned = asMap.filterValues { it == null }.keys

    (mine - minePrevious).forEach { logger.info("Stream acquired: $it") }
    (minePrevious - mine).forEach { logger.info("Stream dropped: $it") }
  }

  private fun handleQuery(query: Query) {
    query.result.complete(
      if (query.streamId in mine) {
        "1.23"
      } else {
        null
      }
    )
  }

  companion object {
    private const val CAPACITY = 4
    private const val BATCH_SIZE = 2
    private const val SESSION_RENEWAL_PERIOD_MILLIS = 20_000L
    private const val STREAM_GRAB_PERIOD_MILLIS = 1_000L
  }
}
