package at.ac.hcw

import currency.CurrencyServiceGrpcKt
import io.grpc.ManagedChannelBuilder
import io.ktor.server.application.*
import io.ktor.server.config.tryGetString
import io.ktor.util.*

val CurrencyClientKey = AttributeKey<CurrencyServiceGrpcKt.CurrencyServiceCoroutineStub>("CurrencyClient")

fun Application.configureGrpc() {
    val host = environment.config.tryGetString("grpc.currency.host") ?: "localhost"
    val port = environment.config.tryGetString("grpc.currency.port")?.toInt() ?: 5001

    val channel = ManagedChannelBuilder.forAddress(host, port)
        .usePlaintext()
        .build()

    val stub = CurrencyServiceGrpcKt.CurrencyServiceCoroutineStub(channel)
    attributes.put(CurrencyClientKey, stub)

    monitor.subscribe(ApplicationStopped) {
        channel.shutdown()
    }
}
