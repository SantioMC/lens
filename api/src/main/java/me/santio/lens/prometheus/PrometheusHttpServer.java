package me.santio.lens.prometheus;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.UtilityClass;
import me.santio.lens.Lens;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

@UtilityClass
@Getter
@Accessors(fluent = true)
public class PrometheusHttpServer {

    private @Nullable HttpServer httpServer;

    /**
     * Starts the Prometheus client's HTTP server
     * @param address The bind address for the http server
     * @param port The port to bind to
     */
    public void start(String address, int port) {
        if (httpServer != null) return;

        try {
            httpServer = HttpServer.create(new InetSocketAddress(address, port), 0);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        httpServer.createContext("/metrics", PrometheusHttpServer::handle);
        httpServer.start();
    }

    /**
     * Stops the Prometheus client's HTTP server, releasing the bound port and its dispatcher thread
     */
    public void stop() {
        if (httpServer == null) return;

        httpServer.stop(0);
        httpServer = null;
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            final byte[] body;

            try {
                body = Lens.instance().scrape().getBytes(StandardCharsets.UTF_8);
            } catch (RuntimeException e) {
                e.printStackTrace();
                exchange.sendResponseHeaders(500, -1);
                return;
            }

            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
        }
    }

}
