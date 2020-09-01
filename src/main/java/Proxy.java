import interfaces.ProxyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Proxy {

    public static final Logger LOGGER = LoggerFactory.getLogger(Proxy.class);

    private final Server server;

    public Proxy(ProxyConfig config) {
        ProxyConnectorFactory handlerFactory = new ProxyConnectorFactory(config);
        LOGGER.debug("Created new handlerFactory ProxyConnectorFactory for " + config.getName());
        ServerConfig serverConfig = new ServerConfig(config.getLocalPort(), handlerFactory, config.getWorkerCount());
        LOGGER.debug("Created new ServerConfig for " + config.getName());
        server = new Server(serverConfig);
        LOGGER.debug("Created new Server for " + config.getName());
    }

    public void start() {
        server.start();
    }

    public void shutdown() {
        server.shutdown();
    }
}
