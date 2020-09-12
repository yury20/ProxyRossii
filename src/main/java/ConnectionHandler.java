import interfaces.ProxyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;

public class ConnectionHandler implements Runnable {

    public static final Logger LOGGER = LoggerFactory.getLogger(ConnectionHandler.class);

    private final ExecutorService pool;

    private final ProxyConfig config;
    private final Socket clientSocket;
    private Socket serverSocket = null;

    public ConnectionHandler(Socket clientSocket, ProxyConfig config, ExecutorService pool) {
        this.clientSocket = clientSocket;
        this.config = config;
        this.pool = pool;
    }

    @Override
    public void run() {
        String clientHost = clientSocket.getInetAddress().getHostName();
        int clientPort = clientSocket.getPort();
        String remoteServerHost = config.getRemoteHost();
        int remoteServerPort = config.getRemotePort();
        LOGGER.debug("Server {} starting new connection handler for incoming connection from {}:{}", config.getName(), clientHost, clientPort);

        try {
            serverSocket = new Socket(remoteServerHost, remoteServerPort);
        } catch (IOException exception) {
            LOGGER.error("Server {} failed creating socket to {}:{}", config.getName(), remoteServerHost, remoteServerPort, exception);
            return;
        }

        LOGGER.debug("Server {} established proxy connection: {}:{} <<<--->>> {}:{}", config.getName(), clientHost, clientPort, remoteServerHost, remoteServerPort);
        pool.execute(new SocketsBridge(clientSocket, serverSocket, 0, config.getName()));
        pool.execute(new SocketsBridge(serverSocket, clientSocket, config.getDelay(), config.getName()));
    }
}
