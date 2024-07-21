package ru.tele2.govorova.june.chat.db;

public class Queries {

    public final static String GET_USER_BY_LOGIN_AND_PASSWORD = """
            select u.user_name from users u
            where u.login = ?
            and u.password = ?
            """;

    public final static String ADD_USER = """
            insert into users (login, password, user_name) values (?, ?, ?);
            insert into users_to_roles (u.id, (select r.id from roles r where r.name = 'USER') from users u where u.login = ?)
            """;
}
