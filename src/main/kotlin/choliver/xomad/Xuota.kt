package choliver.xomad

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.event.Level

object Xuota {
  @JvmStatic
  fun main(args: Array<String>) {
    val server = embeddedServer(Netty, port = 8080) {
      install(CallLogging) {
        level = Level.INFO
      }

      routing {
        get("/") {
          call.respondText("Hello world")
        }
      }
    }

    server.start(wait = true)
  }
}
