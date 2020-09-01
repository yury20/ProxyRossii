import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;

public class ProxyBuffer {

    public static final Logger LOGGER = LoggerFactory.getLogger(ProxyBuffer.class);

    private enum BufferState {
        READY_TO_WRITE,
        READY_TO_READ
    }

    private final static int BUFFER_SIZE = 1024;

    private ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
    private BufferState state = BufferState.READY_TO_WRITE;

    public boolean isReadyToRead() {
        return state == BufferState.READY_TO_READ;
    }

    public boolean isReadyToWrite() {
        return state == BufferState.READY_TO_WRITE;
    }

    public void writeFrom(SocketChannel channel) throws IOException {
        int read = channel.read(buffer);
        if (read == -1)
            throw new ClosedChannelException();

        if (read > 0) {
            buffer.flip();
            state = BufferState.READY_TO_READ;
        }
    }

    /* Этот метод пытается записать данные из буфера в канал.
    Буфер меняет состояние на READY_TO_READ, только если все данные были
    записаны в канал, в противном случае вы должны вызвать этот метод снова. */
    public void writeTo(SocketChannel channel) throws IOException {

        channel.write(buffer);

        // only if buffer is empty
        if (buffer.remaining() == 0) {
            buffer.clear();
            state = BufferState.READY_TO_WRITE;
        }
    }
}
