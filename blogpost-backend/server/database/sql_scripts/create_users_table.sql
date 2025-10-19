create table if not exists users(
    id varchar(20) primary key,
    name varchar(20),
    email_address varchar(320),
    password varchar(10),
    tagline varchar(50),
    join_date date
) ;