import interfaces.ProxyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ApplicationMain {

    public static final Logger LOGGER = LoggerFactory.getLogger(ApplicationMain.class);

    public static void main(String[] args) {

        ProxyConfigList configs;
        try {
            if(args.length > 0)
                configs = new ProxyConfigList(args[0]);
            else
                configs = new ProxyConfigList("src/main/resources/proxyR.properties");
        } catch (IOException e) {
            LOGGER.error(e.toString());
            return;
        }

        LOGGER.info("Proxy is starting with " + configs.size() + " connectors");
        int cores = Runtime.getRuntime().availableProcessors();
        LOGGER.info("Proxy detected " + cores + " core" + (cores > 1 ? "s" : ""));
        int workerCount = Math.max(cores / configs.size(), 1);
        LOGGER.info("Proxy will use " + workerCount + " workers per connector");

        for (ProxyConfig config : configs) {
            config.setWorkerCount(workerCount);
            new Proxy(config).start();
        }

        LOGGER.info("Proxy started!");
    }
}
