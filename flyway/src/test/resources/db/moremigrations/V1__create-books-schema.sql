create table books(
    id bigint auto_increment primary key,
    name varchar(255) not null,
    constraint UK_name unique (name)
);
