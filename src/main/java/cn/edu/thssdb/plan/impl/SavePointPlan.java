package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.plan.LogicalPlan;

public class SavePointPlan extends LogicalPlan {

  private String savepoint;

  public SavePointPlan(String savepoint) {
    super(LogicalPlanType.SAVEPOINT);
    this.savepoint = savepoint;
  }

  @Override
  public void exec() {}

  public String getSavepoint() {
    return savepoint;
  }
}
