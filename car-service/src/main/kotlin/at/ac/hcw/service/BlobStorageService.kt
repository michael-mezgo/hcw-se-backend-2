package at.ac.hcw.service

import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClientBuilder

class BlobStorageService(
    connectionString: String,
    containerName: String,
) {
    private val containerClient: BlobContainerClient =
        BlobServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient()
            .getBlobContainerClient(containerName)
            .also { if (!it.exists()) it.create() }

    fun upload(blobName: String, data: ByteArray, overwrite: Boolean = true) {
        containerClient.getBlobClient(blobName)
            .upload(data.inputStream(), data.size.toLong(), overwrite)
    }

    fun delete(blobName: String): Boolean =
        containerClient.getBlobClient(blobName).deleteIfExists()

    fun list(): List<String> =
        containerClient.listBlobs().map { it.name }

    fun getUrl(blobName: String): String =
        containerClient.getBlobClient(blobName).blobUrl

}