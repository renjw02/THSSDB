package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.exception.DatabaseNotExistException;
import cn.edu.thssdb.exception.TableNotExistException;
import cn.edu.thssdb.parser.token.MultipleConditionToken;
import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.schema.*;

import java.util.*;

public class DeletePlan extends LogicalPlan {
  private String tableName;
  private MultipleConditionToken whereToken = null; // null则删除所有
  private ArrayList<Row> rowsHasDelete;
  private Table table;
  private int primaryIndex;

  /** [method] 构造方法 */
  public DeletePlan(String tableName) {
    super(LogicalPlanType.DELETE);
    this.tableName = tableName;
    rowsHasDelete = new ArrayList<>();
  }

  public DeletePlan(String tableName, MultipleConditionToken whereToken) {
    super(LogicalPlanType.DELETE);
    this.tableName = tableName;
    this.whereToken = whereToken;
    rowsHasDelete = new ArrayList<>();
  }

  /** [method] 执行操作 */
  public void exec() {
    if (database == null) {
      throw new DatabaseNotExistException();
    }
    table = database.get(tableName);
    if (table == null) {
      throw new TableNotExistException();
    } else {
      int oldNow = table.getFileNow();
      for (int i = oldNow; i % table.fileRows.size() != oldNow || i == oldNow; i++) {
        if (table.fileRows.get(i) == 0) continue;
        if (i != oldNow) table.changeto(i);
        Iterator<Row> rowIterator = table.iterator();
        if (whereToken == null) {
          while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            rowsHasDelete.add(row);
          }
          table.delete();
        } else {
          ArrayList<String> columnNames = new ArrayList<>();
          for (Column column : table.columns) {
            columnNames.add(column.getName());
          }
          while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            if (whereToken.evaluate(row, columnNames)) {
              table.delete(row);
              rowsHasDelete.add(row);
            }
          }
        }
      }
      table.changeto(oldNow);
    }
  }

  public void undo() { // TODO  也许已经删除的行再插入不会出现主键重复的情况，所以可以直接插入
    for (Row row : rowsHasDelete) {
      table.insert(row);
    }
  }

  public LinkedList<String> getLog() {
    LinkedList<String> log = new LinkedList<>();
    primaryIndex = table.primaryIndex;
    for (Row row : rowsHasDelete) {
      log.add("DELETE " + tableName + " " + row.getEntries().get(primaryIndex).toString());
    }
    return log;
  }

  @Override
  public ArrayList<String> getTableName() {
    return new ArrayList<>(Arrays.asList(this.tableName));
  }
}
