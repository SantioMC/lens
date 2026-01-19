package me.santio.lens.prometheus;

import com.sun.net.httpserver.HttpServer;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.UtilityClass;
import me.santio.lens.Lens;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

@UtilityClass
@Getter
@Accessors(fluent = true)
public class PrometheusHttpServer {
    
    private HttpServer httpServer;
    
    /**
     * Starts the Prometheus client's HTTP server
     * @param address The bind address for the http server
     * @param port The port to bind to
     */
    public void start(String address, int port) {
        if (httpServer == null) {
            try {
                httpServer = HttpServer.create(new InetSocketAddress(address, port), 0);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            
            httpServer.createContext("/metrics", exchange -> {
                final byte[] body = Lens.instance().scrape().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.getResponseBody().close();
            });
            
            httpServer.start();
        }
    }

}
