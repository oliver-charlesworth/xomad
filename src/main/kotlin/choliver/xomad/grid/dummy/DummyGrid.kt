package choliver.xomad.grid.dummy

import choliver.xomad.HEALTHCHECK_PATH
import choliver.xomad.getAddress
import choliver.xomad.getBaseRoute
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.event.Level

object DummyGrid {
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

      routing {
        get(HEALTHCHECK_PATH) { call.respond(HttpStatusCode.OK) }

        route(getBaseRoute()) {
          get("/") { call.respondText("Hello from ${javaClass.simpleName}") }
        }
      }
    }
    server.start(wait = true)
  }
}
