package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.plan.LogicalPlan;

public class RollBackPlan extends LogicalPlan {

  private String savepoint;

  public RollBackPlan() {
    super(LogicalPlanType.ROLLBACK);
    savepoint = null;
  }

  public RollBackPlan(String savepoint) {
    super(LogicalPlanType.ROLLBACK);
    this.savepoint = savepoint;
  }

  @Override
  public void exec() {}

  public String getSavepoint() {
    return savepoint;
  }
}
