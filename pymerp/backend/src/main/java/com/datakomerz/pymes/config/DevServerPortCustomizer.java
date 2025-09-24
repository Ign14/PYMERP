package com.datakomerz.pymes.config;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.OptionalInt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Provides a developer friendly fallback when the configured dev server port is already in
 * use. Instead of failing the application start-up the web server will try to bind to the next
 * available port and log the outcome so the developer can adjust the client configuration.
 */
@Configuration
@Profile("dev")
@ConditionalOnProperty(prefix = "app.server.port-fallback", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DevServerPortCustomizer implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

    private static final Logger log = LoggerFactory.getLogger(DevServerPortCustomizer.class);

    private final int fallbackStartPort;

    public DevServerPortCustomizer(@Value("${app.server.port-fallback.starting-port:0}") int fallbackStartPort) {
        this.fallbackStartPort = fallbackStartPort;
    }

    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        int configuredPort = factory.getPort();
        if (configuredPort <= 0) {
            return;
        }

        if (isPortFree(configuredPort)) {
            return;
        }

        log.warn("Configured dev server port {} is already in use. Searching for a free fallback port...", configuredPort);
        int startingPoint = fallbackStartPort > 0 ? fallbackStartPort : configuredPort + 1;
        OptionalInt availablePort = findAvailablePort(startingPoint);
        if (availablePort.isEmpty()) {
            throw new IllegalStateException("Unable to find an available fallback port for the dev server");
        }

        int selectedPort = availablePort.getAsInt();
        factory.setPort(selectedPort);
        log.warn("Dev server port {} is occupied. Falling back to port {}.", configuredPort, selectedPort);
        log.warn("Remember to point your API clients to http://localhost:{} for this session.", selectedPort);
    }

    private boolean isPortFree(int port) {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(false);
            socket.bind(new java.net.InetSocketAddress((InetAddress) null, port));
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private OptionalInt findAvailablePort(int startFrom) {
        for (int port = Math.max(startFrom, 1024); port <= 65535; port++) {
            if (isPortFree(port)) {
                return OptionalInt.of(port);
            }
        }
        return OptionalInt.empty();
    }
}
