package cn.edu.thssdb.plan;

import cn.edu.thssdb.schema.Database;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public abstract class LogicalPlan {
  protected String username = "";
  protected String databaseName = "";
  protected Database database;

  protected LogicalPlanType type;

  public LogicalPlan(LogicalPlanType type) {
    this.type = type;
  }

  public LogicalPlanType getType() {
    return type;
  }

  /** 实现了exec()方法的多态 */
  public abstract void exec();

  public String getMessage() {
    return null;
  }

  public void setSessionId(Long sessionId) {}

  public ArrayList<String> getTableName() {
    return null;
  }

  public LinkedList<String> getLog() {
    return null;
  }

  public String getDatabaseName() {
    return null;
  }

  public List<List<String>> getData() {
    return null;
  }

  public List<String> getColumns() {
    return null;
  }

  public String getStmt() {
    return null;
  }

  public void undo() {}

  public void setCurrentUser(String username, String database) {
    this.username = username;
    this.databaseName = database;
    this.database = cn.edu.thssdb.schema.Manager.getInstance().getDatabaseByName(database);
  }

  public enum LogicalPlanType {
    // TODO: add more LogicalPlanType
    CREATE_DB,
    SHOW_DB,
    SHOW_TABLE,
    CREATE_TABLE,
    COMMIT_TYPE,
    BEGIN_TRANSACTION,
    USE_DB,
    CREATE_USER,
    DROP_DB,
    INSERT,
    DELETE,
    DROP_TABLE,
    SELECT,
    UPDATE,
    ROLLBACK,
    SAVEPOINT,
  }
}
