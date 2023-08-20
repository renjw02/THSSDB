package cn.edu.thssdb.parser.token;

import cn.edu.thssdb.type.ColumnType;

public class TypeToken {
  private ColumnType columnType;
  private int strLen;

  public TypeToken(ColumnType columnType) {
    this.columnType = columnType;
    this.strLen = 0;
  }

  public TypeToken(ColumnType columnType, int strLen) {
    this.columnType = columnType;
    this.strLen = strLen;
  }

  public ColumnType getColumnType() {
    return columnType;
  }

  public int getStrLen() {
    return strLen;
  }
}
