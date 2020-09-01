import interfaces.ProxyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class ProxyConfigList extends ArrayList<ProxyConfig> {

    public static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfigList.class);

    public ProxyConfigList(String propsFileName) throws IOException {

        Properties properties = new Properties();
        properties.load(new FileInputStream(propsFileName));

        Set<String> proxyNames = new HashSet<>();
        Set<String> props = properties.stringPropertyNames();

        for(String prop : props)
            proxyNames.add(prop.substring(0, prop.indexOf('.')));

        for(String name : proxyNames) {
            try {
                int localPort = Integer.parseInt(properties.getProperty(name + ".localPort"));
                String remoteHost = properties.getProperty(name + ".remoteHost");
                int remotePort = Integer.parseInt(properties.getProperty(name + ".remotePort"));
                int delay = Integer.parseInt(properties.getProperty(name + ".delay"));
                Config config = new Config(name, localPort, remoteHost, remotePort, delay);
                this.add(config);
            } catch (NumberFormatException e) {
                throw new IOException("Bad config file! Can't parse \"" + name + "\" data!");
            }
        }
    }

    private class Config implements ProxyConfig {

        private String configName;
        private int localPort;
        private String remoteHost;
        private int remotePort;
        private int delay;
        private int workerCount;

        public Config(String configName, int localPort, String remoteHost, int remotePort, int delay) {
            this(configName, localPort, remoteHost, remotePort, delay, 0);
        }

        public Config(String configName, int localPort, String remoteHost, int remotePort, int delay, int workerCount) {
            this.configName = configName;
            this.localPort = localPort;
            this.remoteHost = remoteHost;
            this.remotePort = remotePort;
            this.delay = delay;
            this.workerCount = workerCount;
            LOGGER.info(String.format("Config \"%s\" was successfully created: localPort=%d, remoteHost=%s, remotePort=%d, delay=%d, workerCount=%d", configName, localPort, remoteHost, remotePort, delay, workerCount));
        }

        @Override
        public String getName() {
            return configName;
        }

        @Override
        public int getLocalPort() {
            return localPort;
        }

        @Override
        public String getRemoteHost() {
            return remoteHost;
        }

        @Override
        public int getRemotePort() {
            return remotePort;
        }

        @Override
        public int getDelay() {
            return delay;
        }

        @Override
        public int getWorkerCount() {
            return workerCount;
        }

        @Override
        public void setWorkerCount(int workerCount) {
            this.workerCount = workerCount;
        }
    }
}
