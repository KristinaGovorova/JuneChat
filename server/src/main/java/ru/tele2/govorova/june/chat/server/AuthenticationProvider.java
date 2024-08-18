package ru.tele2.govorova.june.chat.server;

import java.util.Set;

public interface AuthenticationProvider {
    void initialize();
    boolean authenticate(ClientHandler clientHandler, String login, String password);
    boolean registration(ClientHandler clientHandler, String login, String password, String username);

    void banOrUnbanUser(String banStatus, int days, String userName);

    void setUserName(String currentUserName, String newUserName);

    Set<String> getUsersToUnban();
}
