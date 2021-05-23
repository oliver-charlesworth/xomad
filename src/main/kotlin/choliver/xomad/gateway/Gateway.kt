package choliver.xomad.gateway

import choliver.xomad.startApp
import org.slf4j.LoggerFactory

object Gateway {
  private val logger = LoggerFactory.getLogger(javaClass)

  // TODO - release session on exit -> should release all locks


  @JvmStatic
  fun main(args: Array<String>) = startApp(name = javaClass.simpleName) {



  }
}
