import interfaces.ProxyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
// TODO Убрать русский текст из логов
// TODO Убрать лишний дебаг из логов (оставить info)
// TODO Написать README

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

        for (ProxyConfig config : configs) {
            new Thread(new ProxyServer(config), "Thread-" + config.getName()).start();
        }

        LOGGER.info(" >>>>>>>>>>>>>> Proxy Rossii started! <<<<<<<<<<<<<<<");
    }
}
