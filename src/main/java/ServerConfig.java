import interfaces.ServerHandlerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Конфиг сервера
public class ServerConfig {

    public static final Logger LOGGER = LoggerFactory.getLogger(ServerConfig.class);

    private int port; // Локальный порт, который будет слушать сервер (0-64000)
    private int workerCount; // > 0
    private ServerHandlerFactory handlerFactory;

    public ServerConfig(int port, ServerHandlerFactory handlerFactory, int workerCount) {
        if (workerCount < 1)
            throw new IllegalArgumentException("Count of workers should be at least 1!");

        if (port < 0)
            throw new IllegalArgumentException("Port can't be negative!");

        if (handlerFactory == null)
            throw new NullPointerException("Please specify handler factory!");

        this.port = port;
        this.workerCount = workerCount;
        this.handlerFactory = handlerFactory;
    }

    public int getPort() {
        return port;
    }

    public ServerHandlerFactory getHandlerFactory() {
        return handlerFactory;
    }

    public int getWorkerCount() {
        return workerCount;
    }

}
