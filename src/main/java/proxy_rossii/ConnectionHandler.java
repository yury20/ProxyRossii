package proxy_rossii;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

@Slf4j
public class ConnectionHandler implements Runnable {

    private final ExecutorService pool;

    private final ProxyData proxyData;
    private final Socket clientSocket;
    private Socket serverSocket = null;

    public ConnectionHandler(Socket clientSocket, ProxyData proxyData, ExecutorService pool) {
        this.clientSocket = clientSocket;
        this.proxyData = proxyData;
        this.pool = pool;
    }

    @Override
    @SneakyThrows
    public void run() {
        String clientHost = clientSocket.getInetAddress().getHostName();
        int clientPort = clientSocket.getPort();
        String remoteServerHost = proxyData.getRemoteHost();
        int remoteServerPort = proxyData.getRemotePort();
        log.debug("Server {} starting new connection handler for incoming connection from {}:{}", proxyData.getProxyName(), clientHost, clientPort);

        try {
            serverSocket = new Socket(remoteServerHost, remoteServerPort);
        } catch (IOException exception) {
            log.error("Server {} failed creating socket to {}:{}", proxyData.getProxyName(), remoteServerHost, remoteServerPort, exception);
            clientSocket.close();
            return;
        }

        log.debug("Server {} established proxy connection: {}:{} <<<--->>> {}:{}", proxyData.getProxyName(), clientHost, clientPort, remoteServerHost, remoteServerPort);
        if(!Thread.currentThread().isInterrupted()) {
            pool.execute(new SocketsBridge(clientSocket, serverSocket, proxyData, SocketsBridge.Direction.CLIENT_TO_SERVER));
            pool.execute(new SocketsBridge(serverSocket, clientSocket, proxyData, SocketsBridge.Direction.SERVER_TO_CLIENT));
        } else {
            log.error("Server {} was interrupted! These sockets will be closed immediately: {}:{} XXX---XXX {}:{}", proxyData.getProxyName(), clientHost, clientPort, remoteServerHost, remoteServerPort);
            clientSocket.close();
            serverSocket.close();
        }
    }
}
