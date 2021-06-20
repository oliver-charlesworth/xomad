package choliver.xomad.grid.streams

import choliver.xomad.ConsulUtils.updatesFor
import choliver.xomad.HEALTHCHECK_PATH
import choliver.xomad.StreamId
import choliver.xomad.getAddress
import choliver.xomad.getBaseRoute
import choliver.xomad.quoter.KEY_PREFIX
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import org.slf4j.event.Level

object StreamsGrid {
  private class StreamTracker {
    private var streams = mapOf<StreamId, String?>()
    private val queries = Channel<CompletableDeferred<Map<StreamId, String?>>>()

    suspend fun getStreams(): Map<StreamId, String?> {
      val result = CompletableDeferred<Map<StreamId, String?>>()
      queries.send(result)
      return result.await()
    }

    fun CoroutineScope.start() = launch {
      val updates = updatesFor(KEY_PREFIX)

      while (true) {
        select<Unit> {
          updates.onReceive { values ->
            streams = values.associate {
              val id = StreamId(it.key.removePrefix(KEY_PREFIX))
              val address = if (it.session.isPresent) it.valueAsString.orElse(null) else null
              id to address
            }
          }
          queries.onReceive { it.complete(streams) }
        }
      }
    }
  }

  @JvmStatic
  fun main(args: Array<String>) {
    val address = getAddress()
    val server = embeddedServer(Netty, host = address.host, port = address.port) {
      install(CallLogging) {
        level = Level.INFO
        filter { it.request.path() != HEALTHCHECK_PATH }
      }
      install(ContentNegotiation) { jackson() }
      install(IgnoreTrailingSlash)

      val tracker = StreamTracker().apply { start() }

      routing {
        get(HEALTHCHECK_PATH) { call.respond(HttpStatusCode.OK) }

        route(getBaseRoute()) {
          get("/") { call.respond(tracker.getStreams()) }
        }
      }
    }
    server.start(wait = true)
  }
}
