package proxy_rossii;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
public class SocketsBridge implements Runnable {

    enum Direction {
        CLIENT_TO_SERVER,
        SERVER_TO_CLIENT
    }

    private final ScheduledThreadPoolExecutor scheduledPool = new ScheduledThreadPoolExecutor(1);

    private final Socket in;
    private final Socket out;
    private final ProxyData proxyData;
    private final Direction direction;

    private long lastFutureTime = System.nanoTime(); // It's mark for executing time of the last scheduled task

    public SocketsBridge(Socket in, Socket out, ProxyData proxyData, Direction direction) {
        this.in = in;
        this.out = out;
        this.proxyData = proxyData;
        this.direction = direction;
    }

    @Override
    public void run() {
        String bridgeName = String.format("[%s][%s][%s:%d --> %s:%d]", proxyData.getProxyName(), direction, in.getInetAddress().getHostName(), in.getPort(), out.getInetAddress().getHostName(), out.getPort());
        log.debug("{} is starting...", bridgeName);
        try {
            InputStream inputStream = in.getInputStream();
            OutputStream outputStream = out.getOutputStream();
            if (inputStream == null || outputStream == null) {
                log.error("{}: can't run the bridge because {}", bridgeName, String.format("%s%s%s", inputStream == null ? "inputStream is null" : "", (inputStream == null && outputStream == null) ? " and " : "", outputStream == null ? "outputStream is null" : ""));
                return;
            }

            int bytesRead;
            boolean isScheduledMode = false;
            byte[] buffer = new byte[32768]; // 97% reading operations suit this size

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                if (bytesRead > 0) {
                    long timestamp = System.currentTimeMillis(); // helpful for debugging to check real delay between reading and writing
                    byte[] tempData = Arrays.copyOf(buffer, bytesRead);
                    log.trace(".................... {}: reading {} bytes.", bridgeName, bytesRead);
                    log.trace("................... {}: reading {} bytes. Data as string (UTF-8): {}",
                            bridgeName, bytesRead, new String(tempData, StandardCharsets.UTF_8));

                    // logic for smoothing delay's cutting
                    if (proxyData.getDelay() > 0 || isScheduledMode) {
                        long currDelayNS = (proxyData.getDelay() * 1000_000L) / 2;
                        long diff = lastFutureTime - System.nanoTime();
                        scheduledPool.schedule(new ScheduledResponse(bridgeName, outputStream, tempData, timestamp), Math.max(diff, currDelayNS), TimeUnit.NANOSECONDS);
                        lastFutureTime = System.nanoTime() + Math.max(diff, currDelayNS);
                        isScheduledMode = (currDelayNS > 0 || diff > 0);
                    } else {
                        log.trace("................... {}: writing {} bytes", bridgeName, bytesRead);
                        outputStream.write(buffer, 0, bytesRead);
                        outputStream.flush();
                    }
                }
                if(Thread.currentThread().isInterrupted())
                    break;
            }
        } catch (SocketException ignored) {
            log.trace("{}: SocketException has caught... It's likely because socket has already closed", bridgeName, ignored);
        } catch (IOException exception) {
            log.error("{}: some IOException happened during executing", bridgeName, exception);
        }
        try {
            Thread.sleep(proxyData.getDelay() / 2 + 1);
        } catch (InterruptedException exception) {
            log.error("{}: was interrupted! Closing in/out sockets immediately...", bridgeName, exception);
            scheduledPool.shutdownNow();
        }
        try {
            in.close();
            out.close();
        } catch (IOException exception) {
            log.error("{}: can't close in/out sockets", bridgeName, exception);
        }

        log.debug("{} was finished", bridgeName);
    }

    private class ScheduledResponse implements Runnable {

        private String bridgeName;
        private OutputStream outputStream;
        private byte[] data;
        private long timestamp;

        public ScheduledResponse(String bridgeName, OutputStream outputStream, byte[] data, long timestamp) {
            this.bridgeName = bridgeName;
            this.outputStream = outputStream;
            this.data = data;
            this.timestamp = timestamp;
        }

        @Override
        public void run() {
            try {
                log.trace(".................... {}: writing {} bytes. De facto the bridge's delay was {} ms", bridgeName, data.length, System.currentTimeMillis() - timestamp);
                outputStream.write(data);
                outputStream.flush();
            } catch (IOException exception) {
                log.error("{}: can't write data to outputStream", bridgeName, exception);
            }
        }
    }
}
