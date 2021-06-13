package choliver.xomad.mygrid

import choliver.xomad.startApp
import org.slf4j.LoggerFactory

object MyGrid {
  private val logger = LoggerFactory.getLogger(javaClass)

  // TODO - release session on exit -> should release all locks


  @JvmStatic
  fun main(args: Array<String>) = startApp(name = javaClass.simpleName) {

  }
}
