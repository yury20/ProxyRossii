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

    private static final ScheduledThreadPoolExecutor scheduledPool = new ScheduledThreadPoolExecutor(100);

    private final Socket in;
    private final Socket out;
    private final int delay;

    public SocketsBridge(Socket in, Socket out, int delay) {
        this.in = in;
        this.out = out;
        this.delay = delay;
    }

    @Override
    public void run() {
        String bridgeType = String.format("%s:%d --> %s:%d", in.getInetAddress().getHostName(), in.getPort(), out.getInetAddress().getHostName(), out.getPort());
        LOGGER.debug("SocketsBridge {} is starting...", bridgeType);
        try (InputStream inputStream = in.getInputStream(); OutputStream outputStream = out.getOutputStream()) {
            if (inputStream == null || outputStream == null)
                return;

            int bytesRead;
            byte[] buffer = new byte[131072];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                LOGGER.debug("............................... {}: прочитано {} байт", bridgeType, bytesRead);
                if(bytesRead > 0)
                    scheduledPool.schedule(new ScheduledResponse(outputStream, Arrays.copyOf(buffer, bytesRead)), delay, TimeUnit.MILLISECONDS);
//                    LOGGER.debug("Содержимое потока " + bridgeType + ": " + new String(allData, StandardCharsets.UTF_8));
            }
        } catch (SocketException ignored) {
        } catch (Exception exception) {
            LOGGER.error("Some exception happened during executing SocketsBridge {}", bridgeType, exception);
        }
        LOGGER.debug("SocketsBridge {} was finished.", bridgeType);
    }

    private class ScheduledResponse implements Runnable {

        private OutputStream outputStream;
        private byte[] data;

        public ScheduledResponse(OutputStream outputStream, byte[] data) {
            this.outputStream = outputStream;
            this.data = data;
        }

        @Override
        public void run() {
            try {
                outputStream.write(data);
                outputStream.flush();
            } catch (IOException e) {
                throw new RuntimeException("Can't write data to OutputStream!");
            }
        }
    }
}
