package proxy_rossii;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import proxy_rossii.factories.ProxyDataFactory;
import proxy_rossii.factories.ProxyServerFactory;

@Slf4j
@Configuration
public class ApplicationConfiguration {

    @Bean
    ProxyController proxyController() {
        ProxyController proxyController = new ProxyController();
        proxyController.setProxyDataFactory(proxyDataFactory());
        proxyController.setProxyServerFactory(proxyServerFactory());
        return proxyController;
    }

    @Bean
    ProxyDataFactory proxyDataFactory() {
        return new ProxyDataFactory() {
            @Override
            public ProxyData createProxyData(String proxyName, int localPort, String remoteHost, int remotePort) {
                return proxyData(proxyName, localPort, remoteHost, remotePort);
            }
        };
    }

    @Bean
    @Scope("prototype")
    ProxyData proxyData(String proxyName, int localPort, String remoteHost, int remotePort) {
        return new ProxyData(proxyName, localPort, remoteHost, remotePort);
    }

    @Bean
    ProxyServerFactory proxyServerFactory() {
        return new ProxyServerFactory() {
            @Override
            public ProxyServer createProxyServer(ProxyData proxyData) {
                return proxyServer(proxyData);
            }
        };
    }

    @Bean
    @Scope("prototype")
    ProxyServer proxyServer(ProxyData proxyData) {
        return new ProxyServer(proxyData);
    }
}
