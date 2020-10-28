package proxy_rossii.factories;

import proxy_rossii.ProxyData;
import proxy_rossii.ProxyServer;

public interface ProxyDataFactory {
    ProxyData createProxyData(String proxyName, int localPort, String remoteHost, int remotePort);
}
