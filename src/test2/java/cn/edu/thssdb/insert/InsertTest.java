package cn.edu.thssdb.insert;

import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.plan.LogicalGenerator;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
public class InsertTest {
    @Before
    public void setUp() {

    }

//    @Test
//    public void testParsing() {
//        ArrayList<LogicalPlan> plans = new ArrayList<>();
//        plans.add(LogicalGenerator.generate("create database db1;"));
//        plans.add(LogicalGenerator.generate("use db1;"));
//        plans.add(LogicalGenerator.generate("create table t4 ( C1 string( 3 ) primary key ); "));
//        plans.add(LogicalGenerator.generate("insert into t4 ( C1 ) values ( '1'  );"));
//        for(LogicalPlan plan:plans){
//            plan.exec();
//        }
//
//        //plans.add(LogicalGenerator.generate("insert ;");
//    }

//    public void main() {
//        testParsing();
//    }
}


