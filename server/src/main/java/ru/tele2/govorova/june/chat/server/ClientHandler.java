package ru.tele2.govorova.june.chat.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;


public class ClientHandler {
    private static final Logger logger = LogManager.getLogger(ClientHandler.class.getName());

    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private String username;
    private String role;
    private String banFlag;


    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getBanFlag() {
        return banFlag;
    }

    public void setBanFlag(String banFlag) {
        this.banFlag = banFlag;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());

        new Thread(() -> {
            try {
                sendMessage("Перед работой с чатом необходимо выполнить аутентификацию '/auth login password' или регистрацию '/register login password username'");
                logger.info("Подключился новый клиент");
                while (true) {
                    String message = in.readUTF();
                    if (message.equals("/exit")) {
                        sendMessage("/exitok");
                        return;
                    }

                    if (message.startsWith("/auth ")) {
                        String[] elements = message.split(" ");
                        if (elements.length != 3) {
                            sendMessage("Неверный формат команды /auth");
                            continue;
                        }
                        if (server.getAuthenticationProvider().authenticate(this, elements[1], elements[2])) {
                            break;
                        }
                        continue;
                    }
                    if (message.startsWith("/register ")) {
                        String[] elements = message.split(" ");
                        if (elements.length != 4) {
                            sendMessage("Неверный формат команды /register");
                            continue;
                        }
                        if (server.getAuthenticationProvider().registration(this, elements[1], elements[2], elements[3])) {
                            break;
                        }
                        break;
                    }
                }

                while (true) {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("[hh24:mm:ss]");
                    String messageTime = simpleDateFormat.format(new Date());
                    String message = in.readUTF();
                    if (message.startsWith("/")) {
                        if (message.equals("/exit")) {
                            sendMessage("/exitok");
                            break;
                        }
                        if (message.startsWith("/w")) {
                            String[] words = message.split(" ");
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

                            server.whisperMessage(this, messageTime + username + ": " + messageToSend, userToSend);
                            continue;
                        }

                        if (message.startsWith("/activelist")) {
                            String[] elements = message.split(" ");
                            if (elements.length > 1) {
                                sendMessage("Некорректный формат команды");
                            }
                            server.sendActiveUsers(this);
                            continue;
                        }
                        if (message.startsWith("/changenick ")) {
                            String[] elements = message.split(" ");
                            if (elements.length != 2) {
                                this.sendMessage("Некорректный формат команды");
                                break;
                            }
                            String oldUserName = getUsername();
                            String newUserName = elements[1];
                            server.getAuthenticationProvider().setUserName(oldUserName, newUserName);
                            setUsername(newUserName);
                            server.sendUserNameChangedMessage(oldUserName, newUserName);
                            this.sendMessage("Your userName is changed to \"" + newUserName + "\"");
                            continue;
                        }

                        if (message.startsWith("/kick")) {
                            if (!(getRole().equals("admin"))) {
                                sendMessage("Данное действие недоступно. Вы не администратор");
                                continue;
                            }
                            String[] elements = message.split(" ");
                            if (elements.length != 2) {
                                sendMessage("Неверный формат команды /kick");
                                continue;
                            }
                            if (!(server.isUsernameBusy(elements[1]))) {
                                sendMessage("Вы хотите выгнать несуществующего пользователя");
                                continue;
                            }
                            server.disconnectUser(elements[1]);
                            continue;
                        }
                        if (message.startsWith("/ban")) {
                            if (!(getRole().equals("admin"))) {
                                sendMessage("Данное действие недоступно. Вы не администратор");
                                continue;
                            }
                            String[] elements = message.split(" ");
                            if (elements.length == 2) {
                                String userToBan = elements[1];
                                server.getAuthenticationProvider().banOrUnbanUser("Y", -1, userToBan);
                                server.disconnectUser(userToBan);
                                sendMessage("Пользователь " + userToBan + " заблокирован");
                                continue;
                            }
                            if (elements.length == 3) {
                                String userToBan = elements[1];
                                int daysCount = Integer.parseInt(elements[2]);
                                server.getAuthenticationProvider().banOrUnbanUser("Y", daysCount, userToBan);
                                server.disconnectUser(userToBan);
                                sendMessage("Пользователь " + userToBan + " заблокирован");
                                continue;
                            }
                        }
                        if (message.startsWith("/unban ")) {
                            if (!(getRole().equals("admin"))) {
                                sendMessage("Данное действие недоступно. Вы не администратор");
                                continue;
                            }
                            String[] elements = message.split(" ");
                            if (elements.length == 2) {
                                String userToUnban = elements[1];
                                server.getAuthenticationProvider().banOrUnbanUser("N", -1, userToUnban);
                                sendMessage("Пользователь " + userToUnban + " был разблокирован");
                                continue;
                            }
                        }
                        if (message.startsWith("/shutdown")) {
                            if (!(getRole().equals("admin"))) {
                                sendMessage("Данное действие недоступно. Вы не администратор");
                                continue;
                            }
                            sendMessage("/shutdown_ok");
                            server.shutdown();
                            break;
                        }
                        continue;
                    }
                    server.broadcastMessage(messageTime + username + ": " + message);
                }
            } catch (IOException e) {
                logger.error("Произошла I\\O ошибка ", e);
            } finally {
                disconnect();
            }
        }).start();
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            logger.error("Произошла ошибка при отправке сообщения", e);
        }
    }

    public void disconnect() {
        server.unsubscribe(this);
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            logger.error("Ошибка закрытия in потока", e);
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            logger.error("Ошибка закрытия out потока", e);
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            logger.error("Ошибка закрытия сокета", e);
        }
    }
}
