package choliver.xomad

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.event.Level

fun startApp(
  name: String,
  configuration: Routing.() -> Unit
) {
  val address = getAddress()
  val server = embeddedServer(Netty, host = address.host, port = address.port) {
    install(CallLogging) {
      level = Level.INFO
      filter { it.request.path() != HEALTHCHECK_PATH }
    }

    routing {
      get("/") {
        call.respondText("Hello from $name")
      }

      get(HEALTHCHECK_PATH) {
        call.respond(OK)
      }

      configuration()
    }
  }
  server.start(wait = true)
}

private fun getAddress() = System.getenv("NOMAD_ADDR_http")?.let {
  val parts = it.split(":")
  require(parts.size == 2) { "Invalid address: $it" }
  Address(parts[0], parts[1].toInt())
} ?: DEFAULT_ADDRESS

private data class Address(val host: String, val port: Int)

private const val HEALTHCHECK_PATH = "/_healthz"
private val DEFAULT_ADDRESS = Address("0.0.0.0", 8080)




