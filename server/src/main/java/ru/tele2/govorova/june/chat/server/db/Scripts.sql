	create sequence user_id_seq start 1;
	create sequence role_id_seq start 1;

    create table users (
	id INT default nextval('user_id_seq') primary key,
	login VARCHAR(255) not null,
	password VARCHAR(255) not null,
	user_name VARCHAR(255),
	ban_flag char(1) check (ban_flag in ('Y','N')) default 'N',
	unban_date timestamp
	);

	create table roles (
	id INT default nextval('role_id_seq') primary key,
	name VARCHAR(255),
	description VARCHAR(255)
	);

	create table users_to_roles	(
	user_id int,
	role_id int,
	constraint user_fk foreign key (user_id) references users(id),
	constraint role_fk foreign key (role_id) references roles(id),
	primary key (user_id, role_id)
	);

    INSERT INTO public.roles
    (id, "name", description)
    VALUES(1, 'admin', 'Администратор');
    INSERT INTO public.roles
    (id, "name", description)
    VALUES(2, 'user', 'Пользователь чата')

    insert into users (login,"password",user_name)
    values ('admin','admin','admin'),('user','user','user');

    insert into roles ("name",description)
    values ('admin','Администратор'),('user','Пользователь');

    insert into users_to_roles (user_id, role_id)
    values (1,1),(2,2);