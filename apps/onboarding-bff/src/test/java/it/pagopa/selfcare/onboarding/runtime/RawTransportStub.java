package it.pagopa.selfcare.onboarding.runtime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Raw-socket downstream able to break the transport in the ways an HTTP server framework cannot:
 * it answers the first path segment of a request as a "service" and, for the failing requests of
 * that service, drops the connection at a precise point of the exchange. Every request that reaches
 * a service is recorded with the connection that carried it.
 */
final class RawTransportStub implements AutoCloseable {

    enum Failure {
        /** Reads the whole request, then closes the connection without answering. */
        CLOSE,
        /** Reads the whole request, then resets the connection. */
        RESET,
        /** Starts the status line and closes the connection. */
        TRUNCATED_HEAD,
        /** Sends complete headers announcing a longer body, a part of it, and closes. */
        TRUNCATED_BODY,
        /** Answers bytes that are not HTTP. */
        GARBAGE,
        /** Reads the whole request and then never answers (until the stub is closed). */
        HANG
    }

    record Hit(String method, String path, int connection, long at) {}

    private static final byte[] DEFAULT_ANSWER = "{\"id\":\"prod-io\"}".getBytes(StandardCharsets.UTF_8);
    private static final int MAX_BODY_BYTES = 10 * 1024 * 1024;
    private static final int MAX_CHUNK_LINE_BYTES = 8192;

    private final Map<String, byte[]> answers = new ConcurrentHashMap<>();
    private final ServerSocket server;
    private final AtomicInteger connections = new AtomicInteger();
    private final Map<String, List<Hit>> hits = new ConcurrentHashMap<>();
    private final Map<String, Failure> failures = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> failuresLeft = new ConcurrentHashMap<>();
    private final List<Socket> sockets = new CopyOnWriteArrayList<>();

