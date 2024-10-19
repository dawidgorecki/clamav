package pl.dawidgorecki.clamav;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.dawidgorecki.clamav.exception.ClamAVException;
import pl.dawidgorecki.clamav.exception.UnknownCommandException;
import pl.dawidgorecki.clamav.result.ScanResult;
import pl.dawidgorecki.clamav.result.ScanStatus;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ClamAVClient {
    private static final Logger logger = LoggerFactory.getLogger(ClamAVClient.class);
    private static final int CHUNK_SIZE = 2048;
    private static final int CONNECTION_TIMEOUT = 3000;
    private static final int READ_TIMEOUT = 15000;
    private static final String RESPONSE_OK = "stream: OK";
    private static final String UNKNOWN_COMMAND = "UNKNOWN COMMAND";
    private static final String PONG = "PONG";
    private static final String STREAM_PREFIX = "stream:";
    private static final String FOUND_SUFFIX = "FOUND";
    private static final String ERROR_SUFFIX = "ERROR";

    private final String host;
    private final int port;
    private final int connectionTimeout;
    private final int readTimeout;

    public ClamAVClient(String host, int port, int connectionTimeout, int readTimeout) {
        this.host = host;
        this.port = port;
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
    }

    public ClamAVClient(String host, int port, int connectionTimeout) {
        this(host, port, connectionTimeout, READ_TIMEOUT);
    }

    public ClamAVClient(String host, int port) {
        this(host, port, CONNECTION_TIMEOUT, READ_TIMEOUT);
    }

    /**
     * Sends a command to the ClamAV server via a TCP socket and returns the response.
     * <p>
     * This method connects to the server at the specified host and port, sends a command
     * as a string, and then receives the response. The connection is automatically closed
     * after the operation completes.
     * <p>
     * It's recommended to prefix command with the letter z (eg. zPING)
     * to indicate that the command will be delimited by a NULL character.
     * </p>
     *
     * @param command The command to be sent to the server.
     * @return The server's response as a string (with leading and trailing whitespace trimmed).
     * @throws IOException             If an I/O error occurs, such as connection issues, or failure to send/receive data.
     * @throws UnknownCommandException If the server returns "UNKNOWN COMMAND", indicating that the command was not recognized.
     * @see Socket
     * @see DataOutputStream
     * @see InputStream
     */
    public String sendCommand(String command) throws IOException {
        String response = "";

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), connectionTimeout);
            socket.setSoTimeout(readTimeout);

            try (DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {
                output.writeBytes(command);
                output.flush();

                InputStream input = socket.getInputStream();
                byte[] buffer = new byte[CHUNK_SIZE];
                int nRead;

                while ((nRead = input.read(buffer)) != -1) {
                    response = new String(buffer, 0, nRead);
                }
            }
        }

        String result = response.trim();
        logger.debug("Response from [{}:{}]: {}", host, port, response);

        if (UNKNOWN_COMMAND.equals(result)) {
            throw new UnknownCommandException("Command '" + command + "' was not recognized");
        }

        return result;
    }

    /**
     * Sends a ping command to the ClamAV server to check if it is reachable and responsive.
     * <p>
     * This method sends the "PING" command to the server using the {@link #sendCommand(String)} method
     * and expects a "PONG" response. If an exception occurs, the method returns false, indicating
     * that the server is not reachable.
     * </p>
     *
     * @return {@code true} if the server responds with "PONG", otherwise {@code false}.
     * @see #sendCommand(String)
     */
    public boolean ping() {
        try {
            String result = sendCommand("zPING\0");
            return result.equalsIgnoreCase(PONG);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Retrieves the version of the ClamAV server by sending the "VERSION" command.
     *
     * @return The version string returned by the ClamAV server.
     * @throws ClamAVException If there is an issue sending the "VERSION" command or processing the response.
     */
    public String getVersion() {
        try {
            return sendCommand("zVERSION\0");
        } catch (Exception e) {
            throw new ClamAVException("Failed to retrieve ClamAV version: " +
                    "Error occurred while sending 'VERSION' command.", e);
        }
    }

    /**
     * Scans the provided data stream using the ClamAV server.
     *
     * @param stream The input stream containing data to be scanned.
     * @return The result of the scan as a {@link ScanResult}, including status and any detected signature.
     */
    public ScanResult scan(InputStream stream) {
        if (!ping()) {
            return new ScanResult(ScanStatus.CONNECTION_ERROR, "ClamAV did not respond to ping request");
        }

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), connectionTimeout);
            socket.setSoTimeout(readTimeout);

            try (OutputStream output = new BufferedOutputStream(socket.getOutputStream())) {
                output.write("zINSTREAM\0".getBytes(StandardCharsets.UTF_8));
                output.flush();

                byte[] buffer = new byte[CHUNK_SIZE];

                try (InputStream input = socket.getInputStream()) {
                    int read = stream.read(buffer);

                    while (read >= 0) {
                        byte[] chunkSize = ByteBuffer.allocate(4).putInt(read).array();
                        output.write(chunkSize);
                        output.write(buffer, 0, read);

                        if (input.available() == 0) {
                            // reply from server before scan command has been terminated
                            byte[] reply = input.readAllBytes();
                            throw new IOException("Scan command has been terminated. Reply from server: " +
                                    new String(reply, StandardCharsets.UTF_8));
                        }

                        read = stream.read(buffer);
                    }

                    // terminate scan
                    output.write(new byte[]{0, 0, 0, 0});
                    output.flush();

                    String result = new String(input.readAllBytes()).trim();
                    return getScanResult(result);
                }
            }
        } catch (Exception e) {
            logger.error("An error occurred while scanning file: {}", e.getMessage(), e);
            return new ScanResult(ScanStatus.ERROR, e.getMessage());
        }
    }

    /**
     * Scans the provided byte array using the ClamAV server.
     *
     * @param fileBytes The byte array representing the data to be scanned.
     * @return The result of the scan as a {@link ScanResult}.
     */
    public ScanResult scan(byte[] fileBytes) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(fileBytes);
        return scan(byteArrayInputStream);
    }

    /**
     * Scans the specified file using the ClamAV server.
     *
     * @param file The file to be scanned.
     * @return The result of the scan as a {@link ScanResult}.
     * @throws FileNotFoundException If file does not exist
     */
    public ScanResult scan(File file) throws FileNotFoundException {
        FileInputStream fileInputStream = new FileInputStream(file);
        return scan(fileInputStream);
    }

    private static ScanResult getScanResult(String result) {
        if (result.startsWith("INSTREAM size limit exceeded")) {
            throw new ClamAVException("Clamd size limit exceeded");
        }

        ScanResult scanResult = new ScanResult(ScanStatus.FAILED, result);

        if (result.isEmpty() || result.endsWith(ERROR_SUFFIX)) {
            scanResult.setStatus(ScanStatus.ERROR);
        } else if (RESPONSE_OK.equals(result)) {
            scanResult.setStatus(ScanStatus.PASSED);
        } else if (result.endsWith(FOUND_SUFFIX)) {
            scanResult.setSignature(
                    result.substring(
                            STREAM_PREFIX.length(),
                            result.lastIndexOf(FOUND_SUFFIX) - 1
                    ).trim()
            );
        }

        return scanResult;
    }
}
