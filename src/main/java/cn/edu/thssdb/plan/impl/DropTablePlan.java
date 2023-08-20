package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.exception.DatabaseNotExistException;
import cn.edu.thssdb.plan.LogicalPlan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

public class DropTablePlan extends LogicalPlan {
  private String name; // 表名称

  public DropTablePlan(String name) {
    super(LogicalPlanType.DROP_TABLE);
    this.name = name;
  }

  public void exec() {
    if (database == null) {
      throw new DatabaseNotExistException();
    }
    database.drop(name);
  }

  @Override
  public ArrayList<String> getTableName() {
    return new ArrayList<>(Arrays.asList(this.name));
  }

  @Override
  public LinkedList<String> getLog() {
    return new LinkedList<>(Arrays.asList("DROP TABLE " + name));
  }
}
