package choliver.xomad

data class Address(val host: String, val port: Int) {
  override fun toString() = "${host}:${port}"
}

data class StreamId(val value: String) : Comparable<StreamId> {
  override fun compareTo(other: StreamId) = value.compareTo(other.value)
  override fun toString() = value
}

