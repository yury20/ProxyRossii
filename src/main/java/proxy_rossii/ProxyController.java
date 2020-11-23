package proxy_rossii;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import proxy_rossii.factories.ProxyDataFactory;
import proxy_rossii.factories.ProxyServerFactory;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

@Slf4j
class ProxyController {

    @Getter private final Map<String, ProxyServer> proxyServerMap = new TreeMap<>();

    private final String pathToConfigs = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath().replaceFirst("/\\w+\\.jar!/BOOT-INF/classes!", "/proxyR.configs").replace("file:", "");
    private final File FILE_CONFIGS_FOR_LOAD_AND_SAVING = pathToConfigs.endsWith("/main/") ?
            getFileFromURL("proxyR.configs") :
            new File(pathToConfigs);

    @Getter private Properties properties;
    @Setter private ProxyServerFactory proxyServerFactory;
    @Setter private ProxyDataFactory proxyDataFactory;

    public ProxyController() {
        this.properties = new Properties();
        try {
            boolean fileDoesntExist = FILE_CONFIGS_FOR_LOAD_AND_SAVING.createNewFile();
            if(fileDoesntExist)
                log.warn("Can't find the old config-file! A new config was created by path: {}", FILE_CONFIGS_FOR_LOAD_AND_SAVING);
            else
                log.debug("Previously created config-file found by path: {}", FILE_CONFIGS_FOR_LOAD_AND_SAVING);
        } catch (IOException exception) {
            log.error("Can't create new config-file for path: {}", FILE_CONFIGS_FOR_LOAD_AND_SAVING);
        }
        try (FileReader in = new FileReader(FILE_CONFIGS_FOR_LOAD_AND_SAVING)) {
            properties.load(in);
        } catch (IOException exception) {
            log.error("Can't load properties file!", exception);
        }
    }

    private File getFileFromURL(String file_configs) {
        URL url = this.getClass().getClassLoader().getResource(file_configs);
        File file;
        try {
            file = new File(Objects.requireNonNull(url).toURI());
        } catch (URISyntaxException exception) {
            file = new File(Objects.requireNonNull(url).getPath());
        }

        return file;
    }

    @PostConstruct
    private void init() {

        Set<String> proxyNames = new HashSet<>();
        Set<String> props = properties.stringPropertyNames();

        for(String prop : props) {
            int dotIndex = prop.indexOf('.');
            if(dotIndex > 0)
                proxyNames.add(prop.substring(0, dotIndex));
            else
                properties.remove(prop);
        }

        for(String proxyName : proxyNames) {
            try {
                int localPort = Integer.parseInt(properties.getProperty(proxyName + ".localPort"));
                String remoteHost = properties.getProperty(proxyName + ".remoteHost");
                int remotePort = Integer.parseInt(properties.getProperty(proxyName + ".remotePort"));
                proxyServerMap.put(proxyName, createProxyServer(proxyName, localPort, remoteHost, remotePort));
            } catch (NumberFormatException exception) {
                log.error("Bad config file! Can't parse \"{}\" data!", proxyName, exception);
            }
        }

        log.info(">>>>>>>>>>>>>> Proxy Rossii started! <<<<<<<<<<<<<<<");
    }

    public void stopProxyServer(String proxyName) {
        ProxyServer proxyServer = proxyServerMap.get(proxyName);
        if(proxyServer != null) {
            proxyServer.stopServer();
            ProxyServer recreatedProxyServer = createProxyServer(proxyServer.getProxyData());
            proxyServerMap.put(proxyName, recreatedProxyServer);
        }
    }

    public void startProxyServer(String proxyName) {
        ProxyServer proxyServer = proxyServerMap.get(proxyName);
        if(proxyServer != null && !proxyServer.isStopped())
            proxyServer.start();
    }

    public void addProxyServer(String proxyName, int localPort, String remoteHost, int remotePort) {
        ProxyServer newProxyServer = createProxyServer(proxyName, localPort, remoteHost, remotePort);
        proxyServerMap.put(proxyName, newProxyServer);

        if(newProxyServer != null) {
            properties.setProperty(proxyName + ".localPort", Integer.toString(localPort));
            properties.setProperty(proxyName + ".remoteHost", remoteHost);
            properties.setProperty(proxyName + ".remotePort", Integer.toString(remotePort));
            saveConfigs(properties);
        }
    }

    public void deleteProxyServer(String proxyServerName) {
        proxyServerMap.remove(proxyServerName);
        properties.remove(proxyServerName + ".localPort");
        properties.remove(proxyServerName + ".remoteHost");
        properties.remove(proxyServerName + ".remotePort");
        saveConfigs(properties);
    }

    public ProxyServer createProxyServer(String proxyName, int localPort, String remoteHost, int remotePort) {
        return proxyServerFactory.createProxyServer(proxyDataFactory.createProxyData(proxyName, localPort, remoteHost, remotePort));
    }

    public ProxyServer createProxyServer(ProxyData proxyData) {
        return proxyServerFactory.createProxyServer(proxyData);
    }

    private void saveConfigs(Properties properties) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_CONFIGS_FOR_LOAD_AND_SAVING))) {
            properties.list(writer);
        } catch (IOException exception) {
            log.error("Can't save configs to file {}", FILE_CONFIGS_FOR_LOAD_AND_SAVING, exception);
        }
    }

}
