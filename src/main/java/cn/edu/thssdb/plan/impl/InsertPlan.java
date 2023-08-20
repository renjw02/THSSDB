package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.exception.DatabaseNotExistException;
import cn.edu.thssdb.exception.TableNotExistException;
import cn.edu.thssdb.exception.WrongInsertException;
import cn.edu.thssdb.parser.token.LiteralValueToken;
import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.schema.Column;
import cn.edu.thssdb.schema.Entry;
import cn.edu.thssdb.schema.Row;
import cn.edu.thssdb.schema.Table;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

public class InsertPlan extends LogicalPlan {

  private String tableName;
  private ArrayList<String> columnNames = null;
  private ArrayList<ArrayList<LiteralValueToken>> values; // TODO
  private ArrayList<Row> rowsHasInsert; // TODO
  private ArrayList<Row> rowsToInsert; // TODO
  private int[] columnMatch;

  private Table table;

  static final String wrongColumnNum =
      "Exception: wrong insert operation (columns unmatched)!"; // 列数不匹配
  static final String wrongColumnType =
      "Exception: wrong insert operation (type unmatched)!"; // 类型不匹配
  static final String wrongValueNum =
      "Exception: wrong insert operation (number of columns and values unmatched)!"; // 列数与值数不匹配
  static final String duplicateColumnName =
      "Exception: wrong insert operation (duplicate name of columns)!"; // 列名重复
  static final String wrongColumnName =
      "Exception: wrong insert operation (wrong column name)!"; // 属性名不在列定义中
  static final String duplicateKey =
      "Exception: wrong insert operation (insertion causes duplicate key)!"; // 主键重复
  static final String wrongStringLength =
      "Exception: wrong insert operation (string exceeds length limit)!"; // 字符串过长

  /** [method] 构造方法 */
  public InsertPlan(String tableName, ArrayList<ArrayList<LiteralValueToken>> values) {
    super(LogicalPlanType.INSERT);
    this.tableName = tableName;
    this.values = values;
    rowsHasInsert = new ArrayList<>();
    rowsToInsert = new ArrayList<>();
  }

  public InsertPlan(
      String tableName,
      ArrayList<String> columnNames,
      ArrayList<ArrayList<LiteralValueToken>> values) {
    super(LogicalPlanType.INSERT);
    this.tableName = tableName;
    this.columnNames = columnNames;
    this.values = values;
    rowsHasInsert = new ArrayList<>();
    rowsToInsert = new ArrayList<>();
  }

