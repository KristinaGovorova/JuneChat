package ru.tele2.govorova.june.chat.server;

import java.sql.*;

import ru.tele2.govorova.june.chat.server.db.Queries;

public class DatabaseAuthenticationProvider implements AuthenticationProvider {

    private final Server server;
    private final Connection connection;
    private final Statement statement;

    public DatabaseAuthenticationProvider(Server server, String url, String login, String password) throws SQLException {
        this.server = server;
        this.connection = DriverManager.getConnection(url, login, password);
        this.statement = connection.createStatement();
    }

    @Override
    public void initialize() {
        System.out.println("Сервис аутентификации запущен: Database режим");
    }

    private String getUsernameByLoginAndPassword(String login, String password) {
        try (PreparedStatement ps = connection.prepareStatement(Queries.GET_USER_BY_LOGIN_AND_PASSWORD)) {
            ps.setString(1, login);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("user_name");
            }
        } catch (SQLException se) {
            se.printStackTrace();
        }
        return null;
    }

    private boolean isLoginAlreadyExist(String login) {
        try (PreparedStatement ps = connection.prepareStatement(Queries.GET_USER_BY_LOGIN)) {
            ps.setString(1, login);
            ResultSet rs = ps.executeQuery();
            if (rs.isBeforeFirst()) {
                return true;
            }
        } catch (SQLException se) {
            se.printStackTrace();
        }
        return false;
    }

    private boolean isUsernameAlreadyExist(String username) {
        try (PreparedStatement ps = connection.prepareStatement(Queries.GET_USER_BY_USERNAME)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.isBeforeFirst()) {
                return true;
            }
        } catch (SQLException se) {
            se.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean authenticate(ClientHandler clientHandler, String login, String password) {
        String authUsername = getUsernameByLoginAndPassword(login, password);
        if (authUsername == null) {
            clientHandler.sendMessage("Некорретный логин/пароль");
            return false;
        }
        if (server.isUsernameBusy(authUsername)) {
            clientHandler.sendMessage("Указанная учетная запись уже занята");
            return false;
        }
        clientHandler.setUsername(authUsername);
        try (PreparedStatement ps = connection.prepareStatement(Queries.GET_USER_ROLES)) {
            ps.setString(1, authUsername);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                clientHandler.setRole(rs.getString("name"));
            }
        } catch (SQLException se) {
            se.printStackTrace();
        }
        server.subscribe(clientHandler);
        clientHandler.sendMessage("/authok " + authUsername);
        return true;
    }

    @Override
    public boolean registration(ClientHandler clientHandler, String login, String password, String username) {
        if (login.trim().length() < 3 || password.trim().length() < 6 || username.trim().length() < 1) {
            clientHandler.sendMessage("Логин 3+ символа, Пароль 6+ символов, Имя пользователя 1+ символ");
            return false;
        }
        if (isLoginAlreadyExist(login)) {
            clientHandler.sendMessage("Указанный логин уже занят");
            return false;
        }
        if (isUsernameAlreadyExist(username)) {
            clientHandler.sendMessage("Указанное имя пользователя уже занято");
            return false;
        }
        try (PreparedStatement ps = connection.prepareStatement(Queries.ADD_USER)) {
            ps.setString(1, login);
            ps.setString(2, password);
            ps.setString(3, username);
            ps.setString(4, login);
            ps.executeUpdate();
        } catch (SQLException se) {
            se.printStackTrace();
        }
        clientHandler.setUsername(username);
        try (PreparedStatement ps = connection.prepareStatement(Queries.GET_USER_ROLES)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                clientHandler.setRole(rs.getString("name"));
            }
        } catch (SQLException se) {
            se.printStackTrace();
        }
        server.subscribe(clientHandler);
        clientHandler.sendMessage("/regok " + username);
        return true;
    }
}
