package interfaces;

import java.nio.channels.SocketChannel;

// Сервер использует этот класс для создания обработчика всех входящих подключений от клиентов.
// После того как обработчик был создан, сервер использует его для обработки событий из клиентского канала.
public interface ServerHandlerFactory {
    ServerHandler create(SocketChannel clientChannel);
}
