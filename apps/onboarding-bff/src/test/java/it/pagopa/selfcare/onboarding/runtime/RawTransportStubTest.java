package it.pagopa.selfcare.onboarding.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RawTransportStubTest {

    @ParameterizedTest
    @ValueSource(strings = {"0", "3"})
    void acceptsContentLength(String length) throws Exception {
        try (RawTransportStub stub = new RawTransportStub()) {
            String response = exchange(stub, "Content-Length: " + length, "0".equals(length) ? "" : "abc");
            assertTrue(response.startsWith("HTTP/1.1 200 OK"));
            assertEquals(1, stub.hits("test").size());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "abc", "10485761", "2147483647", "4294967295"})
    void rejectsInvalidContentLength(String length) throws Exception {
        try (RawTransportStub stub = new RawTransportStub()) {
            assertEquals("", exchange(stub, "Content-Length: " + length, ""));
            assertTrue(stub.hits("test").isEmpty());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "xyz", "a00001", "7ffffffe", "7fffffff", "ffffffff"})
    void rejectsInvalidChunkLength(String length) throws Exception {
        try (RawTransportStub stub = new RawTransportStub()) {
            assertEquals("", exchange(stub, "Transfer-Encoding: chunked", length + "\r\n"));
            assertTrue(stub.hits("test").isEmpty());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"3\r\nabc\r\n0\r\n\r\n", "0\r\n\r\n",
            "3;name=value\r\nabc\r\n0\r\nX-Trailer: value\r\n\r\n"})
    void consumesChunksBeforeTheNextRequest(String body) throws Exception {
        try (RawTransportStub stub = new RawTransportStub()) {
            String next = "GET /test/next HTTP/1.1\r\nHost: localhost\r\n\r\n";
            String response = exchange(stub, "Transfer-Encoding: chunked", body + next);
            assertTrue(response.startsWith("HTTP/1.1 200 OK"));
            assertEquals(2, response.split("HTTP/1.1 200 OK", -1).length - 1);
            assertEquals(List.of("POST", "GET"), stub.hits("test").stream().map(RawTransportStub.Hit::method).toList());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"3\r\nab", "3\r\nabcxx", "3\r\nabc\r\n", "0\r\nX-Trailer: value\r\n", "3\nabc\r\n"})
    void rejectsIncompleteOrMalformedChunks(String body) throws Exception {
        try (RawTransportStub stub = new RawTransportStub()) {
            assertEquals("", exchange(stub, "Transfer-Encoding: chunked", body));
            assertTrue(stub.hits("test").isEmpty());
        }
    }

    @Test
    void rejectsTruncatedContentLength() throws Exception {
        try (RawTransportStub stub = new RawTransportStub()) {
            assertEquals("", exchange(stub, "Content-Length: 3", "ab"));
            assertTrue(stub.hits("test").isEmpty());
        }
    }

    @Test
    void rejectsAnOversizedChunkLine() throws Exception {
        try (RawTransportStub stub = new RawTransportStub()) {
            assertEquals("", exchange(stub, "Transfer-Encoding: chunked", "0".repeat(8192)));
            assertTrue(stub.hits("test").isEmpty());
        }
    }

    @Test
    void appliesTheLimitToTheWholeChunkedBody() throws Exception {
        try (RawTransportStub stub = new RawTransportStub()) {
            String body = "a00000\r\n" + "x".repeat(10 * 1024 * 1024) + "\r\n1\r\n";
            assertEquals("", exchange(stub, "Transfer-Encoding: chunked", body));
            assertTrue(stub.hits("test").isEmpty());
        }
    }

    private static String exchange(RawTransportStub stub, String headers, String body) throws Exception {
        URI uri = URI.create(stub.url("test"));
        try (Socket socket = new Socket(uri.getHost(), uri.getPort())) {
            socket.setSoTimeout(5000);
            String request = "POST /test HTTP/1.1\r\nHost: localhost\r\n" + headers + "\r\n\r\n" + body;
            socket.getOutputStream().write(request.getBytes(StandardCharsets.ISO_8859_1));
            socket.shutdownOutput();
            return new String(socket.getInputStream().readAllBytes(), StandardCharsets.ISO_8859_1);
        }
    }
}
