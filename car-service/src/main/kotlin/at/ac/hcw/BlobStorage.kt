package at.ac.hcw

import at.ac.hcw.service.BlobStorageService
import io.ktor.server.application.*
import io.ktor.util.*

val CarBlobContainerClientKey = AttributeKey<BlobStorageService>("CarBlobContainerClientKey")

fun Application.configureBlobStorage() {
    val connectionString = environment.config.property("blob.storage.connectionString").getString()
    val containerName = environment.config.property("blob.storage.containerName").getString()
    val service = BlobStorageService(connectionString, containerName)
    attributes.put(CarBlobContainerClientKey, service)
}