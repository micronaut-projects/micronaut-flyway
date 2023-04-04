create table users (
  id bigint auto_increment primary key,
  first_name varchar(50) null,
  last_name varchar(50) null,
  username varchar(100) not null,
  constraint UK_r43af9ap4edm43mmtq01oddj6
    unique (username)
);

