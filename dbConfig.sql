CREATE DATABASE comp4111project;
USE comp4111project;
CREATE TABLE book
( 	bookid bigint(50) NOT NULL AUTO_INCREMENT,
	Title varchar(100) NOT NULL,
	Author varchar(100) NOT NULL,
    Publisher varchar(100) NOT NULL,
    Year int(30) NOT NULL,
    Available tinyint(4) NOT NULL,
	PRIMARY KEY (bookid)
);
CREATE TABLE user
( 	userid bigint(30) NOT NULL,
	Username varchar(50) NOT NULL,
	Password varchar(50) NOT NULL,
	PRIMARY KEY (userid)
);