CREATE TABLE public.authors
(
    id      SERIAL,
    name    VARCHAR(255),
    version INTEGER STORAGE PLAIN,
    CONSTRAINT author_pkey PRIMARY KEY (id)
);

insert into authors(name, version) values('author1', 1);
insert into authors(name, version) values('author2', 1);
insert into authors(name, version) values('author3', 1);
