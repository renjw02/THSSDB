package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.schema.Manager;

public class ShowDatabasePlan extends LogicalPlan {

  private String message;

  public ShowDatabasePlan() {
    super(LogicalPlanType.SHOW_DB);
  }

  @Override
  public void exec() {
    Manager manager = Manager.getInstance();
    message = manager.showAllDatabases();
  }

  @Override
  public String toString() {
    return "ShowDatabasePlan{}";
  }

  @Override
  public String getMessage() {
    return message;
  }
}
