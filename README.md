# Glitchy Server

This project contains a client application written in Kotlin/JVM to download binary data from an unstable HTTP server that supports the `Range` header. The client downloads data in chunks, verifies its integrity using SHA-256, and compares it with the hash printed by the server in the terminal.

## Running the Project

### 1. Start the Server

Run the server in one terminal:

```sh
python3 server/glitchy_server.py
```

Once started, the server will output:

```
Length of data: <data_size>
SHA-256 hash of the data: <data_hash>
Starting HTTP server on port 127.0.0.1:8080
```

Save the hash for verification after downloading the data.

### 2. Run the Client

In another terminal, execute:

```sh
./gradlew build
./gradlew run
```

The client will begin downloading data in chunks and display progress. Once the download is complete, it will calculate the SHA-256 hash and compare it with the server's hash.

## Project Structure

```
.
├── server/
│   ├── glitchy_server.py   # Buggy HTTP server
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   ├── Main.kt      # Main client code
├── build.gradle.kts        # Gradle configuration
├── settings.gradle.kts     # Project settings
├── README.md               # This file
```

## How the Client Works

1. Sends a `GET` request to determine the data size.
2. Downloads data in chunks using the `Range` header.
3. Handles possible partial downloads due to server instability.
4. Verifies the integrity of the downloaded data by comparing the SHA-256 hash with the server’s hash.

## Requirements

- Python 3 (to run the server)
- JDK 17+
- Gradle (no need for a separate installation since the wrapper is included)

## License

This project is licensed under the MIT License.

