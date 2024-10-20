package pl.dawidgorecki.clamav;

import org.junit.jupiter.api.Test;
import pl.dawidgorecki.clamav.exception.ClamAVException;
import pl.dawidgorecki.clamav.exception.UnknownCommandException;
import pl.dawidgorecki.clamav.result.ScanResult;
import pl.dawidgorecki.clamav.result.ScanStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ClamAVClientTest {

    @Test
    void sendCommand_returnExpectedResponse_sendCorrectCommand() throws IOException {
        // given
        ClamAVClient underTest = new ClamAVClient("localhost", 3310);
        String commandToSend = "zPING\0";
        String expectedResponse = "PONG";

        // when
        String pong = underTest.sendCommand(commandToSend);

        // then
        assertEquals(pong, expectedResponse);
    }

    @Test
    void sendCommand_throwUnknownCommandException_sendUnknownCommand() throws IOException {
        // given
        ClamAVClient underTest = new ClamAVClient("localhost", 3310);
        String unknownCommand = "WRONGCMD";

        // when & then
        assertThrows(UnknownCommandException.class, () -> underTest.sendCommand(unknownCommand));
    }

    @Test
    void ping_true_serverResponds() {
        // given
        ClamAVClient underTest = new ClamAVClient("localhost", 3310);

        // when
        boolean result = underTest.ping();

        // then
        assertTrue(result);
    }

    @Test
    void ping_false_serverNotResponding() {
        // given
        ClamAVClient underTest = new ClamAVClient("localhost", 3320);

        // when
        boolean result = underTest.ping();

        // then
        assertFalse(result);
    }

    @Test
    void getVersion_returnVersion_serverResponds() {
        // given
        ClamAVClient underTest = new ClamAVClient("localhost", 3310);

        // when
        String version = underTest.getVersion();

        // then
        assertTrue(version.startsWith("ClamAV"));
    }

    @Test
    void getVersion_throwClamAVException_serverNotResponding() {
        // given
        ClamAVClient underTest = new ClamAVClient("localhost", 3320);

        // when & then
        assertThrows(ClamAVException.class, underTest::getVersion);
    }

    @Test
    void scan_passed_sendCleanBytes() {
        // given
        ClamAVClient underTest = new ClamAVClient("localhost", 3310);
        byte[] bytesToScan = "test".getBytes();
        String resultOk = "stream: OK";

        // when
        ScanResult result = underTest.scan(bytesToScan);

        // then
        assertEquals(result.getStatus(), ScanStatus.PASSED);
        assertEquals(result.getResult(), resultOk);
    }

    @Test
    void scan_failed_sendEicarBytes() {
        // given
        ClamAVClient underTest = new ClamAVClient("localhost", 3310);
        byte[] eicar = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*"
                .getBytes(StandardCharsets.US_ASCII);
        String expectedSignature = "Win.Test.EICAR_HDB-1";
        String expectedResult = "stream: Win.Test.EICAR_HDB-1 FOUND";

        // when
        ScanResult result = underTest.scan(eicar);

        // then
        assertEquals(result.getStatus(), ScanStatus.FAILED);
        assertEquals(result.getResult(), expectedResult);
        assertEquals(result.getSignature(), expectedSignature);
    }

    @Test
    void scan_connectionError_serverNotResponding() {
        // given
        ClamAVClient underTest = new ClamAVClient("localhost", 3320);
        byte[] bytesToScan = "test".getBytes();
        String errorResponse = "ClamAV did not respond to ping request";

        // when
        ScanResult result = underTest.scan(bytesToScan);

        // then
        assertEquals(result.getStatus(), ScanStatus.CONNECTION_ERROR);
        assertEquals(result.getResult(), errorResponse);
    }
}