    RawTransportStub() {
        try {
            server = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        Thread acceptor = new Thread(this::acceptLoop, "raw-transport-stub");
        acceptor.setDaemon(true);
        acceptor.start();
    }

    String url(String service) {
        return "http://127.0.0.1:" + server.getLocalPort() + "/" + service;
    }

    /** The JSON the service answers with when a request is not failed. */
    void answer(String service, String json) {
        answers.put(service, json.getBytes(StandardCharsets.UTF_8));
    }

    void reset() {
        hits.clear();
        failures.clear();
        failuresLeft.clear();
    }

    /** The next {@code requests} requests of the service fail that way, the following ones are answered. */
    void failNext(String service, Failure failure, int requests) {
        failures.put(service, failure);
        failuresLeft.put(service, new AtomicInteger(requests));
    }

    void failAlways(String service, Failure failure) {
        failNext(service, failure, Integer.MAX_VALUE);
    }

    List<Hit> hits(String service) {
        return List.copyOf(hits.getOrDefault(service, List.of()));
    }

    @Override
    public void close() {
        try {
            server.close();
            for (Socket socket : sockets) {
                socket.close();
            }
        } catch (IOException ignored) {
            // best effort on shutdown
        }
    }

    private void acceptLoop() {
        while (!server.isClosed()) {
            try {
                Socket socket = server.accept();
                sockets.add(socket);
                int connection = connections.incrementAndGet();
                Thread worker = new Thread(() -> serve(socket, connection), "raw-transport-stub-" + connection);
                worker.setDaemon(true);
                worker.start();
            } catch (IOException e) {
                return;
            }
        }
    }

    private void serve(Socket socket, int connection) {
        try {
            socket.setTcpNoDelay(true);
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            while (true) {
                String head = readHead(in);
                if (head == null) {
                    socket.close();
                    return;
                }
                skipBody(in, head);
                String[] requestLine = head.substring(0, head.indexOf("\r\n")).split(" ");
                String service = requestLine[1].split("/")[1];
                hits.computeIfAbsent(service, k -> new CopyOnWriteArrayList<>())
                        .add(new Hit(requestLine[0], requestLine[1], connection, System.currentTimeMillis()));
                Failure failure = failureOf(service);
                if (failure == null) {
                    byte[] body = answers.getOrDefault(service, DEFAULT_ANSWER);
                    out.write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: "
                            + body.length + "\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
                    if (!"HEAD".equals(requestLine[0])) {
                        out.write(body);
                    }
                    out.flush();
                } else if (!fail(socket, out, failure)) {
                    return;
                }
            }
        } catch (IOException e) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // already closed
            }
        }
    }

    private Failure failureOf(String service) {
        Failure failure = failures.get(service);
        AtomicInteger left = failuresLeft.get(service);
        return failure != null && left != null && left.getAndDecrement() > 0 ? failure : null;
    }

    /** Applies the failure; false when the connection is gone and the serving loop must end. */
    private boolean fail(Socket socket, OutputStream out, Failure failure) throws IOException {
        switch (failure) {
            case CLOSE -> socket.close();
            case RESET -> {
                socket.setSoLinger(true, 0);
                socket.close();
            }
            case TRUNCATED_HEAD -> {
                out.write("HTTP/1.1 200 OK\r\nContent-".getBytes(StandardCharsets.ISO_8859_1));
                out.flush();
                socket.close();
            }
            case TRUNCATED_BODY -> {
                out.write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: 100\r\n\r\n{\"id\":")
                        .getBytes(StandardCharsets.ISO_8859_1));
                out.flush();
                socket.close();
            }
            case GARBAGE -> {
                out.write("GARBAGE NOT HTTP\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
                out.flush();
                socket.close();
            }
            case HANG -> {
                try {
                    Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                socket.close();
            }
        }
        return false;
    }

    private static String readHead(InputStream in) throws IOException {
        ByteArrayOutputStream head = new ByteArrayOutputStream();
        int matched = 0;
        int c;
        while ((c = in.read()) != -1) {
            head.write(c);
            matched = c == "\r\n\r\n".charAt(matched) ? matched + 1 : c == '\r' ? 1 : 0;
            if (matched == 4) {
                return head.toString(StandardCharsets.ISO_8859_1);
            }
        }
        return null;
    }

    private static void skipBody(InputStream in, String head) throws IOException {
        String lower = head.toLowerCase(Locale.ROOT);
        int length = lower.indexOf("content-length:");
        if (length >= 0) {
            in.skipNBytes(bodyLength(lower.substring(length + 15, lower.indexOf("\r\n", length)).trim(), 10));
        } else if (lower.contains("transfer-encoding: chunked")) {
            int remaining = MAX_BODY_BYTES;
            while (true) {
                String line = readChunkLine(in);
                int extension = line.indexOf(';');
                int size = bodyLength((extension < 0 ? line : line.substring(0, extension)).trim(), 16);
                if (size > remaining) {
                    throw new IOException("Chunked body exceeds the stub body limit");
                }
                remaining -= size;
                if (size == 0) {
                    while (!readChunkLine(in).isEmpty()) {
                        // Consume trailers before the next request on the same connection.
                    }
                    return;
                }
                in.skipNBytes(size);
                if (in.read() != '\r' || in.read() != '\n') {
                    throw new IOException("Missing chunk terminator");
                }
            }
        }
    }

    private static int bodyLength(String value, int radix) throws IOException {
        try {
            int length = Integer.parseInt(value, radix);
            if (length < 0 || length > MAX_BODY_BYTES) {
                throw new IOException("Body exceeds the stub body limit");
            }
            return length;
        } catch (NumberFormatException e) {
            throw new IOException("Invalid body length", e);
        }
    }

    private static String readChunkLine(InputStream in) throws IOException {
        StringBuilder line = new StringBuilder();
        while (line.length() < MAX_CHUNK_LINE_BYTES) {
            int c = in.read();
            if (c == -1) {
                throw new IOException("Closed inside a chunked body");
            }
            if (c == '\r') {
                if (in.read() != '\n') {
                    throw new IOException("Missing chunk line terminator");
                }
                return line.toString();
            }
            line.append((char) c);
        }
        throw new IOException("Chunk line exceeds the stub limit");
    }
}