  /** [method] 执行操作 */
  public void exec() {
    if (database == null) { // TODO
      throw new DatabaseNotExistException();
    }

    table = database.get(tableName);
    if (table == null) {
      throw new TableNotExistException();
    }

    ArrayList<Column> columns = table.getColumns();

    int primaryKeyIndex = table.primaryIndex;
    String primaryKey = columns.get(primaryKeyIndex).getName();
    System.out.println("insertplan columnnamesize" + columnNames.size() + "\n");
    //  TODO判断插入是否有主键
    if (columnNames.size() == 0) { // 插入一个table所有colum都有赋值的value
      for (ArrayList<LiteralValueToken> value : values) {
        if (value.size() != columns.size()) {
          System.out.print(wrongColumnNum);
          throw new WrongInsertException(wrongColumnNum);
        }
        ArrayList<Entry> entries = new ArrayList<>();

        // 类型检查
        Iterator<Column> column_it = columns.iterator();
        Iterator<LiteralValueToken> value_it = value.iterator();
        while (column_it.hasNext()) {
          matchType(column_it.next(), value_it.next(), primaryKey, entries);
        }
        Row newRow = new Row(entries);
        rowsToInsert.add(newRow);
      }
    } else {
      columnMatch = new int[columns.size()];
      for (int i = 0; i < columns.size(); i++) {
        columnMatch[i] = -1;
      }
      if (columnNames.size() > columns.size()) {
        System.out.print(wrongColumnNum);
        throw new WrongInsertException(wrongColumnNum);
      }
      for (ArrayList<LiteralValueToken> items : values) {
        if (items.size() != columnNames.size()) {
          System.out.print(wrongValueNum);
          throw new WrongInsertException(wrongValueNum);
        }
      }

      // 列名重复或不存在
      for (int i = 0; i < columnNames.size(); i++) {
        for (int j = 0; j < i; j++) {
          if (columnNames.get(i).equals(columnNames.get(j))) {
            System.out.print(duplicateColumnName);
            throw new WrongInsertException(duplicateColumnName);
          }
        }
        boolean hasMatched = false;
        for (int j = 0; j < columns.size(); j++) {
          //          System.out.print(columnNames.get(i));
          //          System.out.print(table.getColumns().get(j).getName());
          if (columnNames.get(i).equals(table.getColumns().get(j).getName())) {
            hasMatched = true;
            columnMatch[j] = i;
            break;
          }
        }
        if (hasMatched == false) {
          System.out.print(wrongColumnName);
          throw new WrongInsertException(wrongColumnName);
        }
      }

      for (ArrayList<LiteralValueToken> value : values) {

        ArrayList<Entry> entries = new ArrayList<>();

        Iterator<Column> column_it = columns.iterator();
        int i = 0;
        while (column_it.hasNext()) {
          Column c = column_it.next();
          int match = columnMatch[i];

          // 将没匹配到的列的值置为null
          if (match != -1) {
            matchType(c, value.get(match), primaryKey, entries);
          } else {
            if (c.isNotNull()) {
              System.out.print(
                  "Exception: wrong insert operation ( column "
                      + c.getName()
                      + " cannot be null )");
              throw new WrongInsertException(
                  "Exception: wrong insert operation ( column "
                      + c.getName()
                      + " cannot be null )");
            } else {
              entries.add(new Entry(null));
            }
          }
          i++;
        }
        //        System.out.print("insertplan\n");
        //        for(Entry e:entries){
        //          System.out.print(e.value);
        //        }
        Row newRow = new Row(entries);
        rowsToInsert.add(newRow);
      }
    }
    insert();
  }

  /** [method] 撤销操作 */
  public void undo() {
    for (Row row : rowsHasInsert) {
      table.deleteUndo(row);
    }
  }

  /** [method] 确认无异常后插入 */
  private void insert() {
    try {
      // table.insert(rowsToInsert);
      for (Row row : rowsToInsert) {
        table.insert(row);
        rowsHasInsert.add(row);
      }
    } catch (Exception e) {
      undo();
      System.out.print(duplicateKey);
      throw new WrongInsertException(duplicateKey);
    }

    rowsToInsert.clear();
  }

  @Override
  public ArrayList<String> getTableName() {
    ArrayList<String> temp = new ArrayList<>();
    temp.add(this.tableName);
    return temp;
  }

