package projects.ankit;

import projects.ankit.util.SignatureUtil;

import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RequestHandler implements Runnable {

    private static final String CRLF = "\r\n";
    private final Socket socket;
    private final BucketManager bucketManager = new BucketManager();
    private final ObjectStore objectStore = new ObjectStore();
    private final MetadataStore metadataStore = new MetadataStore();

    public RequestHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (InputStream in = socket.getInputStream(); OutputStream out = socket.getOutputStream()) {

            // --- 1. Read headers as raw bytes until CRLFCRLF or EOF ---
            ByteArrayOutputStream headerBuf = new ByteArrayOutputStream();
            int prev3 = -1, prev2 = -1, prev1 = -1, curr;
            while ((curr = in.read()) != -1) {
                headerBuf.write(curr);
                if (prev3 == 13 && prev2 == 10 && prev1 == 13 && curr == 10)
                    break; // CRLFCRLF found
                prev3 = prev2; prev2 = prev1; prev1 = curr;
            }

            if (headerBuf.size() == 0) return; // no request received

            // --- 2. Parse header text ---
            String headerText = headerBuf.toString(StandardCharsets.UTF_8);
            String[] headerLines = headerText.split("\r\n");

            StringTokenizer tokenizer = new StringTokenizer(headerLines[0]);
            String method = tokenizer.nextToken();
            String fullPath = tokenizer.nextToken();
            String path = fullPath.split("\\?")[0];

            Map<String, String> query = parseQuery(fullPath);

            // --- 3. Validate pre-signed URL if present ---
            if (query.containsKey("expires") && query.containsKey("signature")) {
                if (!SignatureUtil.isValid(method, path, query)) {
                    writeResponse(out, 403, "Invalid or expired signature");
                    return;
                }
            }

            // --- 4. Extract bucket and key ---
            String[] parts = path.split("/");
            if (parts.length < 3) {
                writeResponse(out, 400, "Invalid path");
                return;
            }

            String bucket = parts[2];
            String key = parts.length > 3 ? String.join("/", Arrays.copyOfRange(parts, 3, parts.length)) : null;

            // --- 5. Determine body transfer mode ---
            int contentLength = -1;
            boolean isChunked = false;
            for (String line : headerLines) {
                String lower = line.toLowerCase();
                if (lower.startsWith("content-length:")) {
                    contentLength = Integer.parseInt(line.split(":", 2)[1].trim());
                }
                if (lower.startsWith("transfer-encoding:") && lower.contains("chunked")) {
                    isChunked = true;
                }
            }

            // --- 6. Dispatch by HTTP method ---
            switch (method) {
                case "PUT" -> handlePut(bucket, key, in, out, contentLength, isChunked);
                case "GET" -> handleGet(bucket, key, out);
                case "DELETE" -> handleDelete(bucket, key, out);
                default -> writeResponse(out, 405, "Method Not Allowed");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    // --- Utility to parse query parameters ---
    private Map<String, String> parseQuery(String fullPath) {
        Map<String, String> map = new HashMap<>();
        if (!fullPath.contains("?")) return map;
        String query = fullPath.substring(fullPath.indexOf('?') + 1);
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=");
            if (kv.length == 2)
                map.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
        }
        return map;
    }

    // --- PUT handler: supports both Content-Length and chunked ---
    private void handlePut(String bucket, String key, InputStream in, OutputStream out,
                           int contentLength, boolean isChunked) throws IOException {

        if (!bucketManager.ensureBucket(bucket)) {
            writeResponse(out, 500, "Failed to create or find bucket");
            return;
        }

        File file = new File("data/" + bucket + "/" + key);
        file.getParentFile().mkdirs();

        System.out.println("handlePut: key=" + key + ", contentLength=" + contentLength + ", chunked=" + isChunked);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            if (isChunked) {
                readChunkedBody(in, fos);
            } else if (contentLength > 0) {
                readFixedBody(in, fos, contentLength);
            } else {
                readUntilClose(in, fos);
            }
        }

        metadataStore.add(bucket, key);
        writeResponse(out, 200, "Uploaded");
        System.out.println("PUT completed: " + file.getAbsolutePath());
    }

    // --- GET handler ---
    private void handleGet(String bucket, String key, OutputStream out) throws IOException {
        if ("list".equals(key)) {
            String list = metadataStore.list(bucket);
            writeResponse(out, 200, list);
            return;
        }

        File file = objectStore.load(bucket, key);
        if (!file.exists()) {
            writeResponse(out, 404, "Not Found");
            return;
        }
        writeFileResponse(out, file);
        System.out.println("GET completed: " + file.getAbsolutePath());
    }

    // --- DELETE handler ---
    private void handleDelete(String bucket, String key, OutputStream out) throws IOException {
        objectStore.delete(bucket, key);
        metadataStore.remove(bucket, key);
        writeResponse(out, 200, "Deleted");
        System.out.println("DELETE completed: " + bucket + "/" + key);
    }

    // --- response builders ---
    private void writeResponse(OutputStream out, int code, String message) throws IOException {
        String resp = "HTTP/1.1 " + code + " OK" + CRLF +
                "Content-Type: text/plain" + CRLF +
                "Content-Length: " + message.getBytes().length + CRLF + CRLF +
                message;
        out.write(resp.getBytes());
    }

    private void writeFileResponse(OutputStream out, File file) throws IOException {
        String header = "HTTP/1.1 200 OK" + CRLF +
                "Content-Length: " + file.length() + CRLF + CRLF;
        out.write(header.getBytes());
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.transferTo(out);
        }
    }

    // --- body readers ---
    private void readFixedBody(InputStream in, FileOutputStream fos, int contentLength) throws IOException {
        byte[] buffer = new byte[8192];
        int total = 0;
        while (total < contentLength) {
            int read = in.read(buffer, 0, Math.min(buffer.length, contentLength - total));
            if (read == -1) break;
            fos.write(buffer, 0, read);
            total += read;
        }
        System.out.println("readFixedBody done: " + total + " bytes");
    }

    private void readUntilClose(InputStream in, FileOutputStream fos) throws IOException {
        byte[] buffer = new byte[8192];
        int read, total = 0;
        while ((read = in.read(buffer)) != -1) {
            fos.write(buffer, 0, read);
            total += read;
        }
        System.out.println("readUntilClose done: " + total + " bytes");
    }

    private void readChunkedBody(InputStream in, FileOutputStream fos) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.US_ASCII));
        int total = 0;
        while (true) {
            String sizeLine = br.readLine();
            if (sizeLine == null) break;
            int size = Integer.parseInt(sizeLine.trim(), 16);
            if (size == 0) {
                br.readLine(); // consume final CRLF
                break;
            }
            byte[] chunk = in.readNBytes(size);
            fos.write(chunk);
            total += size;
            br.readLine(); // consume CRLF after each chunk
        }
        System.out.println("readChunkedBody done: " + total + " bytes");
    }
}

