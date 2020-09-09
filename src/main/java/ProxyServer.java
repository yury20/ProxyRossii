import interfaces.ProxyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProxyServer implements Runnable {

    public static final Logger LOGGER = LoggerFactory.getLogger(ProxyServer.class);

    private final ProxyConfig config;
    private final ExecutorService pool;

    public ProxyServer(ProxyConfig config) {
        this.config = config;
        pool = Executors.newCachedThreadPool();
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(config.getLocalPort())) { // default 50 connections max in backlog
            LOGGER.info("Server \"" + config.getName() + "\" successfully started!");
            while(true)
                try {
                    Socket clientSocket = serverSocket.accept();
                    clientSocket.setSoTimeout(0);
                    LOGGER.debug("Server " + config.getName() + " accepted incoming connection!");
                    pool.execute(new ConnectionHandler(clientSocket, config, pool));
                } catch (IOException exception) {
                    pool.shutdown();
                    LOGGER.error("Caught an exception during executing " + config.getLocalPort() + " for server " + config.getName(), exception);
                }
        } catch (IOException | IllegalArgumentException exeption) {
            LOGGER.error("Can't create socket on port " + config.getLocalPort() + " for server " + config.getName(), exeption);
        }
    }
}
