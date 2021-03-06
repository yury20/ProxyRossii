import interfaces.ProxyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ProxyConfigList extends ArrayList<ProxyConfig> {

    public static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfigList.class);

    public ProxyConfigList(InputStream inputResources) throws IOException {

        Properties properties = new Properties();
        properties.load(inputResources);

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

        private Config(String configName, int localPort, String remoteHost, int remotePort, int delay) {
            this.configName = configName;
            this.localPort = localPort;
            this.remoteHost = remoteHost;
            this.remotePort = remotePort;
            this.delay = delay;
            LOGGER.info("Config \"{}\" was successfully created: localPort={}, remoteHost={}, remotePort={}, delay={}", configName, localPort, remoteHost, remotePort, delay);
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
    }
}
