package ru.tele2.govorova.june.chat.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Server {
    private static final Logger logger = LogManager.getLogger(Server.class.getName());
    private int port;
    private List<ClientHandler> clients;
    private AuthenticationProvider authenticationProvider;
    private Properties properties;
    private static final String CONFIG_PATH = "config.properties";
    private final ExecutorService connectionsPool = Executors.newCachedThreadPool();

    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    public Server(int port) throws SQLException, IOException {
        this.port = port;
        this.clients = new ArrayList<>();
        getProperties();
        this.authenticationProvider = new DatabaseAuthenticationProvider(this,
                properties.getProperty("database_url"),
                properties.getProperty("database_login"),
                properties.getProperty("database_password"));
    }

    private void getProperties() throws IOException {
        properties = new Properties();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream stream = loader.getResourceAsStream(CONFIG_PATH);
        properties.load(stream);
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("Сервер запущен на порту: {}", port);
            authenticationProvider.initialize();
            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(this, socket);
            }
        } catch (Exception e) {
            logger.error("Ошибка при запуске сервера", e);
        }
        connectionsPool.shutdown();
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        broadcastMessage("В чат зашел: " + clientHandler.getUsername());
        clients.add(clientHandler);
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastMessage("Из чата вышел: " + clientHandler.getUsername());
    }

    public synchronized void broadcastMessage(String message) {
        for (ClientHandler c : clients) {
            c.sendMessage(message);
        }
    }

    public synchronized void whisperMessage(ClientHandler sourceClient, String message, String username) {
        for (ClientHandler c : clients) {
            if (c.getUsername().equals(username)) {
                c.sendMessage(message);
                sourceClient.sendMessage(message);
                return;
            }
        }
        sourceClient.sendMessage("Указанный пользователь не онлайн");
    }

    public boolean isUsernameBusy(String username) {
        for (ClientHandler c : clients) {
            if (c.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void disconnectUser(String userName) {
        for (ClientHandler c : clients) {
            if (c.getUsername().equals(userName)) {
                c.sendMessage("/exit");
                return;
            }
        }
    }

    public synchronized void sendActiveUsers(ClientHandler sourceClient) {
        StringBuilder clientList = new StringBuilder();
        for (ClientHandler client : clients) {
            clientList.append(client.getUsername()).append("\r\n");
        }
        sourceClient.sendMessage(clientList.toString());
    }

    public synchronized void sendUserNameChangedMessage(String oldUserName, String newUserName) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("[hh24:mm:ss]");
        String messageTime = simpleDateFormat.format(new Date());
        String message = messageTime + " UserName " + oldUserName + " is changed to " + newUserName;
        for (ClientHandler client : clients) {
            if (!client.getUsername().equals(newUserName)) {
                client.sendMessage(message);
            }
        }
    }

    public ExecutorService getConnectionsPool() {
        return connectionsPool;
    }

    public void shutdown() {
        System.exit(0);
    }
}
