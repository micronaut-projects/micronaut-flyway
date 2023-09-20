create table books(
    id serial primary key,
    name varchar(255) not null,
    author varchar(255) not null,
    constraint UK_name unique (name)
);
