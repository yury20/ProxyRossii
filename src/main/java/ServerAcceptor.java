import interfaces.ServerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Queue;

public class ServerAcceptor implements ServerHandler {

    public static final Logger LOGGER = LoggerFactory.getLogger(ServerAcceptor.class);

    private final static int ACCEPT_BUFFER_SIZE = 1000;

    private ServerConfig serverConfig;
    private Queue<ServerHandler> handlers;

    public ServerAcceptor(ServerConfig serverConfig, Queue<ServerHandler> handlers) {
        this.serverConfig = serverConfig;
        this.handlers = handlers;
    }

    @Override
    public void register(Selector selector) {
        try {
            ServerSocketChannel server = ServerSocketChannel.open();
            server.socket().bind(new InetSocketAddress(serverConfig.getPort()), ACCEPT_BUFFER_SIZE);
            server.configureBlocking(false);
            server.register(selector, SelectionKey.OP_ACCEPT, this);
        } catch (IOException exception) {
            LOGGER.error("Can't init server connection!", exception);
        }
    }

    @Override
    public void process(SelectionKey key) {
        if (key.isValid() && key.isAcceptable())
            try {
                ServerSocketChannel server = (ServerSocketChannel) key.channel();
                SocketChannel clientChannel = server.accept();
                handlers.add(serverConfig.getHandlerFactory().create(clientChannel));
            } catch (IOException exception) {
                LOGGER.error("Can't accept client connection!", exception);
            }
    }

    @Override
    public void destroy() {
        throw new UnsupportedOperationException("Method destroy() doesn't have implementation!");
    }
}
