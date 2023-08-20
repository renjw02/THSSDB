package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.schema.Manager;

public class DropDatabasePlan extends LogicalPlan {
  private String databaseName;

  public DropDatabasePlan(String databaseName) {
    super(LogicalPlanType.DROP_DB);
    this.databaseName = databaseName;
  }
  /** [method] 执行操作 */
  @Override
  public void exec() {
    Manager manager = Manager.getInstance();
    manager.dropDatabaseIfExists(databaseName);
  }

  @Override
  public String getMessage() {
    return null;
  }

  public String getDatabaseName() {
    return databaseName;
  }

  @Override
  public String toString() {
    return "DropDatabasePlan{" + "databaseName='" + databaseName + '\'' + '}';
  }
}
