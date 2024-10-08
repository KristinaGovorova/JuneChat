package ru.tele2.govorova.june.chat.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import ru.tele2.govorova.june.chat.server.DatabaseAuthenticationProvider;


public class Server {
    private int port;
    private List<ClientHandler> clients;
    private AuthenticationProvider authenticationProvider;
    private Properties properties;
    private static final String CONFIG_PATH = "config.properties";

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
            System.out.println("Сервер запущен на порту: " + port);
            authenticationProvider.initialize();
            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(this, socket);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
}
