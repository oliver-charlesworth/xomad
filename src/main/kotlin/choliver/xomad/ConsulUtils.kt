package choliver.xomad

import choliver.xomad.CoroutineUtils.io
import com.orbitz.consul.Consul
import com.orbitz.consul.model.kv.Value
import com.orbitz.consul.model.session.ImmutableSession
import com.orbitz.consul.option.ImmutableQueryOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.produce
import org.slf4j.LoggerFactory
import java.math.BigInteger
import kotlin.concurrent.thread

object ConsulUtils {
  private val logger = LoggerFactory.getLogger(javaClass)

  private val client by lazy { Consul.builder().build() }
  private val kvClient by lazy { client.keyValueClient() }
  private val sessionClient by lazy { client.sessionClient() }

  // TODO - this is gross
  val mySessionId: String by lazy {
    sessionClient.createSession(ImmutableSession.builder().ttl(SESSION_TTL).build()).id.also {
      logger.info("Created session: $it")
      Runtime.getRuntime().addShutdownHook(thread(start = false) {
        logger.info("Destroying session: $it")
        sessionClient.destroySession(it)
      })
    }
  }

  suspend fun renewSession() {
    io { sessionClient.renewSession(mySessionId) }
  }

  suspend fun handleUpdates(key: String, block: suspend (List<Value>) -> Unit) {
    var index = BigInteger.ZERO
    while (true) {
      val options = ImmutableQueryOptions.builder().index(index).wait(QUERY_WAIT).build()
      val response = io { kvClient.getConsulResponseWithValues(key, options) }
      response.response?.run { block(this) }
      index = response.index
    }
  }

  fun CoroutineScope.updatesFor(key: String) = produce {
    var index = BigInteger.ZERO
    while (true) {
      val options = ImmutableQueryOptions.builder().index(index).wait(QUERY_WAIT).build()
      val response = io { kvClient.getConsulResponseWithValues(key, options) }
      response.response?.run { send(this) }
      index = response.index
    }
  }

  suspend fun acquireLock(key: String, value: String) {
    io { kvClient.acquireLock(key, value, mySessionId) }
  }

  private const val QUERY_WAIT = "5m"
  private const val SESSION_TTL = "30s"
}
