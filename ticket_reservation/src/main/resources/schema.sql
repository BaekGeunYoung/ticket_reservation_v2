drop table if exists reservation;
create table reservation (
 id bigint primary key auto_increment,
 user_id bigint,
 number int
);