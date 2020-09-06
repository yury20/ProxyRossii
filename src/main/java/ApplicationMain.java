import interfaces.ProxyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ApplicationMain {

    public static final Logger LOGGER = LoggerFactory.getLogger(ApplicationMain.class);

    public static void main(String[] args) {

        ProxyConfigList configs;
        try {
            if(args.length > 0) {
                LOGGER.info("\n\n\nStart reading the external editable config file!");
                configs = new ProxyConfigList(new FileInputStream(args[0]));
            }
            else {
                LOGGER.info("\n\n\nStart reading the default config file!");
                InputStream inputStream = ApplicationMain.class.getClassLoader().getResourceAsStream("proxyR.properties");
                configs = new ProxyConfigList(inputStream);
            }
        } catch (IOException e) {
            LOGGER.error(e.toString());
            return;
        }

        for (ProxyConfig config : configs)
            new Thread(new ProxyServer(config), "Thread-" + config.getName()).start();

        LOGGER.info(">>>>>>>>>>>>>> Proxy Rossii started! <<<<<<<<<<<<<<<");
    }
}
