package choliver.xomad.quoter

import choliver.xomad.Address
import choliver.xomad.ConsulUtils
import choliver.xomad.ConsulUtils.acquireLock
import choliver.xomad.ConsulUtils.mySessionId
import choliver.xomad.ConsulUtils.renewSession
import choliver.xomad.StreamId
import choliver.xomad.quoter.ConsulArbitrator.Event.StreamAcquired
import choliver.xomad.quoter.ConsulArbitrator.Event.StreamDropped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.selects.select

class ConsulArbitrator(
  private val address: Address
) {
  private var streams = Streams()

  fun CoroutineScope.arbitrateStreams() = produce {
    val sessionRenewalTicker = ticker(SESSION_RENEWAL_PERIOD_MILLIS)
    val streamGrabTicker = ticker(STREAM_GRAB_PERIOD_MILLIS)
    val streamsUpdates = watchForStreamsUpdates()

    while (true) {
      select<Unit> {
        sessionRenewalTicker.onReceive { renewSession() }

        streamGrabTicker.onReceive {
          val numToGrab = minOf(CAPACITY - streams.mine.size, streams.unowned.size, BATCH_SIZE)
          if (numToGrab > 0) {
            val targets = streams.unowned.shuffled().take(numToGrab)  // Randomise selection
            targets.forEach { acquireLock("$KEY_PREFIX$it", "$address") }
          }
        }

        streamsUpdates.onReceive { update ->
          (update.mine - streams.mine).forEach { send(StreamAcquired(it)) }
          (streams.mine - update.mine).forEach { send(StreamDropped(it)) }
          streams = update
        }
      }
    }
  }

  private fun CoroutineScope.watchForStreamsUpdates() = produce {
    ConsulUtils.handleUpdates(KEY_PREFIX) { values ->
      val asMap = values.associate { StreamId(it.key.removePrefix(KEY_PREFIX)) to it.session.orElse(null) }
      send(
        Streams(
          mine = asMap.filterValues { it == mySessionId }.keys,
          unowned = asMap.filterValues { it == null }.keys,
        )
      )
    }
  }

  private data class Streams(
    val mine: Set<StreamId> = emptySet(),
    val unowned: Set<StreamId> = emptySet(),
  )

  sealed class Event {
    data class StreamAcquired(val id: StreamId) : Event()
    data class StreamDropped(val id: StreamId) : Event()
  }

  companion object {
    private const val CAPACITY = 6
    private const val BATCH_SIZE = 2
    private const val SESSION_RENEWAL_PERIOD_MILLIS = 20_000L
    private const val STREAM_GRAB_PERIOD_MILLIS = 1_000L
  }
}
