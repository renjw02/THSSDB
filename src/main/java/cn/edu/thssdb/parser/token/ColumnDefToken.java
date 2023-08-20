package cn.edu.thssdb.parser.token;

public class ColumnDefToken {
  private String columnName;
  private TypeToken typeToken;
  private boolean isPrimaryKey;
  private boolean isNotNull;

  public ColumnDefToken(
      String columnName, TypeToken typeToken, boolean isPrimaryKey, boolean isNotNull) {
    this.columnName = columnName.toLowerCase();
    this.typeToken = typeToken;
    this.isPrimaryKey = isPrimaryKey;
    this.isNotNull = isNotNull;
  }

  public void setPrimaryKey(boolean primaryKey) {
    isPrimaryKey = primaryKey;
  }

  public void setNotNull(boolean notNull) {
    isNotNull = notNull;
  }

  public String getColumnName() {
    return columnName;
  }

  public TypeToken getTypeToken() {
    return typeToken;
  }

  public boolean isPrimaryKey() {
    return isPrimaryKey;
  }

  public boolean isNotNull() {
    return isNotNull;
  }
}
