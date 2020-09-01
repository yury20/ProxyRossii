import interfaces.ServerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/*
Простой TCP-сервер на базе NIO.
Сервер использует воркеры для обработки входящих клиентских подключений.
Worker - это поток, он ожидает своего селектора - java.nio.channels.Selector

Только один воркер обрабатывает входящее клиентское соединение.
После чего этот воркер использует ServerHandlerFactory для создания хэндлера ServerHandler
и добавляет его в очередь не запущенных хэндлеров. Все воркеры имеют доступ к этой очереди.

Воркер имеет следующий жизненный цикл:
- попытаться получить из очереди один незапущенный обработчик
- если он существует, зарегистрируйте его, затем дождитесь селектора с таймаутом, получите события ввода-вывода
- для каждого события привязываем обработчик от ключа и обрабатываем его.
- После этого воркер возвращается к шагу с очередью.
*/
public class Server {

    public static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

    private ServerConfig config;
    private String name;

    private Queue<ServerHandler> handlers;
    private Thread[] workers;

    public Server(ServerConfig config) {
        if (config == null)
            throw new NullPointerException("Please specify server config!");

        this.config = config;
        name = "Server on port " + config.getPort();
    }

    // Этот метод начинает ожидание входящих соединений для проброса на удаленный хост.
    // Метод возвращает контроль когда все воркеры стартанут. Метод не блокирующий.
    public void start() {
        if (workers != null)
            throw new UnsupportedOperationException("Please shutdown connector!");

        LOGGER.info("Starting \"" + name + "\" with " + config.getWorkerCount() + " workers");

        handlers = new ConcurrentLinkedQueue<>();
        handlers.add(new ServerAcceptor(config, handlers));

        workers = new Thread[config.getWorkerCount()];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new ServerWorker(handlers);
            workers[i].start();
        }

        LOGGER.info(name + " successfully started!");
    }

    // Разрывает соединение.
    // Метод ждет когда все ресурсы будут закрыты.
    // Вы можете вызвать этот метод в любое время.
    // Если вы вызываете этот метод дважды (без старта сервера) - без проблем.
    public void shutdown() {

        if (workers == null) {
            LOGGER.info(name + " already been shutdown");
            return;
        }

        LOGGER.info("Starting shutdown " + name);

        for (Thread worker : workers) {
            worker.interrupt();
            try {
                worker.join();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }

        workers = null;

        ServerHandler handler;
        while ((handler = handlers.poll()) != null)
            handler.destroy();
        handlers = null;

        LOGGER.info(name + " was shutdown");
    }
}
