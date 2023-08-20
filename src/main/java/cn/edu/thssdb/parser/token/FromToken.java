package cn.edu.thssdb.parser.token;

public class FromToken {
  private String tableNameA = null;
  private String tableNameB = null;
  private MultipleConditionToken multipleConditionToken = null;

  public String getTableNameA() {
    return tableNameA;
  }

  public String getTableNameB() {
    return tableNameB;
  }

  public MultipleConditionToken getMultipleConditionToken() {
    return multipleConditionToken;
  }

  public FromToken(String tableNameA, String tableNameB, MultipleConditionToken tokens) {
    if (tableNameA != null) this.tableNameA = tableNameA;
    if (tableNameB != null) this.tableNameB = tableNameB;
    this.multipleConditionToken = tokens;
  }
}
