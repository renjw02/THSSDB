package cn.edu.thssdb.User;

import cn.edu.thssdb.plan.*;
import cn.edu.thssdb.plan.impl.CommitPlan;
import cn.edu.thssdb.rpc.thrift.*;
import cn.edu.thssdb.schema.*;
import cn.edu.thssdb.transaction.TransactionManager;
import cn.edu.thssdb.transaction.TransactionStatus;
import cn.edu.thssdb.utils.StatusUtil;

import java.util.ArrayList;
import java.util.List;

public class UserService {
  public User user; // 用户数据
  private Long sessionId;

  private TransactionManager transactionManager; // 事务管理
  /** [method] 构造方法 */
  public UserService(User user, Long sessionId) {
    this.user = user;
    this.sessionId = sessionId;
    transactionManager = new TransactionManager(user.database, null);
  }

  public void disconnect() {
    forceCommit();
    if (user.database != null) {
      Manager.getInstance().quitDatabase(user.database);
    }
  }

  // 强制提交
  private void forceCommit() {
    if (user.database != null) {
      if (transactionManager.isUnderTransaction()) {
        TransactionStatus status = transactionManager.exec(new CommitPlan());
        if (!status.getStatus()) {
          throw new RuntimeException(status.getMessage());
        }
      }
    }
  }

  /** [method] 服务处理方法 —— 主方法 */
  public synchronized ExecuteStatementResp handle(String statement) { // 传入的是一条完整的SQL语句
    ExecuteStatementResp resp = new ExecuteStatementResp();
    boolean has_select = false;
    List<List<String>> data_all = new ArrayList<>();
    List<String> columns_all = new ArrayList<>();
    LogicalPlan plan = null;
    try {
      plan = LogicalGenerator.generate(statement.toLowerCase());
    } catch (Exception e) {
      resp.setStatus(StatusUtil.fail("bad input " + e.getMessage()));
      resp.setHasResult(false);
      return resp;
    }
    plan.setCurrentUser(user.username, user.database);
    switch (plan.getType()) {
      case USE_DB:
        System.out.println("[DEBUG] " + plan);
        forceCommit(); // 强制提交
        plan.exec();
        String databaseName = plan.getDatabaseName(); // 获取use的数据库名
        user.database = databaseName; // 更新当前用户的数据库名
        transactionManager.setDatabase(databaseName); // 更新事务管理器的数据库名
        return new ExecuteStatementResp(StatusUtil.success(), false);
      case CREATE_USER:
        System.out.println("[DEBUG] " + plan);
        plan.setSessionId(sessionId);
        plan.exec();
        return new ExecuteStatementResp(StatusUtil.success(), false);
      case CREATE_DB:
        System.out.println("[DEBUG] " + plan);
        plan.exec();
        return new ExecuteStatementResp(StatusUtil.success(), false);
        //            case SHOW_DB:
        //                System.out.println("[DEBUG] " + plan);
        //                plan.exec();
        //                return new ExecuteStatementResp(StatusUtil.success(plan.getMessage()),
        // false);
      default:
        System.out.println("[DEBUG] " + plan);
        // 所有的操作都需要先提交，再执行
        TransactionStatus status = transactionManager.exec(plan);
        if (!status.getStatus()) {
          resp.setStatus(StatusUtil.success(status.getMessage()));
          resp.setHasResult(false);
          return resp;
          // throw new RuntimeException(status.getMessage());
        }
        TransactionStatus.Table result = status.getRes(); // select or show 会返回结果表
        if (result != null) {
          columns_all = result.columns;
          data_all = result.data;
          has_select = true;
        }
        if (has_select) {
          resp.setColumnsList(columns_all);
          resp.setRowList(data_all);
        }
        resp.setStatus(StatusUtil.success(plan.getMessage()));
        resp.setHasResult(has_select);
        return resp;
    }
    //        return null;
  }
}
