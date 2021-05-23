package choliver.xomad.xuota

import choliver.xomad.Address
import choliver.xomad.StreamId
import choliver.xomad.startApp
import choliver.xomad.xuota.ConsulArbitrator.Event.StreamAcquired
import choliver.xomad.xuota.ConsulArbitrator.Event.StreamDropped
import io.ktor.application.*
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import org.slf4j.LoggerFactory

object Xuota {
  private val logger = LoggerFactory.getLogger(javaClass)

  // TODO - wire up call handler to state somehow

  @JvmStatic
  fun main(args: Array<String>) {
    startApp(
      name = javaClass.simpleName,
      onStart = { handleStreams(it) }
    ) {
      get("/prices/{instrument}") {
        when (val instrument = call.parameters["instrument"]) {
          "BTC" -> call.respondText("33885.96")
          "DOGE" -> call.respondText("0.29")
          else -> call.respondText("Unsupported instrument: $instrument", status = NotFound)
        }
      }
    }
  }

  private fun CoroutineScope.handleStreams(address: Address) = launch {
    val streams = mutableSetOf<StreamId>()

    val events = with(ConsulArbitrator(address)) { arbitrateStreams() }

    while (true) {
      select<Unit> {
        events.onReceive { e ->
          when (e) {
            is StreamAcquired -> {
              logger.info("Stream acquired: ${e.id}")
              streams += e.id
            }
            is StreamDropped -> {
              logger.info("Stream dropped: ${e.id}")
              streams -= e.id
            }
          }
        }
      }
    }
  }
}
