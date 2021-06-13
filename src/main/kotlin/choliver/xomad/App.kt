package choliver.xomad

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.CoroutineScope
import org.slf4j.event.Level

fun startApp(
  name: String,
  onStart: CoroutineScope.(Address) -> Unit = {},
  configuration: Route.() -> Unit
) {
  val address = getAddress()
  val server = embeddedServer(Netty, host = address.host, port = address.port) {
    install(CallLogging) {
      level = Level.INFO
      filter { it.request.path() != HEALTHCHECK_PATH }
    }
    install(IgnoreTrailingSlash)

    onStart(address)

    routing {
      get(HEALTHCHECK_PATH) { call.respond(OK) }
      route(getBaseRoute()) {
        get("/") { call.respondText("Hello from $name") }
        configuration()
      }
    }
  }
  server.start(wait = true)
}

fun getBaseRoute() = System.getenv("BASE_ROUTE") ?: "/"

fun getAddress() = System.getenv("NOMAD_ADDR_http")?.let {
  val parts = it.split(":")
  require(parts.size == 2) { "Invalid address: $it" }
  Address(parts[0], parts[1].toInt())
} ?: DEFAULT_ADDRESS


const val HEALTHCHECK_PATH = "/_healthz"
val DEFAULT_ADDRESS = Address("0.0.0.0", 8080)




