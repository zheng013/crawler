CREATE TABLE LINKS_TO_BE_PROCESSED(link varchar(2000));
CREATE TABLE LINKS_ALREADY_PROCESSED(link varchar(2000));
CREATE TABLE NEWS(id bigint PRIMARY KEY auto_increment,title text,content text,url varchar(2000),created_at timestamp,modified_at timestamp );