package ru.tele2.govorova.june.chat.server.db;

public class Queries {

    public final static String GET_USER_BY_LOGIN_AND_PASSWORD = """
            select u.user_name from users u
            where u.login = ?
            and u.password = ?
            """;

    public final static String GET_USER_BY_LOGIN = """
            select u.user_name from users u
            where u.login = ?
            """;


    public final static String ADD_USER = """
            insert into users (login, password, user_name) values (?, ?, ?);
            insert into users_to_roles (select u.id, (select r.id from roles r where r.name = 'user') from users u where u.login = ?)
            """;

    public final static String GET_USER_BY_USERNAME = """
            select * from  users u where u.user_name = ?
            """;

    public final static String GET_USER_ROLES = """
            select r.name from roles r, users_to_roles ur, users u where
            r.id = ur.role_id and 
            u.id = ur.user_id and 
            u.user_name = ?
            """;

    public final static String SET_USER_NAME = """
            update users set user_name = ? where user_name = ?
            """;

    public final static String BAN_OR_UNBAN_USER = """
            UPDATE users SET ban_flag = ?, unban_date = ? WHERE user_name = ?
            """;

    public final static String GET_USER_BLOCK_STATUS = """
            SELECT ban_flag from users WHERE user_name = ?
            """;
}
