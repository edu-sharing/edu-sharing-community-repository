CREATE TABLE edu_one_time_tokens
(
    username varchar(100) not null primary key,
    secret   varchar(32)  not null
);
