package choliver.xomad

import choliver.xomad.Gateway.Event.*
import com.orbitz.consul.Consul
import com.orbitz.consul.model.session.ImmutableSession
import com.orbitz.consul.option.ImmutableQueryOptions
import org.slf4j.LoggerFactory
import java.math.BigInteger

object Gateway {
  private val logger = LoggerFactory.getLogger(javaClass)

  // TODO - release session on exit -> should release all locks
  // TODO - this approach won't scale - M instances * N streams


  @JvmStatic
  fun main(args: Array<String>) = startApp(name = javaClass.simpleName) {
    val client = Consul.builder().build()

    val sessionClient = client.sessionClient()
    val kvClient = client.keyValueClient()

    val session = ImmutableSession.builder()
      .name("foo")
      .build()
    val mySessionId = SessionId(sessionClient.createSession(session).id)

    var streams = mapOf<StreamId, SessionId?>()
    var index = BigInteger.ZERO

    // TODO - initial get


    while (true) {
      val e = getEvent()
      logger.info("Event: $e")

      when (e) {
        is SessionRenewalTimeout -> {
          // TODO - put on IO dispatcher
          sessionClient.renewSession(mySessionId.value)
        }

        is StreamAcquisitionTimeout -> {
          if (streams.count { it.value == mySessionId } < CAPACITY) {
            val id = streams.filterValues { it == null }.keys.random()
            // TODO - move blocking calls to IO dispatcher
            kvClient.acquireLock(lockKey(id), mySessionId.value)
          }
        }

        is StreamsChanged -> {
          // TODO - put on IO dispatcher
          val response = kvClient.getConsulResponseWithValues(
            KEY_PREFIX,
            ImmutableQueryOptions.builder().index(index).build()
          )
          index = response.index

          val newStreams: Map<StreamId, SessionId?> = response.response.associate {
            StreamId(it.key) to (it.session.map(::SessionId).orElseGet(null))
          }

          val myOldStreams = streams.filterValues { it == mySessionId }.keys
          val myNewStreams = newStreams.filterValues { it == mySessionId }.keys
          (myNewStreams - myOldStreams).forEach { startProcessing(it) }
          (myOldStreams - myNewStreams).forEach { stopProcessing(it) }
          streams = newStreams

          logger.info("""
            Status:
              Mine:    ${streams.filterValues { it == mySessionId }.keys}
              Owned:   ${streams.filterValues { it != mySessionId && it != null }.keys}
              Unowned: ${streams.filterValues { it == null }.keys}
          """.trimIndent())
        }
      }
    }
  }

  private fun startProcessing(id: StreamId) {
    logger.info("Started processing: $id")
  }

  private fun stopProcessing(id: StreamId) {
    logger.info("Stopped processing: $id")
  }

  private fun lockKey(id: StreamId) = "$KEY_PREFIX/$id"


  private fun getEvent(): Event {
    return StreamsChanged(42)
  }

  private sealed class Event {
    data class StreamsChanged(val x: Int) : Event()
    object SessionRenewalTimeout : Event()
    object StreamAcquisitionTimeout : Event()
  }

  private data class StreamId(val value: String) {
    override fun toString() = value
  }

  private data class SessionId(val value: String) {
    override fun toString() = value
  }

  private const val CAPACITY = 4
  private const val KEY_PREFIX = "xuota/streams"
}
