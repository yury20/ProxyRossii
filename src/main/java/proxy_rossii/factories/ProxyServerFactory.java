package proxy_rossii.factories;

import proxy_rossii.ProxyData;
import proxy_rossii.ProxyServer;

public interface ProxyServerFactory {
    ProxyServer createProxyServer(ProxyData proxyData);
}
