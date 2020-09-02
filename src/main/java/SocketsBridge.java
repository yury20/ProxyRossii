import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
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
        LOGGER.debug("SocketsBridge {}:{} --> {}:{} is starting...", in.getInetAddress().getHostName(), in.getPort(), out.getInetAddress().getHostName(), out.getPort());
        try (InputStream inputStream = in.getInputStream(); OutputStream outputStream = out.getOutputStream()) {
            if (inputStream == null || outputStream == null)
                return;

            byte[] data = new byte[1024];
            int bytesRead;
            int totalBytesRead = 0;
            while ((bytesRead = inputStream.read(data)) != -1) {
                byte[] dataCopy = Arrays.copyOf(data, bytesRead);
                scheduledPool.schedule(new ScheduledResponse(outputStream, dataCopy), delay, TimeUnit.MILLISECONDS);
                totalBytesRead += bytesRead;
                LOGGER.debug("Прочитано и записано " + bytesRead + " байт");
                LOGGER.debug("Содержимое ответа: " + new String(dataCopy, StandardCharsets.UTF_8));
            }
            LOGGER.debug("Переданы данные размером " + totalBytesRead + " байт");
        } catch (SocketException ignored) {
        } catch (Exception exception) {
            LOGGER.error("Some exception happened during executing SocketsBridge {}:{} --> {}:{}",
                    in.getInetAddress().getHostName(), in.getPort(), out.getInetAddress().getHostName(), out.getPort(), exception);
        }
        LOGGER.debug("SocketsBridge {}:{} --> {}:{} was finished.", in.getInetAddress().getHostName(), in.getPort(), out.getInetAddress().getHostName(), out.getPort());
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
