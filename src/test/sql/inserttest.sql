create database asd;
use asd;
create table t1 ( c1 float primary key );
insert into t1 values (1);
insert into t1 values (1);
insert into t1 values ('asd');
create table t2 (c1 int primary key, c2 int not null, c3 int not null);
insert into t2(C1,C2) values (1,2);
create table t4 ( C1 string( 3 ) primary key );
insert into t4 values ('asdasd');
use db1;
drop database asd;