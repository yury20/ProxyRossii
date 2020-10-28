package proxy_rossii;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class ProxyServer extends Thread {

    @Autowired
    private ProxyController proxyController;

    @Getter private final ProxyData proxyData;
    private ServerSocket serverSocket;
    private boolean isRunning;
    private boolean isStopped;
    private final ExecutorService pool = Executors.newCachedThreadPool();

    ProxyServer(ProxyData proxyData) {
        super("Thread-" + proxyData.getProxyName());
        this.proxyData = proxyData;
    }

    @Override
    public void run() {
        isRunning = true;
        try (ServerSocket serverSocket = new ServerSocket(proxyData.getLocalPort())) { // default 50 connections max in backlog
            this.serverSocket = serverSocket;
            log.info("Server \"" + proxyData.getProxyName() + "\" successfully started!");
            while(!isStopped)
                try {
                    Socket clientSocket = serverSocket.accept();
                    proxyData.incrementTcpConnectCounter();
                    log.debug("Server " + proxyData.getProxyName() + " accepted {}-th incoming connection!", proxyData.getTcpConnectCount());
                    pool.execute(new ConnectionHandler(clientSocket, proxyData, pool));
                } catch (Exception probablyServerSocketWasClosed) {
                    if(serverSocket.isClosed()) {
                        isStopped = true;
                        log.info("Server \"" + proxyData.getProxyName() + "\" was stopped!");
                    } else
                        log.error("Caught some exception while accepting incoming connection on port" + proxyData.getLocalPort() + " for server " + proxyData.getProxyName(), probablyServerSocketWasClosed);
                }
            pool.shutdownNow();
        } catch (IOException | IllegalArgumentException exception) {
            isStopped = true;
            isRunning = false;
            log.error("Can't create socket on port " + proxyData.getLocalPort() + " for server " + proxyData.getProxyName() + "! This server will be re-creating!", exception);
            proxyController.getProxyServerMap().put(proxyData.getProxyName(), proxyController.createProxyServer(proxyData));
        }
    }

    public void stopServer() {
        try {
            isStopped = true;
            if(serverSocket != null)
                serverSocket.close();
        } catch (IOException exception) {
            log.error("Can't close socket on port " + proxyData.getLocalPort() + " for server " + proxyData.getProxyName(), exception);
        }
    }

    public boolean isStopped() {
        return isStopped;
    }

    public boolean isRunning() {
        return isRunning;
    }
}
