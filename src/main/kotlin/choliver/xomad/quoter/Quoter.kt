package choliver.xomad.quoter

import choliver.xomad.HEALTHCHECK_PATH
import choliver.xomad.StreamId
import choliver.xomad.getAddress
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.event.Level

object Quoter {
  @JvmStatic
  fun main(args: Array<String>) {
    val address = getAddress()
    val server = embeddedServer(Netty, host = address.host, port = address.port) {
      install(CallLogging) {
        level = Level.INFO
        filter { it.request.path() != HEALTHCHECK_PATH }
      }

      val pricer = MultiPricer(address).apply { start() }

      routing {
        get("/") { call.respondText("Hello from ${this@Quoter.javaClass.simpleName}") }

        get(HEALTHCHECK_PATH) { call.respond(OK) }

        get("/prices/{stream}") {
          val streamId = StreamId(call.parameters["stream"]!!)
          when (val result = pricer.getPrice(streamId)) {
            null -> call.respondText("Unsupported stream: $streamId", status = NotFound)
            else -> call.respondText(result)
          }
        }
      }
    }
    server.start(wait = true)
  }
}
