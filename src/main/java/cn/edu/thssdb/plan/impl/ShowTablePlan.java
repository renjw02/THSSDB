package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.exception.TableNotExistException;
import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.schema.Column;
import cn.edu.thssdb.schema.Table;
import cn.edu.thssdb.type.ColumnType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShowTablePlan extends LogicalPlan {
  private String name; // 表名称
  private String stmt; // SQL语句
  private List<List<String>> showTable;
  private List<String> columnNames;

  public ShowTablePlan(String name) {
    super(LogicalPlanType.SHOW_TABLE);
    this.name = name;
    this.stmt = "show table " + this.name;
  }

  @Override
  public void exec() {
    // TODO
    if (database == null) {
      throw new RuntimeException("Database not exist!");
    }
    Table table = database.get(name);
    if (table == null) {
      throw new TableNotExistException();
    }

    showTable = new ArrayList<>();
    columnNames = new ArrayList<>();
    ArrayList<Column> columns = table.getColumns();

    for (int i = 0; i < columns.size(); i++) {
      columnNames.add(columns.get(i).getName()); // 这行的作用是把列名加到columnNames里面
    }

    ArrayList<String> columnTypes = new ArrayList<>();
    ArrayList<String> is_null = new ArrayList<>();
    ArrayList<String> is_primary = new ArrayList<>();
    for (Column column : columns) {
      String type = ColumnType.columnType2String(column.getType());
      if (column.getType() == ColumnType.STRING)
        type += " (MAX_LENGTH: " + column.getMaxLength() + ")";
      columnTypes.add(type);
      if (column.isNotNull()) {
        is_null.add("NOT NULL");
      } else {
        is_null.add("");
      }
      if (column.isPrimary()) {
        is_primary.add("PRIMARY KEY");
      } else {
        is_primary.add("");
      }
    }
    showTable.add(columnNames);
    showTable.add(columnTypes);
    showTable.add(is_null);
  }

  public String getStmt() {
    return stmt;
  }

  public List<List<String>> getShowTable() {
    return showTable;
  }

  @Override
  public String getMessage() {
    return String.valueOf(showTable);
  }

  public List<String> getColumnNames() {
    return columnNames;
  }

  public ArrayList<String> getTableName() {
    return new ArrayList<>(Arrays.asList(this.name));
  }
}
