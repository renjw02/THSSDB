package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.exception.DatabaseNotExistException;
import cn.edu.thssdb.exception.WrongCreateTableException;
import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.schema.Column;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

public class CreateTablePlan extends LogicalPlan {
  private String name; // 表名称
  private Column[] columns; // 列定义
  private int primaryIndex; // 主键索引
  private String stmt;

  static final String keyNotDefined = "Exception: create table failed (primary key not defined) !";
  static final String moreThanOneKey =
      "Exception: create table failed (more than one primary key defined) !";
  static final String duplicateColumnName =
      "Exception: create table failed (duplicate column name) !";

  /** [method] 构造方法 */
  public CreateTablePlan(String name, Column[] columns, int primaryIndex, String stmt) {
    super(LogicalPlanType.CREATE_TABLE);
    this.name = name;
    this.columns = columns;
    this.primaryIndex = primaryIndex;
    this.stmt = stmt;
  }

  /** [method] 执行操作 */
  public void exec() {
    System.out.print("asd");
    if (database == null) {
      throw new DatabaseNotExistException();
    }
    // 这个循环的作用是检查是否有重复的列名
    for (int i = 0; i < columns.length; i++) {
      for (int j = 0; j < i; j++) {
        if (columns[i].getName().equals(columns[j].getName())) {
          throw new WrongCreateTableException(duplicateColumnName);
        }
      }
    }

    if (primaryIndex == -1) {
      throw new WrongCreateTableException(keyNotDefined);
    }

    int keys = 0;

    for (Column column : columns) {
      if (column.isPrimary()) {
        keys++;
      }
    }

    if (keys != 1) {
      throw new WrongCreateTableException(moreThanOneKey);
    }
    System.out.println(columns);
    database.create(name, columns, primaryIndex);
    database.persistMeta();
  }

  @Override
  public ArrayList<String> getTableName() {
    return new ArrayList<>(Arrays.asList(this.name));
  }

  @Override
  public LinkedList<String> getLog() {
    return new LinkedList<>(Arrays.asList(stmt));
  }
}
