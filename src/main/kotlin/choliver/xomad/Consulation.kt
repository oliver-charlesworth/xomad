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
object Consulation {
  private val logger = LoggerFactory.getLogger(javaClass)

  private val client = Consul.builder().build()
  private val sessionClient = client.sessionClient()
  private val kvClient = client.keyValueClient()

  private val mySessionId by lazy {
    SessionId(sessionClient.createSession(ImmutableSession.builder()
      .name("foo")
      .build()
    ).id).also {
      logger.info("Created session: $it")
    }
  }

  private var streams = mapOf<StreamId, SessionId?>()

  @JvmStatic
  fun main(args: Array<String>) {
    val sessionRenewalTicker = ticker(20_000)
    val streamGrabTicker = ticker(1_000)

    Runtime.getRuntime().addShutdownHook(thread(start = false) {
      logger.info("Destroying session: $mySessionId")
      sessionClient.destroySession(mySessionId.value)
    })

    runBlocking {
      val streamUpdates = watchForStreamUpdates()
      while (true) {
        select<Unit> {
          sessionRenewalTicker.onReceive { renewSession() }
          streamGrabTicker.onReceive { maybeGrabStreams() }
          streamUpdates.onReceive { newStreams -> handleNewStreams(newStreams) }
        }
      }
    }
  }

  private fun CoroutineScope.watchForStreamUpdates() = produce<Map<StreamId, SessionId?>> {
    var index = BigInteger.ZERO
    while (true) {
      val response = kvClient.io {
        getConsulResponseWithValues(KEY_PREFIX,
          ImmutableQueryOptions.builder()
            .index(index)
            .wait("5m")
            .build()
        )
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
    logger.info("Renewing session")
    sessionClient.io { renewSession(mySessionId.value) }
  }

  private suspend fun maybeGrabStreams() {
    val myStreams = streams.filterValues { it == mySessionId }
    val unownedStreams = streams.filterValues { it == null }
    val numToGrab = minOf(CAPACITY - myStreams.size, unownedStreams.size)
    if (numToGrab > 0) {
      val targets = unownedStreams.keys.shuffled().take(numToGrab)
      logger.info("Attempting to grab streams: $targets")
      kvClient.io {
        targets.forEach { acquireLock("$KEY_PREFIX$it", mySessionId.value) }
      }
    }
  }

  private fun handleNewStreams(newStreams: Map<StreamId, SessionId?>) {
    val myOldStreams = streams.filterValues { it == mySessionId }.keys
    val myNewStreams = newStreams.filterValues { it == mySessionId }.keys
    (myNewStreams - myOldStreams).forEach { startProcessing(it) }
    (myOldStreams - myNewStreams).forEach { stopProcessing(it) }
    streams = newStreams
    logStatus()
  }

  private fun logStatus() {
    val mine = streams.filterValues { it == mySessionId }.keys.sorted()
    val owned = streams.filterValues { it != mySessionId && it != null }.keys.sorted()
    val unowned = streams.filterValues { it == null }.keys.sorted()
    logger.info("Mine: $mine, Owned: $owned, Unowned: $unowned")
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

  private const val CAPACITY = 6
  private const val KEY_PREFIX = "xuota/streams/"
}
