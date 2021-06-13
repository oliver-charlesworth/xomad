package choliver.xomad.xuota

import choliver.xomad.Address
import choliver.xomad.StreamId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import org.slf4j.LoggerFactory

class MultiPricer(
  private val address: Address
) {
  private val logger = LoggerFactory.getLogger(javaClass)

  private val streams = mutableSetOf<StreamId>()
  private val queries = Channel<Query>()

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
    val events = with(ConsulArbitrator(address)) { arbitrateStreams() }

    while (true) {
      select<Unit> {
        events.onReceive { handleArbitratorEvent(it) }
        queries.onReceive { handleQuery(it) }
      }
    }
  }

  private fun handleQuery(query: Query) {
    query.result.complete(
      if (query.streamId in streams) {
        "1.23"
      } else {
        null
      }
    )
  }

  private fun handleArbitratorEvent(e: ConsulArbitrator.Event) {
    when (e) {
      is ConsulArbitrator.Event.StreamAcquired -> {
        logger.info("Stream acquired: ${e.id}")
        streams += e.id
      }
      is ConsulArbitrator.Event.StreamDropped -> {
        logger.info("Stream dropped: ${e.id}")
        streams -= e.id
      }
    }
  }
}
