import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SocketsBridge implements Runnable {

    public static final Logger LOGGER = LoggerFactory.getLogger(SocketsBridge.class);

    private final ScheduledThreadPoolExecutor scheduledPool = new ScheduledThreadPoolExecutor(1);

    private final Socket in;
    private final Socket out;
    private final int delay;
    private final String proxyName;

    public SocketsBridge(Socket in, Socket out, int delay, String proxyName) {
        this.in = in;
        this.out = out;
        this.delay = delay;
        this.proxyName = proxyName;
    }

    @Override
    public void run() {
        String bridgeName = String.format("[%s][%s:%d --> %s:%d]", proxyName, in.getInetAddress().getHostName(), in.getPort(), out.getInetAddress().getHostName(), out.getPort());
        LOGGER.debug("{} is starting...", bridgeName);
        try {
            InputStream inputStream = in.getInputStream();
            OutputStream outputStream = out.getOutputStream();
            if (inputStream == null || outputStream == null) {
                LOGGER.error("{}: can't run the bridge because {}", bridgeName, String.format("%s%s%s", inputStream == null ? "inputStream is null" : "", (inputStream == null && outputStream == null) ? " and " : "", outputStream == null ? "outputStream is null" : ""));
                return;
            }

            int bytesRead;
            byte[] buffer = new byte[65536];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                if (bytesRead > 0) {
                    LOGGER.trace(".................................... {}: reading {} bytes", bridgeName, bytesRead);
                    if (delay > 0)
                        scheduledPool.schedule(new ScheduledResponse(bridgeName, outputStream, Arrays.copyOf(buffer, bytesRead)), delay, TimeUnit.MILLISECONDS);
                    else {
                        LOGGER.trace(".................................... {}: writing {} bytes", bridgeName, bytesRead);
                        outputStream.write(buffer, 0, bytesRead);
                        outputStream.flush();
                    }
                }
            }
        } catch (SocketException ignored) {
            LOGGER.trace("{}: SocketException has caught... It's likely because socket has already closed", bridgeName, ignored);
        } catch (Exception exception) {
            LOGGER.error("{}: some exception happened during executing", bridgeName, exception);
        }
        try {
            Thread.sleep(delay);
            in.close();
            out.close();
        } catch (InterruptedException exception) {
            LOGGER.error("{}: was interrupted before closing sockets", bridgeName, exception);
        } catch (IOException exception) {
            LOGGER.error("{}: can't close in/out sockets", bridgeName, exception);
        }
        LOGGER.debug("{} was finished", bridgeName);
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
                LOGGER.trace(".................................... {}: writing {} bytes", bridgeName, data.length);
                outputStream.write(data);
                outputStream.flush();
            } catch (IOException exception) {
                LOGGER.error("{}: can't write data to outputStream", bridgeName, exception);
            }
        }
    }
}
