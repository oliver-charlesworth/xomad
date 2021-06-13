package choliver.xomad.xuota

import choliver.xomad.Address
import choliver.xomad.StreamId
import choliver.xomad.xuota.ConsulArbitrator.Event.*
import com.orbitz.consul.Consul
import com.orbitz.consul.model.session.ImmutableSession
import com.orbitz.consul.option.ImmutableQueryOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.selects.select
import org.slf4j.LoggerFactory
import java.math.BigInteger
import kotlin.concurrent.thread

class ConsulArbitrator(
  private val address: Address
) {
  private val logger = LoggerFactory.getLogger(javaClass)

  private val client = Consul.builder().build()
  private val sessionClient = client.sessionClient()
  private val kvClient = client.keyValueClient()

  // TODO - this is gross
  private val mySessionId by lazy {
    sessionClient.createSession(ImmutableSession.builder().ttl(SESSION_TTL).build()).id.also {
      logger.info("Created session: $it")
      Runtime.getRuntime().addShutdownHook(thread(start = false) {
        logger.info("Destroying session: $it")
        sessionClient.destroySession(it)
      })
    }
  }

  private var streams = Streams()

  fun CoroutineScope.arbitrateStreams() = produce {
    val sessionRenewalTicker = ticker(SESSION_RENEWAL_PERIOD_MILLIS)
    val streamGrabTicker = ticker(STREAM_GRAB_PERIOD_MILLIS)
    val streamsUpdates = watchForStreamsUpdates()

    while (true) {
      select<Unit> {
        sessionRenewalTicker.onReceive {
          io { sessionClient.renewSession(mySessionId) }
        }

        streamGrabTicker.onReceive {
          val numToGrab = minOf(CAPACITY - streams.mine.size, streams.unowned.size, BATCH_SIZE)
          if (numToGrab > 0) {
            val targets = streams.unowned.shuffled().take(numToGrab)  // Randomise selection
            io { targets.forEach { kvClient.acquireLock("$KEY_PREFIX$it", "$address", mySessionId) } }
          }
        }

        streamsUpdates.onReceive { update ->
          (update.mine - streams.mine).forEach { send(StreamAcquired(it)) }
          (streams.mine - update.mine).forEach { send(StreamDropped(it)) }
          streams = update
        }
      }
    }
  }

  private fun CoroutineScope.watchForStreamsUpdates() = produce {
    var index = BigInteger.ZERO
    while (true) {
      val options = ImmutableQueryOptions.builder().index(index).wait(QUERY_WAIT).build()
      val response = io { kvClient.getConsulResponseWithValues(KEY_PREFIX, options) }

      response.response?.run {
        val asMap = associate { StreamId(it.key.removePrefix(KEY_PREFIX)) to it.session.orElse(null) }
        send(Streams(
          mine = asMap.filterValues { it == mySessionId }.keys,
          unowned = asMap.filterValues { it == null }.keys,
        ))
      }
      index = response.index
    }
  }

  private suspend fun <R> io(block: () -> R) = withContext(Dispatchers.IO) { block() }

  private data class Streams(
    val mine: Set<StreamId> = emptySet(),
    val unowned: Set<StreamId> = emptySet(),
  )

  sealed class Event {
    data class StreamAcquired(val id: StreamId) : Event()
    data class StreamDropped(val id: StreamId) : Event()
  }

  companion object {
    private const val CAPACITY = 6
    private const val BATCH_SIZE = 2
    private const val SESSION_RENEWAL_PERIOD_MILLIS = 20_000L
    private const val STREAM_GRAB_PERIOD_MILLIS = 1_000L
    private const val SESSION_TTL = "30s"
    private const val QUERY_WAIT = "5m"
    private const val KEY_PREFIX = "xuota/streams/"
  }
}
