package choliver.xomad

fun getBaseRoute() = System.getenv("BASE_ROUTE") ?: "/"

fun getAddress() = System.getenv("NOMAD_ADDR_http")?.let {
  val parts = it.split(":")
  require(parts.size == 2) { "Invalid address: $it" }
  Address(parts[0], parts[1].toInt())
} ?: DEFAULT_ADDRESS


const val HEALTHCHECK_PATH = "/_healthz"
val DEFAULT_ADDRESS = Address("0.0.0.0", 8080)




