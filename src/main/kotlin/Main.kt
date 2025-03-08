import java.io.InputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.net.Socket

const val BUFFER_SIZE = 64 * 1024

/**
 * Calculates the SHA-256 hash of the provided byte array.
 *
 * @param data The byte array to hash.
 * @return A hexadecimal string representing the SHA-256 hash.
 */
fun calculateSha256(data: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(data)
    return hashBytes.joinToString("") { "%02x".format(it) }
}

/**
 * Reads an HTTP response from the input stream, separating headers and body.
 *
 * @param inputStream The input stream from which to read the response.
 * @return A pair containing the list of headers and the body as a byte array.
 * @throws Exception If no data is received or the response format is invalid.
 */
fun readResponse(inputStream: InputStream): Pair<List<String>, ByteArray> {
    val buffer = ByteArray(BUFFER_SIZE)
    val bytesRead = inputStream.read(buffer)
    if (bytesRead == -1) throw Exception("No data received")

    val response = String(buffer, 0, bytesRead)
    val headerEnd = response.indexOf("\r\n\r\n")
    if (headerEnd == -1) throw Exception("Invalid response")

    val headers = response.substring(0, headerEnd).split("\r\n")
    val contentLength = headers.find { it.lowercase().startsWith("content-length:") }
        ?.split(":")?.get(1)?.trim()?.toInt() ?: 0

    // The body starts after the double CRLF (\r\n\r\n), which is 4 bytes
    val bodyStart = headerEnd + 4
    val initialBody = buffer.copyOfRange(bodyStart, bytesRead)
    val bodyOutput = ByteArrayOutputStream()
    bodyOutput.write(initialBody)

    // Read remaining body bytes if the initial read didn't capture everything
    var remaining = contentLength - initialBody.size
    while (remaining > 0) {
        val moreBytes = inputStream.read(buffer)
        if (moreBytes == -1) break // Server may close connection early
        bodyOutput.write(buffer, 0, moreBytes)
        remaining -= moreBytes
    }

    val body = bodyOutput.toByteArray()
    return Pair(headers, body)
}

/**
 * Retrieves the total size of the data and the first chunk from the server.
 *
 * @param host The server host address.
 * @param port The server port number.
 * @return A pair of the total data size and the first chunk as a byte array.
 */
fun getLength(host: String, port: Int): Pair<Int, ByteArray> {
    val socket = Socket(host, port)
    val outputStream = socket.getOutputStream()
    val inputStream = socket.getInputStream()
    val request = "GET / HTTP/1.1\r\nHost: 127.0.0.1\r\n\r\n"
    outputStream.write(request.toByteArray())
    outputStream.flush()

    val (headers, body) = readResponse(inputStream)
    val contentLength = headers.find { it.lowercase().startsWith("content-length:") }
        ?.split(":")?.get(1)?.trim()?.toInt() ?: 0
    println("Length: $contentLength")

    inputStream.close()
    outputStream.close()
    socket.close()

    return Pair(contentLength, body)
}

/**
 * Downloads data from the server using HTTP range requests.
 *
 * @param host The server host address.
 * @param port The server port number.
 * @return The complete downloaded data as a byte array.
 */
fun downloadData(host: String, port: Int): ByteArray {
    val (totalSize, firstChunk) = getLength(host, port)
    val downloadedData = ByteArrayOutputStream()
    downloadedData.write(firstChunk)
    var currentLength = firstChunk.size

    while (currentLength < totalSize) {
        val socket = Socket(host, port)
        val outputStream = socket.getOutputStream()
        val inputStream = socket.getInputStream()

        // Range request for remaining data
        val request = "GET / HTTP/1.1\r\nHost: 127.0.0.1\r\nRange: bytes=$currentLength-$totalSize\r\n\r\n"
        outputStream.write(request.toByteArray())
        outputStream.flush()

        val (_, bytesRead) = readResponse(inputStream)
        downloadedData.write(bytesRead)

        currentLength += bytesRead.size
        println("Downloaded: $currentLength/$totalSize")

        inputStream.close()
        outputStream.close()
        socket.close()
    }
    return downloadedData.toByteArray()
}

fun main() {
    val host = "127.0.0.1"
    val port = 8080

    println("Downloading data...")
    val data = downloadData(host, port)

    val hash = calculateSha256(data)
    println("SHA-256 hash of the downloaded data: $hash")
}