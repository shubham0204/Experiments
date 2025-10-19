create table if not exists blogs(
    id varchar(20) primary key,
    author_id varchar(20),
    title varchar(255),
    content text,
    foreign key( author_id ) references users( id )
) ;