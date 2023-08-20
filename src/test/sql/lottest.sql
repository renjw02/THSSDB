create database lottest;
show databases;
use lottest;
create table t1 ( c1 int primary key );
show table t1;
insert into t1 ( C1 ) values ( 1 );
insert into t1 values (1.1);
insert into t1 values ('asd');
create table t2 (c1 int primary key, c2 int not null, c3 int not null);
show table t2;
insert into t2 ( C1, C2, C3) values ( 1, 1, 1 );
insert into t2 values (1,1,null);
insert into t2 values (1,1);
create table t3 ( c1 float primary key );
show table t3;
insert into t3 ( C1 ) values ( 1.1 );
insert into t3 ( C1 ) values ( 'asd' );
create table t4 ( C1 string( 3 ) primary key );
show table t4;
insert into t4 ( C1 ) values ( '1'  );
insert into t4 values (1.1);
insert into t4 ( C1 ) values ( 'asdasd' );

connect root root
use db3
create table t1 ( c1 int primary key );
insert into t1 values (1)
insert into t1 values (2)
insert into t1 values (3)
insert into t1 values (4)
insert into t1 values (5)
insert into t1 values (6)
insert into t1 values (7)
select * from t1

connect root root
create database db3
use db3
insert into t1 values (1)

connect root root
use db3
select * from t1