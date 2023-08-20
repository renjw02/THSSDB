package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.plan.LogicalPlan;

public class BeginTansactionPlan extends LogicalPlan {
  public BeginTansactionPlan() {
    super(LogicalPlanType.BEGIN_TRANSACTION);
  }

  @Override
  public void exec() {}
}
