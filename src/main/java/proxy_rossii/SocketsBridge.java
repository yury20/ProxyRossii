package proxy_rossii;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

            boolean isScheduledMode = false;
            int bytesRead;
            byte[] buffer = new byte[65536];

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                if (bytesRead > 0) {
                    byte[] tempData = Arrays.copyOf(buffer, bytesRead);
                    log.trace(".................................... {}: reading {} bytes. Data as string (UTF-8): {}", bridgeName, bytesRead, new String(tempData, StandardCharsets.UTF_8));
                    // logic for smoothing delay's cutting
                    if (proxyData.getDelay() > 0 || isScheduledMode) {
                        RunnableScheduledFuture lastTask = getLastTask(scheduledPool.getQueue());
                        int currDelay = proxyData.getDelay() / 2;
                        if (Objects.isNull(lastTask)) {
                            scheduledPool.schedule(new ScheduledResponse(bridgeName, outputStream, tempData), currDelay, TimeUnit.MILLISECONDS);
                            if(proxyData.getDelay() == 0)
                                isScheduledMode = false;
                        }
                        else {
                            int meldedDelay = Math.max((int) lastTask.getDelay(TimeUnit.MILLISECONDS), currDelay);
                            scheduledPool.schedule(new ScheduledResponse(bridgeName, outputStream, tempData), meldedDelay, TimeUnit.MILLISECONDS);
                            isScheduledMode = true;
                        }
                    } else {
                        log.trace(".................................... {}: writing {} bytes", bridgeName, bytesRead);
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
            Thread.sleep(proxyData.getDelay() / 2);
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

        public ScheduledResponse(String bridgeName, OutputStream outputStream, byte[] data) {
            this.bridgeName = bridgeName;
            this.outputStream = outputStream;
            this.data = data;
        }

        @Override
        public void run() {
            try {
                log.trace(".................................... {}: writing {} bytes", bridgeName, data.length);
                outputStream.write(data);
                outputStream.flush();
            } catch (IOException exception) {
                log.error("{}: can't write data to outputStream", bridgeName, exception);
            }
        }
    }

    private RunnableScheduledFuture getLastTask(Queue<Runnable> queue) {
        Runnable result = null;
        for (Runnable r : queue)
            result = r;
        return (RunnableScheduledFuture) result;
    }
}
