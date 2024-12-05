package ru.tele2.govorova.june.chat.server;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.tele2.govorova.june.chat.server.db.Queries;
import ru.tele2.govorova.june.chat.server.schedulers.UnbanScheduler;

public class DatabaseAuthenticationProvider implements AuthenticationProvider {
    private static final Logger logger = LogManager.getLogger(DatabaseAuthenticationProvider.class.getName());

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
        logger.info("Сервис аутентификации запущен: Database режим");
        UnbanScheduler unbanScheduler = new UnbanScheduler(server);
        server.getConnectionsPool().execute(unbanScheduler::run);
        logger.info("UnbanScheduler запущен");
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
            logger.error("Ошибка при выполнении SQL запроса", se);
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
            logger.error("Ошибка при выполнении SQL запроса", se);
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
            logger.error("Ошибка при выполнении SQL запроса", se);
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
        try (PreparedStatement ps = connection.prepareStatement(Queries.GET_USER_BLOCK_STATUS)) {
            ps.setString(1, authUsername);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                clientHandler.setBanFlag(rs.getString("ban_flag"));
            }
        } catch (SQLException se) {
            logger.error("Ошибка при выполнении SQL запроса", se);
        }
        if (clientHandler.getBanFlag().equals("Y")) {
            clientHandler.sendMessage("Ваша учетная запись заблокирована, обратитесь к администратору.");
            return false;
        }
        if (server.isUsernameBusy(authUsername)) {
            clientHandler.sendMessage("Указанная учетная запись уже занята.");
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
            logger.error("Ошибка при выполнении SQL запроса", se);
        }
        server.subscribe(clientHandler);
        clientHandler.sendMessage("/authok " + authUsername);
        return true;
    }

    @Override
    public boolean registration(ClientHandler clientHandler, String login, String password, String username) {
        if (login.trim().length() < 3 || password.trim().length() < 6 || username.trim().length() < 3) {
            clientHandler.sendMessage("Неверный формат команды.Логин 3+ символа, Пароль 6+ символов, Имя пользователя 3+ символа.");
            return false;
        }
        if (isLoginAlreadyExist(login)) {
            clientHandler.sendMessage("Указанный логин уже занят.");
            return false;
        }
        if (isUsernameAlreadyExist(username)) {
            clientHandler.sendMessage("Указанное имя пользователя уже занято.");
            return false;
        }
        try (PreparedStatement ps = connection.prepareStatement(Queries.ADD_USER)) {
            ps.setString(1, login);
            ps.setString(2, password);
            ps.setString(3, username);
            ps.setString(4, login);
            ps.executeUpdate();
        } catch (SQLException se) {
            logger.error("Ошибка при выполнении SQL запроса", se);
        }
        clientHandler.setUsername(username);
        try (PreparedStatement ps = connection.prepareStatement(Queries.GET_USER_ROLES)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                clientHandler.setRole(rs.getString("name"));
            }
        } catch (SQLException se) {
            logger.error("Ошибка при выполнении SQL запроса", se);
        }
        server.subscribe(clientHandler);
        clientHandler.sendMessage("/regok " + username);
        return true;
    }

    @Override
    public void banOrUnbanUser(String blockStatus, int days, String userName) {
        try (PreparedStatement ps = connection.prepareStatement(Queries.BAN_OR_UNBAN_USER)) {
            Timestamp newTimestamp = null;
            if (Objects.equals(blockStatus, "Y")) {
                if (days == -1) {
                    newTimestamp = Timestamp.valueOf("2999-12-31 23:59:59.999");
                } else {
                    newTimestamp = Timestamp.valueOf(LocalDateTime.now().plusDays(days));
                }
            }
            ps.setString(1, blockStatus);
            ps.setTimestamp(2, newTimestamp);
            ps.setString(3, userName);
            ps.executeUpdate();
        } catch (SQLException se) {
            logger.error("Ошибка при выполнении SQL запроса", se);
        }
    }

    @Override
    public void setUserName(String currentUserName, String newUserName) {
        try (PreparedStatement ps = connection.prepareStatement(Queries.SET_USER_NAME)) {
            ps.setString(1, newUserName);
            ps.setString(2, currentUserName);
            ps.executeUpdate();
        } catch (SQLException se) {
            logger.error("Ошибка при выполнении SQL запроса", se);
        }
    }

    @Override
    public Set<String> getUsersToUnban() {
        Set<String> bannedUsers = new HashSet<>();
        try (PreparedStatement ps = connection.prepareStatement(Queries.GET_USERS_TO_UNBAN)) {
            ResultSet result = ps.executeQuery();
            while (result.next()) {
                bannedUsers.add(result.getString("user_name"));
            }
        } catch (SQLException se) {
            logger.error("Ошибка при выполнении SQL запроса", se);
        }
        return bannedUsers;
    }
}
