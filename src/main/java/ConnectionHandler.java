import interfaces.ProxyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
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
        LOGGER.debug("New ConnectionHandler started for connection incoming from {}:{}", clientHost, clientPort);

        try {
            serverSocket = new Socket(remoteServerHost, remoteServerPort);
        } catch (IOException exception) {
            LOGGER.error("Failed creating socket to {}:{}", remoteServerHost, remoteServerPort, exception);
            return;
        }

        LOGGER.debug("Established proxy connection: {}:{} <-> {}:{}", clientHost, clientPort, remoteServerHost, remoteServerPort);
        pool.execute(new SocketsBridge(clientSocket, serverSocket, config.getDelay()));
        pool.execute(new SocketsBridge(serverSocket, clientSocket, 0));
        pool.execute(() -> {
            while (true) {
                if (clientSocket.isClosed()) {
                    LOGGER.debug("Client socket {}:{} was closed.", clientHost, clientPort);
                    closeRemoteServerSocket();
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }
        });
    }

    private void closeRemoteServerSocket() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                LOGGER.debug("Closing socket to remote server {}:{}", config.getRemoteHost(), config.getRemotePort());
                serverSocket.close();
            } catch (IOException exeption) {
                LOGGER.error("Can't close the remote server socket!", exeption);
            }
        }
    }
}
