package cn.edu.thssdb.exception;

/** ************* [Exception] 表不存在 ************* */
public class TableNotExistException extends RuntimeException {
  private String tableName;

  public TableNotExistException() {
    this.tableName = "";
  }

  public TableNotExistException(String tableName) {
    this.tableName = tableName;
  }

  @Override
  public String getMessage() {
    return "Exception: table " + this.tableName + "doesn't exist!";
  }
}
