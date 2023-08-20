package cn.edu.thssdb.schema;

import cn.edu.thssdb.type.ColumnType;

// Column类是用来描述表中的列的，包括列名、列类型、是否为主键、是否为非空、最大长度等信息
public class Column implements Comparable<Column> {
  private String name; // 列名
  private ColumnType type; // 列类型
  private boolean primary; // 是否为主键
  private boolean notNull; // 是否为非空
  private int maxLength; // 最大长度

  public Column(String name, ColumnType type, boolean primary, boolean notNull, int maxLength) {
    this.name = name;
    this.type = type;
    this.primary = primary;
    this.notNull = notNull;
    this.maxLength = maxLength;
  }

  @Override
  public int compareTo(Column e) {
    return name.compareTo(e.name);
  }

  public String toString() {
    return name + "," + type + "," + primary + "," + notNull + "," + maxLength;
  }

  public String toString(String separator) {
    return name + separator + type + separator + primary + separator + notNull + separator
        + maxLength;
  }

  public String getName() {
    return name;
  }

  public ColumnType getType() {
    return type;
  }

  public int getMaxLength() {
    return maxLength;
  }

  public boolean isPrimary() {
    return primary;
  }

  public boolean isNotNull() {
    return notNull;
  }
}
