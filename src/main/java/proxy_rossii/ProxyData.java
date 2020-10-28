package proxy_rossii;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ProxyData {

    // control data
    @Getter private final String proxyName;
    @Getter private final int localPort;
    @Getter private final String remoteHost;
    @Getter private final int remotePort;
    @Getter @Setter private int delay = 0;

    // statistic data
    @Getter private AtomicInteger httpRequstCount = new AtomicInteger();
    @Getter private AtomicInteger tcpConnectCount = new AtomicInteger();
    @Getter private Date creationDate = new Date();


    public ProxyData(String proxyName, int localPort, String remoteHost, int remotePort) {
        this.proxyName = proxyName;
        this.localPort = localPort;
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
        log.info("ProxyData config \"{}\" was successfully created: localPort={}, remoteHost={}, remotePort={}, delay={}", proxyName, localPort, remoteHost, remotePort, delay);
    }

    public void incrementHttpRequstCounter() {
        httpRequstCount.getAndIncrement();
    }

    public void incrementTcpConnectCounter() {
        tcpConnectCount.getAndIncrement();
    }
}
