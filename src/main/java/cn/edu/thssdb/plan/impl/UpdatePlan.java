package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.exception.*;
import cn.edu.thssdb.parser.token.LiteralValueToken;
import cn.edu.thssdb.parser.token.MultipleConditionToken;
import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.schema.*;
import cn.edu.thssdb.type.ColumnType;

import java.util.*;
import javafx.util.Pair;

public class UpdatePlan extends LogicalPlan {
  private String tableName;
  private String columnName;
  private LiteralValueToken literalValueToken;
  private MultipleConditionToken whereToken = null;
  private ArrayList<Pair<Row, Row>> rowsHasUpdate;
  private int columnIdxToUpdate;
  private Table table = null;

  static final String wrongColumnName = "Exception: wrong update operation ( no such column )!";
  static final String wrongColumnType = "Exception: wrong update operation ( type unmatched )!";
  static final String columnNotNull =
      "Exception: wrong update operation ( this column cannot be null )!"; // 列数与值数不匹配
  static final String duplicateKey =
      "Exception: wrong update operation ( update causes duplicate key )!";
  static final String wrongStringLength =
      "Exception: wrong update operation ( update causes string exceeds length limit )!";

  /** [method] 构造方法 */
  public UpdatePlan(String tableName, String columnName, LiteralValueToken literalValueToken) {
    super(LogicalPlanType.UPDATE);
    this.tableName = tableName;
    this.columnName = columnName;
    this.literalValueToken = literalValueToken;
    rowsHasUpdate = new ArrayList<>();
  }

  public UpdatePlan(
      String tableName,
      String columnName,
      LiteralValueToken literalValueToken,
      MultipleConditionToken multipleConditionToken) {
    super(LogicalPlanType.UPDATE);
    this.tableName = tableName;
    this.columnName = columnName;
    this.literalValueToken = literalValueToken;
    this.whereToken = multipleConditionToken;
    rowsHasUpdate = new ArrayList<>();
  }

