package at.ac.hcw

import currency.CurrencyServiceGrpcKt
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import io.ktor.server.application.*
import io.ktor.server.config.tryGetString
import io.ktor.util.*
import java.net.InetSocketAddress

data class CurrencyClient(
    val stub: CurrencyServiceGrpcKt.CurrencyServiceCoroutineStub,
    val apiKey: String
)

val CurrencyClientKey = AttributeKey<CurrencyClient>("CurrencyClient")

fun Application.configureGrpc() {
    val host = environment.config.tryGetString("grpc.currency.host").also {
        if (it == null) log.warn("grpc.currency.host not configured, using default: localhost")
    } ?: "localhost"
    val port = environment.config.tryGetString("grpc.currency.port")?.toInt().also {
        if (it == null) log.warn("grpc.currency.port not configured, using default: 5125")
    } ?: 5125
    val apiKey = environment.config.tryGetString("grpc.currency.apiKey").also {
        if (it == null) log.warn("grpc.currency.apiKey not configured, using empty string")
    } ?: ""

    val channel = NettyChannelBuilder.forAddress(InetSocketAddress(host, port))
        .usePlaintext()
        .build()

    val stub = CurrencyServiceGrpcKt.CurrencyServiceCoroutineStub(channel)
    attributes.put(CurrencyClientKey, CurrencyClient(stub, apiKey))

    monitor.subscribe(ApplicationStopped) {
        channel.shutdown()
    }
}
