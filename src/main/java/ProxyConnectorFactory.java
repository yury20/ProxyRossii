import interfaces.ProxyConfig;
import interfaces.ServerHandler;
import interfaces.ServerHandlerFactory;

import java.nio.channels.SocketChannel;

public class ProxyConnectorFactory implements ServerHandlerFactory {

    private ProxyConfig config;
    private String name;

    public ProxyConnectorFactory(ProxyConfig config) {
        this.config = config;
        this.name = config.getName();
    }

    @Override
    public ServerHandler create(SocketChannel clientChannel) {
        return new ProxyConnector(clientChannel, config);
    }

    @Override
    public String getName() {
        return name;
    }
}