  // 将字符串转为相应的值加入entries
  private void matchType(
      Column column, LiteralValueToken value, String primaryKey, ArrayList<Entry> entries) {
    LiteralValueToken.Type value_type = value.getType();
    //    System.out.print("matchType");
    //    System.out.print(value_type);
    //    System.out.print("matchType");
    //    System.out.print(column.getType());
    //    System.out.print("matchType");
    //    System.out.print(value.getString());
    switch (column.getType()) {
      case INT:
        if (value_type == LiteralValueToken.Type.FLOAT_OR_DOUBLE) {
          try {
            int tmp = Integer.parseInt(value.getString());
            entries.add(new Entry(tmp));
          } catch (NumberFormatException e) {
            System.out.print(wrongColumnType);
            throw new WrongInsertException(wrongColumnType);
          }
        } else if (value_type == LiteralValueToken.Type.NULL) {
          if (column.isNotNull()) {
            System.out.print(
                "Exception: wrong insert operation ( " + column.getName() + " cannot be null)");
            throw new WrongInsertException(
                "Exception: wrong insert operation ( " + column.getName() + " cannot be null)");
          }
          entries.add((new Entry(null)));
        } else {
          System.out.print(wrongColumnType);
          throw new WrongInsertException(wrongColumnType);
        }
        break;
      case LONG:
        if (value_type == LiteralValueToken.Type.FLOAT_OR_DOUBLE) {
          try {
            long tmp = Long.parseLong(value.getString());
            entries.add(new Entry(tmp));
          } catch (NumberFormatException e) {
            System.out.print(wrongColumnType);
            throw new WrongInsertException(wrongColumnType);
          }
        } else if (value_type == LiteralValueToken.Type.NULL) {
          if (column.isNotNull()) {
            System.out.print(
                "Exception: wrong insert operation ( " + column.getName() + " cannot be null)");
            throw new WrongInsertException(
                "Exception: wrong insert operation ( " + column.getName() + " cannot be null)");
          }
          entries.add((new Entry(null)));
        } else {
          System.out.print(wrongColumnType);
          throw new WrongInsertException(wrongColumnType);
        }
        break;
      case DOUBLE:
        if (value_type == LiteralValueToken.Type.FLOAT_OR_DOUBLE
            || value_type == LiteralValueToken.Type.INT_OR_LONG) {
          try {
            double tmp = Double.parseDouble(value.getString());
            entries.add(new Entry(tmp));
          } catch (NumberFormatException e) {
            throw e;
          }
        } else if (value_type == LiteralValueToken.Type.NULL) {
          if (column.isNotNull()) {
            System.out.print(
                "Exception: wrong insert operation ( " + column.getName() + " cannot be null)");
            throw new WrongInsertException(
                "Exception: wrong insert operation ( " + column.getName() + " cannot be null)");
          }
          entries.add((new Entry(null)));
        } else {
          System.out.print(wrongColumnType);
          throw new WrongInsertException(wrongColumnType);
        }
        break;
      case FLOAT:
        if (value_type == LiteralValueToken.Type.FLOAT_OR_DOUBLE
            || value_type == LiteralValueToken.Type.INT_OR_LONG) {
          try {
            float tmp = Float.parseFloat(value.getString());
            entries.add(new Entry(tmp));
          } catch (NumberFormatException e) {
            throw e;
          }
        } else if (value_type == LiteralValueToken.Type.NULL) {
          if (column.isNotNull()) {
            System.out.print(
                "Exception: wrong insert operation ( " + column.getName() + " cannot be null)");
            throw new WrongInsertException(
                "Exception: wrong insert operation ( " + column.getName() + " cannot be null)");
          }
          entries.add((new Entry(null)));
        } else {
          System.out.print(wrongColumnType);
          throw new WrongInsertException(wrongColumnType);
        }
        break;
      case STRING:
        if (value_type == LiteralValueToken.Type.STRING) {
          if (value.getString().length() > column.getMaxLength()) {
            System.out.print(wrongStringLength);
            throw new WrongInsertException(wrongStringLength);
          }
          entries.add(new Entry(value.getString()));
        } else if (value_type == LiteralValueToken.Type.NULL) {
          if (column.isNotNull()) {
            System.out.print(
                "Exception: wrong insert operation ( " + column.getName() + " cannot be null)");
            throw new WrongInsertException(
                "Exception: wrong insert operation ( " + column.getName() + " cannot be null)");
          }
          entries.add((new Entry(null)));
        } else {
          System.out.print(wrongColumnType);
          throw new WrongInsertException(wrongColumnType);
        }
        break;
    }
  }

  public LinkedList<String> getLog() {
    LinkedList<String> log = new LinkedList<>();
    for (Row row : rowsHasInsert) {
      log.add("INSERT " + tableName + " " + row.toString());
    }
    return log;
  }
}
