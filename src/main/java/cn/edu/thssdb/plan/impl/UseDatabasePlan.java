package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.schema.Manager;

public class UseDatabasePlan extends LogicalPlan {
  private String newDatabaseName; // 切换到的数据库名称

  public UseDatabasePlan(String databaseName) {
    super(LogicalPlanType.USE_DB);
    this.newDatabaseName = databaseName;
  }

  public String getDatabaseName() {
    return newDatabaseName;
  }

  @Override
  public String toString() {
    return "UseDatabasePlan{" + "newDatabaseName='" + newDatabaseName + '\'' + '}';
  }

  @Override
  public void exec() {
    Manager manager = Manager.getInstance();
    if (databaseName != null) manager.quitDatabase(databaseName);
    manager.useDatabase(newDatabaseName);
  }
}
