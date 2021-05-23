package choliver.xomad

import io.ktor.application.*
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.response.*
import io.ktor.routing.*

object Xuota {
  @JvmStatic
  fun main(args: Array<String>) {
    startApp(
      name = javaClass.simpleName,
      onStart = { address -> with(ConsulArbitrator(address)) { start() } }
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
}
