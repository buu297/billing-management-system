package com.buu297.billing.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;

public class StaticFileHandler implements HttpHandler {
    private final File webRoot;

    public StaticFileHandler(File webRoot) {
        this.webRoot = webRoot;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        String path = uri.getPath();

        if (path == null || path.equals("/") || path.isEmpty()) {
            path = "/index.html";
        }

        File targetFile = new File(webRoot, path);
        if (!targetFile.exists() || targetFile.isDirectory()) {
            String notFound = "<html><body><h1>404 Not Found</h1><p>File not found: " + path + "</p></body></html>";
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(404, notFound.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(notFound.getBytes());
            }
            return;
        }

        String mimeType = Files.probeContentType(targetFile.toPath());
        if (mimeType == null) {
            if (path.endsWith(".css")) mimeType = "text/css";
            else if (path.endsWith(".js")) mimeType = "application/javascript";
            else if (path.endsWith(".html")) mimeType = "text/html; charset=UTF-8";
            else if (path.endsWith(".json")) mimeType = "application/json";
            else if (path.endsWith(".svg")) mimeType = "image/svg+xml";
            else mimeType = "application/octet-stream";
        }

        exchange.getResponseHeaders().set("Content-Type", mimeType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, targetFile.length());

        try (OutputStream os = exchange.getResponseBody();
             FileInputStream fis = new FileInputStream(targetFile)) {
            byte[] buf = new byte[8192];
            int read;
            while ((read = fis.read(buf)) != -1) {
                os.write(buf, 0, read);
            }
        }
    }
}
