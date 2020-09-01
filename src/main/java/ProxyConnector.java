import interfaces.ProxyConfig;
import interfaces.ServerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;

public class ProxyConnector implements ServerHandler {

    public static final Logger LOGGER = LoggerFactory.getLogger(ProxyConnector.class);

    private ProxyBuffer clientBuffer = new ProxyBuffer();
    private ProxyBuffer serverBuffer = new ProxyBuffer();

    private SocketChannel clientChannel;

    private Selector selector;
    private SocketChannel serverChannel;
    private ProxyConfig config;

    public ProxyConnector(SocketChannel clientChannel, ProxyConfig config) {
        this.clientChannel = clientChannel;
        this.config = config;
    }

    public void readFromClient() throws IOException {
        serverBuffer.writeFrom(clientChannel);
        if (serverBuffer.isReadyToRead())
            register();
    }

    public void readFromServer() throws IOException {
        clientBuffer.writeFrom(serverChannel);
        if (clientBuffer.isReadyToRead())
            register();
    }

    public void writeToClient() throws IOException {
        clientBuffer.writeTo(clientChannel);
        if (clientBuffer.isReadyToWrite())
            register();
    }

    public void writeToServer() throws IOException {
        serverBuffer.writeTo(serverChannel);
        if (serverBuffer.isReadyToWrite())
            register();
    }

    public void register() throws ClosedChannelException {

        int clientOps = 0;
        if (serverBuffer.isReadyToWrite())
            clientOps |= SelectionKey.OP_READ;
        if (clientBuffer.isReadyToRead())
            clientOps |= SelectionKey.OP_WRITE;

        clientChannel.register(selector, clientOps, this);

        int serverOps = 0;
        if (clientBuffer.isReadyToWrite())
            serverOps |= SelectionKey.OP_READ;
        if (serverBuffer.isReadyToRead())
            serverOps |= SelectionKey.OP_WRITE;
        serverChannel.register(selector, serverOps, this);
    }

    private static void closeQuietly(SocketChannel channel) {
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException exception) {
                LOGGER.error("Could not close channel properly.", exception);
            }
        }
    }

    @Override
    public void register(Selector selector) {
        this.selector = selector;
        try {
            clientChannel.configureBlocking(false);
            InetSocketAddress socketAddress = new InetSocketAddress(config.getRemoteHost(), config.getRemotePort());
            serverChannel = SocketChannel.open();
            serverChannel.connect(socketAddress);
            serverChannel.configureBlocking(false);
            register();
        } catch (IOException exception) {
            destroy();
            LOGGER.error("Could not connect to " + config.getRemoteHost() + ":" + config.getRemotePort(), exception);
        }
    }

    @Override
    public void process(SelectionKey key) {
        try {
            if (key.channel() == clientChannel) {
                if (key.isValid() && key.isReadable())
                    readFromClient();
                if (key.isValid() && key.isWritable())
                    writeToClient();
            }

            if (key.channel() == serverChannel) {
                if (key.isValid() && key.isReadable())
                    readFromServer();
                if (key.isValid() && key.isWritable())
                    writeToServer();
            }
        } catch (ClosedChannelException exception) {
            destroy();
            LOGGER.info("Channel was closed by client or server.", exception);
        } catch (IOException exception) {
            destroy();
            LOGGER.error("Could not process.", exception);
        }
    }

    @Override
    public void destroy() {
        closeQuietly(clientChannel);
        closeQuietly(serverChannel);
    }
}
