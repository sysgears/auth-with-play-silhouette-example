-- play-silhouette schema

-- !Ups

create table play_silhouette.users
(
    id         serial       not null
        constraint users_pk
            primary key,
    name       varchar(64)  not null,
    "lastName" varchar(64)  not null,
    password   varchar(128) not null,
    email      varchar(100) not null
);

create unique index users_id_uindex
    on play_silhouette.users (id);

create unique index users_email_uindex
    on play_silhouette.users (email);


-- !Downs

DROP TABLE play_silhouette.users