package interfaces;

public interface ProxyConfig {
    String getName();
    int getLocalPort();
    String getRemoteHost();
    int getRemotePort();
    int getDelay();
    int getWorkerCount();
    void setWorkerCount(int workerCount);
}
