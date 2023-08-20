use db2;
select * from t1;
select * from t2;
select * from t3;
select * from t4;
select * from t1 join t2 on t1.c1 = t2.c1, t3 where t1.c1 < t3.c1;
select t1.c1, t2.c1, t2.c3, t3.c1, t4.c1 from t1 join t2 on t1.c1 = t2.c1, t3 join t4 on t3.c1 < t4.c1, t4 where t1.c1 = 2;