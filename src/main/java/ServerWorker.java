import interfaces.ServerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Queue;
import java.util.Set;

public class ServerWorker extends Thread {

    public static final Logger LOGGER = LoggerFactory.getLogger(ServerWorker.class);

    private static long SELECTOR_TIMEOUT = 100L;
    private Queue<ServerHandler> handlers;

    public ServerWorker(Queue<ServerHandler> handlers) {
        super("ServerWorker");
        this.handlers = handlers;
    }

    @Override
    public void run() {
        Selector selector = null;
        try {
            selector = Selector.open();

            while (!interrupted()) {
                ServerHandler newHandler = handlers.poll();
                if (newHandler != null)
                    newHandler.register(selector);

                selector.select(SELECTOR_TIMEOUT);

                Set<SelectionKey> keys = selector.selectedKeys();
                for (SelectionKey key : keys) {
                    ServerHandler handler = (ServerHandler) key.attachment();
                    handler.process(key);
                }
                keys.clear();
            }
        } catch (IOException exception) {
            LOGGER.error("Problem with selector, worker will be stopped!", exception);
        } finally {
            if (selector != null)
                closeSelector(selector);
        }
    }

    private void closeSelector(Selector selector) {
        for (SelectionKey key : selector.keys())
            closeOrLog(key.channel(), "Could not close selector channel properly.");

        closeOrLog(selector, "Could not close selector properly.");
    }

    private void closeOrLog(Closeable closeable, String errorMessage) {
        try {
            closeable.close();
        } catch (IOException exception) {
            LOGGER.error(errorMessage, exception);
        }
    }
}