  public void exec() {
    System.out.print(this.literalValueToken.getString());
    if (database == null) {
      throw new DatabaseNotExistException();
    }

    table = database.get(tableName);
    if (table == null) {
      throw new TableNotExistException();
    }

    ArrayList<Column> columns = table.getColumns();
    Column columnToUpdate = null;

    int primaryKeyIndex = -1;
    for (int i = 0; i < columns.size(); i++) {
      if (columns.get(i).getName().equals(columnName.toLowerCase())) {
        columnToUpdate = columns.get(i);
        columnIdxToUpdate = i;
        if (columnToUpdate.isPrimary()) {
          primaryKeyIndex = i;
        }
        break;
      }
    }
    System.out.print(primaryKeyIndex);
    if (columnToUpdate == null) {
      throw new WrongUpdateException(wrongColumnName);
    }
    System.out.print(columnToUpdate);
    LiteralValueToken.Type itemType = literalValueToken.getType();
    ColumnType columnType = columnToUpdate.getType();
    String itemString = literalValueToken.getString();
    Comparable valueToUpdate;
    System.out.print(itemType);
    System.out.print(columnType);

    if (itemType == LiteralValueToken.Type.FLOAT_OR_DOUBLE) {
      if (columnType == ColumnType.STRING) {
        throw new WrongUpdateException(wrongColumnType);
      }
      try {
        if (columnType == ColumnType.INT) {
          valueToUpdate = Integer.valueOf(itemString);
        } else if (columnType == ColumnType.LONG) {
          valueToUpdate = Long.valueOf(itemString);
        } else if (columnType == ColumnType.DOUBLE) {
          valueToUpdate = Double.valueOf(itemString);
        } else {
          valueToUpdate = Float.valueOf(itemString);
        }
      } catch (Exception e) {
        throw new WrongUpdateException(wrongColumnType);
      }
    } else if (itemType == LiteralValueToken.Type.STRING) {
      if (columnType == ColumnType.STRING) {
        if (columnToUpdate.getMaxLength() < itemString.length()) {
          throw new WrongUpdateException(wrongStringLength);
        }
        valueToUpdate = itemString;
      } else {
        throw new WrongUpdateException(wrongColumnType);
      }
    } else {
      if (columnToUpdate.isNotNull()) {
        throw new WrongUpdateException(columnNotNull);
      }
      valueToUpdate = null;
    }
    int oldNow = table.getFileNow();
    int rowsum = 0;
    for (int i = 0; i < table.fileRows.size(); i++) {
      rowsum += table.fileRows.size();
    }
    for (int i = oldNow; i % table.fileRows.size() != oldNow || i == oldNow; i++) {
      if (table.fileRows.get(i) == 0) continue;
      if (i != oldNow) table.changeto(i);
      Iterator<Row> rowIterator = table.iterator();
      if (whereToken == null) { // 更新所有行
        if (primaryKeyIndex != -1) { // 需要对主键进行更新
          if (rowsum > 1) { // 如果size大于1，就会有多个主键是相同值
            throw new WrongUpdateException(duplicateKey);
          } else {
            if (rowIterator.hasNext()) {
              Row oldRow = rowIterator.next();
              Row newRow = getNewRow(oldRow, valueToUpdate);
              if (newRow != null) {
                table.update(oldRow, newRow);
                rowsHasUpdate.add(new Pair<>(oldRow, newRow));
              }
            }
          }
        } else {
          while (rowIterator.hasNext()) {
            Row oldRow = rowIterator.next();
            Row newRow = getNewRow(oldRow, valueToUpdate);
            if (newRow == null) {
              continue;
            }
            rowsHasUpdate.add(new Pair<>(oldRow, newRow));
          }
          table.updateAll(columnIdxToUpdate, valueToUpdate);
        }
      } else {
        ArrayList<String> columnNames = new ArrayList<>();
        for (Column column : table.columns) {
          columnNames.add(column.getName());
        }
        if (primaryKeyIndex != -1) {
          Entry entryToUpdate = new Entry(valueToUpdate);
          while (rowIterator.hasNext()) {
            Row oldRow = rowIterator.next();
            if (whereToken.evaluate(oldRow, columnNames)) {
              Row newRow = getNewRow(oldRow, valueToUpdate);
              if (newRow == null) {
                continue;
              }
              //                            for(int j=0;j<table.fileRows.size();j++){
              //                                if(table.fileRows.get(i)==0)continue;
              //                                table.changeto(j);
              //                                if(table.index.contains(entryToUpdate)){
              //                                    undo();
              //                                    throw new WrongUpdateException(duplicateKey);
              //                                }
              //                            }
              if (table.index.contains(entryToUpdate)) {
                undo();
                throw new WrongUpdateException(duplicateKey);
              }
              table.changeto(i);
              table.update(oldRow, newRow);
              rowsHasUpdate.add(new Pair<>(oldRow, newRow));
            }
          }
        } else {
          while (rowIterator.hasNext()) {
            Row oldRow = rowIterator.next();
            if (whereToken.evaluate(oldRow, columnNames)) {
              Row newRow = getNewRow(oldRow, valueToUpdate);
              if (newRow == null) {
                continue;
              }
              table.update(oldRow, newRow);
              rowsHasUpdate.add(new Pair<>(oldRow, newRow));
            }
          }
        }
      }
    }
    table.changeto(oldNow);
  }

  /** [method] 撤销操作 */
  public void undo() {
    for (int i = rowsHasUpdate.size() - 1; i >= 0; i--) {
      table.updateAllData(rowsHasUpdate.get(i).getValue(), rowsHasUpdate.get(i).getKey());
    }
  }

  private Row getNewRow(Row oldRow, Comparable valueToUpdate) {
    ArrayList<Entry> entries = new ArrayList<>();
    ArrayList<Entry> old_entries = oldRow.getEntries();
    for (Entry e : old_entries) {
      entries.add(new Entry(e.value));
    }
    Entry tmp = new Entry(valueToUpdate);
    Entry old = old_entries.get(columnIdxToUpdate);
    if (old.value != null && old.compareTo(tmp) == 0) { // 更新行与旧行相同
      return null;
    } else {
      entries.set(columnIdxToUpdate, new Entry(valueToUpdate));
    }
    Row newRow = new Row(entries);
    return newRow;
  }

  /** [method] 获取记录 */
  public LinkedList<String> getLog() {
    LinkedList<String> deleteLog = new LinkedList<>();
    LinkedList<String> insertLog = new LinkedList<>();
    int primaryIndex = table.primaryIndex;
    for (Pair<Row, Row> pair : rowsHasUpdate) {
      deleteLog.add(
          "DELETE " + tableName + " " + pair.getValue().getEntries().get(primaryIndex).toString());
      insertLog.add("INSERT " + tableName + " " + pair.getKey().toString());
    }
    deleteLog.addAll(insertLog);
    return deleteLog;
  }

  @Override
  public ArrayList<String> getTableName() {
    return new ArrayList<>(Arrays.asList(this.tableName));
  }
}
