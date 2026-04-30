package at.ac.hcw

import currency.CurrencyServiceGrpcKt
import io.grpc.ManagedChannelBuilder
import io.ktor.server.application.*
import io.ktor.server.config.tryGetString
import io.ktor.util.*

data class CurrencyClient(
    val stub: CurrencyServiceGrpcKt.CurrencyServiceCoroutineStub,
    val apiKey: String
)

val CurrencyClientKey = AttributeKey<CurrencyClient>("CurrencyClient")

fun Application.configureGrpc() {
    val host = environment.config.tryGetString("grpc.currency.host") ?: "localhost"
    val port = environment.config.tryGetString("grpc.currency.port")?.toInt() ?: 5125
    val apiKey = environment.config.tryGetString("grpc.currency.apiKey") ?: ""

    val channel = ManagedChannelBuilder.forAddress(host, port)
        .usePlaintext()
        .build()

    val stub = CurrencyServiceGrpcKt.CurrencyServiceCoroutineStub(channel)
    attributes.put(CurrencyClientKey, CurrencyClient(stub, apiKey))

    monitor.subscribe(ApplicationStopped) {
        channel.shutdown()
    }
}
