package choliver.xomad

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

  fun CoroutineScope.start() = launch {
    val sessionRenewalTicker = ticker(SESSION_RENEWAL_PERIOD_MILLIS)
    val streamGrabTicker = ticker(STREAM_GRAB_PERIOD_MILLIS)
    val streamsUpdates = watchForStreamsUpdates()
    while (true) {
      select<Unit> {
        sessionRenewalTicker.onReceive { renewSession() }
        streamGrabTicker.onReceive { maybeGrabStreams() }
        streamsUpdates.onReceive { update -> handleStreamsUpdate(update) }
      }
    }
  }

  private fun CoroutineScope.watchForStreamsUpdates() = produce {
    var index = BigInteger.ZERO
    while (true) {
      val options = ImmutableQueryOptions.builder().index(index).wait(QUERY_WAIT).build()
      val response = io { kvClient.getConsulResponseWithValues(KEY_PREFIX, options) }

      response.response?.run {
        val asMap = associate { StreamId(it.key.removePrefix(KEY_PREFIX)) to (it.session.orElse(null)) }
        send(Streams(
          mine = asMap.filterValues { it == mySessionId }.keys,
          unowned = asMap.filterValues { it == null }.keys,
        ))
      }
      index = response.index
    }
  }

  private suspend fun renewSession() {
    logger.info("Renewing session: $mySessionId")
    io { sessionClient.renewSession(mySessionId) }
  }

  private suspend fun maybeGrabStreams() {
    val numToGrab = minOf(CAPACITY - streams.mine.size, streams.unowned.size, BATCH_SIZE)
    if (numToGrab > 0) {
      val targets = streams.unowned.shuffled().take(numToGrab)  // Randomise selection
      logger.info("Attempting to grab streams: $targets")
      io { targets.forEach { kvClient.acquireLock("$KEY_PREFIX$it", address.toString(), mySessionId) } }
    }
  }

  private fun handleStreamsUpdate(update: Streams) {
    logger.info("Mine: ${update.mine}")
    (update.mine - streams.mine).forEach { startProcessing(it) }
    (streams.mine - update.mine).forEach { stopProcessing(it) }
    streams = update
  }

  private fun startProcessing(id: StreamId) {
    logger.info("Started processing: $id")
  }

  private fun stopProcessing(id: StreamId) {
    logger.info("Stopped processing: $id")
  }

  private suspend fun <R> io(block: () -> R) = withContext(Dispatchers.IO) { block() }

  private data class StreamId(val value: String) : Comparable<StreamId> {
    override fun compareTo(other: StreamId) = value.compareTo(other.value)
    override fun toString() = value
  }

  private data class Streams(
    val mine: Set<StreamId> = emptySet(),
    val unowned: Set<StreamId> = emptySet(),
  )

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
