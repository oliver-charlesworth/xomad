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

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
class ConsulArbitrator(
  private val address: Address
) {
  private val logger = LoggerFactory.getLogger(javaClass)

  private val client = Consul.builder().build()
  private val sessionClient = client.sessionClient()
  private val kvClient = client.keyValueClient()

  // TODO - this is gross
  private val mySessionId by lazy {
    SessionId(sessionClient.createSession(ImmutableSession.builder().ttl(SESSION_TTL).build()).id).also {
      logger.info("Created session: $it")
      Runtime.getRuntime().addShutdownHook(thread(start = false) {
        logger.info("Destroying session: $it")
        sessionClient.destroySession(it.value)
      })
    }
  }

  private var streams = mapOf<StreamId, SessionId?>()

  fun CoroutineScope.start() = launch {
    val sessionRenewalTicker = ticker(SESSION_RENEWAL_PERIOD_MILLIS)
    val streamGrabTicker = ticker(STREAM_GRAB_PERIOD_MILLIS)
    val streamUpdates = watchForStreamUpdates()
    while (true) {
      select<Unit> {
        sessionRenewalTicker.onReceive { renewSession() }
        streamGrabTicker.onReceive { maybeGrabStreams() }
        streamUpdates.onReceive { newStreams -> handleNewStreams(newStreams) }
      }
    }
  }

  private fun CoroutineScope.watchForStreamUpdates() = produce<Map<StreamId, SessionId?>> {
    var index = BigInteger.ZERO
    while (true) {
      val response = kvClient.io {
        getConsulResponseWithValues(KEY_PREFIX, ImmutableQueryOptions.builder().index(index).wait(QUERY_WAIT).build())
      }

      index = response.index

      if (response.response != null) {
        send(response.response.associate {
          StreamId(it.key.removePrefix(KEY_PREFIX)) to (it.session.map(::SessionId).orElse(null))
        })
      }
    }
  }

  private suspend fun renewSession() {
    logger.info("Renewing session: $mySessionId")
    sessionClient.io { renewSession(mySessionId.value) }
  }

  private suspend fun maybeGrabStreams() {
    val myStreams = streams.filterValues { it == mySessionId }
    val unownedStreams = streams.filterValues { it == null }
    val numToGrab = minOf(CAPACITY - myStreams.size, unownedStreams.size, BATCH_SIZE)
    if (numToGrab > 0) {
      val targets = unownedStreams.keys.shuffled().take(numToGrab)  // Randomise selection
      logger.info("Attempting to grab streams: $targets")
      kvClient.io {
        targets.forEach {
          acquireLock("$KEY_PREFIX$it", "${address.host}:${address.port}", mySessionId.value)
        }
      }
    }
  }

  private fun handleNewStreams(newStreams: Map<StreamId, SessionId?>) {
    val myOldStreams = streams.filterValues { it == mySessionId }.keys
    val myNewStreams = newStreams.filterValues { it == mySessionId }.keys
    (myNewStreams - myOldStreams).forEach { startProcessing(it) }
    (myOldStreams - myNewStreams).forEach { stopProcessing(it) }
    streams = newStreams
    logger.info("Mine: $myNewStreams")
  }

  private fun startProcessing(id: StreamId) {
    logger.info("Started processing: $id")
  }

  private fun stopProcessing(id: StreamId) {
    logger.info("Stopped processing: $id")
  }

  private suspend fun <T, R> T.io(block: T.() -> R) = withContext(Dispatchers.IO) { block() }

  private data class StreamId(val value: String) : Comparable<StreamId> {
    override fun compareTo(other: StreamId) = value.compareTo(other.value)
    override fun toString() = value
  }

  private data class SessionId(val value: String) : Comparable<SessionId> {
    override fun compareTo(other: SessionId) = value.compareTo(other.value)
    override fun toString() = value
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
