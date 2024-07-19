package ru.tele2.govorova.june.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private String username;

    private static int usersCount = 0;

    public String getUsername() {
        return username;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        usersCount++;
        this.username = "user" + usersCount;
        new Thread(() -> {
            try {
                System.out.println("Подключился новый клиент");
                while (true) {
                    String message = in.readUTF();
                    if (message.startsWith("/")) {
                        if (message.equals("/exit")) {
                            sendMessage("/exitok");
                            break;
                        }
                        if (message.startsWith("/w")) {
                            String[] words = message.split(" ", 3);
                            if (words.length < 3) {
                                sendMessage("Неверный формат команды /w");
                                continue;
                            }
                            String userToSend = words[1];
                            if (!(server.isUsernameBusy(userToSend))) {
                                sendMessage("Вы хотите отправить сообщение несуществующему пользователю");
                                continue;
                            }
                            String messageToSend = "";
                            messageToSend = words[2];

                            server.whisperMessage(this, username + ": " + messageToSend, userToSend);
                            continue;

                        }
                        continue;
                    }
                    server.broadcastMessage(username + ": " + message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }).start();
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        server.unsubscribe(this);
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

