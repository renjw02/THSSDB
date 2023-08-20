package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.User.User;
import cn.edu.thssdb.User.UserManager;
import cn.edu.thssdb.plan.LogicalPlan;

public class CreateUserPlan extends LogicalPlan {
  private String userName; // 用户名,小驼峰命名
  private String passWord; // 密码,小驼峰命名
  private Long sessionId;

  /** [method] 构造方法 */
  public CreateUserPlan(String userName, String passWord) {
    super(LogicalPlanType.CREATE_USER);
    this.userName = userName;
    this.passWord = passWord;
  }

  /** [method] 执行操作 */
  @Override
  public void exec() {
    UserManager.getInstance().logon(sessionId, userName, passWord, User.Permission.USER);
  }

  public void setSessionId(Long sessionId) {
    this.sessionId = sessionId;
  }
}
