package cn.edu.thssdb.parser.token;

public class ColumnFullNameToken {
  private String tableName = null;
  private String columnName;

  public ColumnFullNameToken(String tableName, String columnName) {
    if (tableName != null) this.tableName = tableName;
    this.columnName = columnName;
  }

  public String getTableName() {
    return tableName;
  }

  public String getColumnName() {
    return columnName;
  }

  public boolean compareTo(ColumnFullNameToken c) {
    return c.getColumnName().equals(this.columnName)
        && (c.getTableName() == null || c.getTableName().equals(this.tableName));
  }

  public boolean compareTo(String tableName, String columnName) {
    return columnName.equals(this.columnName)
        && (tableName == null || tableName.equals(this.tableName));
  }

  @Override
  public String toString() {
    String fullName;
    if (tableName != null) {
      fullName = tableName + "." + columnName;
    }
    fullName = columnName;
    return fullName;
  }
}
