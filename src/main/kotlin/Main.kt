import java.io.InputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.net.HttpURLConnection
import java.net.URL

const val CHUNK_SIZE = 64 * 1024
const val BUFFER_SIZE = 4096

// Function to send a request and retrieve data by range
fun downloadRange(url: String, start: Int, end: Int): ByteArray {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.setRequestProperty("Range", "bytes=$start-$end")
    connection.connect()

    // Check for successful response
    if (connection.responseCode != HttpURLConnection.HTTP_PARTIAL) {
        throw Exception("Failed to retrieve data, HTTP response code: ${connection.responseCode}")
    }

    // Read data from the stream
    val inputStream: InputStream = connection.inputStream
    val byteArrayOutputStream = ByteArrayOutputStream()

    val buffer = ByteArray(BUFFER_SIZE)
    var bytesRead: Int
    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
        byteArrayOutputStream.write(buffer, 0, bytesRead)
    }

    inputStream.close()
    return byteArrayOutputStream.toByteArray()
}

// Function to calculate the SHA-256 hash
fun calculateSha256(data: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(data)
    return hashBytes.joinToString("") { "%02x".format(it) }
}

// Main function to download all data
fun downloadData(url: String): ByteArray {
    // Get the size of the data
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connect()
    val dataSize = connection.contentLength

    // Download data in chunks
    val chunkSize = CHUNK_SIZE
    val downloadedData = ByteArrayOutputStream()

    var currentLength = 0
    while (currentLength < dataSize) {
        val end = minOf(currentLength + chunkSize, dataSize)

        // Download a chunk of data
        val chunk = downloadRange(url, currentLength, end)
        downloadedData.write(chunk)

        currentLength += chunk.size
        println("Downloaded: $currentLength/$dataSize")
    }

    return downloadedData.toByteArray()
}

fun main() {
    val url = "http://127.0.0.1:8080"  // Server URL

    // 1. Download data
    println("Downloading data...")
    val data = downloadData(url)

    // 2. Calculate the SHA-256 hash of the downloaded data
    val hash = calculateSha256(data)
    println("SHA-256 hash of the downloaded data: $hash")

}