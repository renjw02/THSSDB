package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.exception.DatabaseNotExistException;
import cn.edu.thssdb.exception.TableNotExistException;
import cn.edu.thssdb.parser.token.*;
import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.query.QueryResult;
import cn.edu.thssdb.query.QueryTable;
import cn.edu.thssdb.schema.Column;
import cn.edu.thssdb.schema.Entry;
import cn.edu.thssdb.schema.Row;
import cn.edu.thssdb.schema.Table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class SelectPlan extends LogicalPlan {
  private SelectContentToken selectContentToken;
  private FromContentToken fromContentToken;
  private MultipleConditionToken whereToken;
  private String stmt;

  List<List<String>> data = new ArrayList<>();
  List<String> columns = new ArrayList<>();

  ArrayList<FromToken> fromTokenList = new ArrayList<>();

  ArrayList<Row> finalRowList = new ArrayList<>();
  ArrayList<Column> finalColumnList = new ArrayList<>();

  public SelectPlan(
      SelectContentToken selectContentItem,
      FromContentToken fromItem,
      MultipleConditionToken whereItem,
      String stmt) {
    super(LogicalPlanType.SELECT);
    this.selectContentToken = selectContentItem;
    this.fromContentToken = fromItem;
    this.whereToken = whereItem;
    this.stmt = stmt;
  }

  /** [method] 执行操作 */
  public void exec() {
    //    System.out.println(stmt);
    if (database == null) {
      throw new DatabaseNotExistException();
    }

    fromTokenList = fromContentToken.getFromContent();
    //    System.out.println("fromTokenList size:" + fromTokenList.size());
    //    System.out.println(fromToken.getTableNameA());
    //    System.out.println(fromToken.getTableNameB());

    ArrayList<Table> tables = new ArrayList<>();
    ArrayList<String> tableNames = new ArrayList<>();
    for (FromToken fromToken : fromTokenList) {
      String tableName1 = fromToken.getTableNameA();
      String tableName2 = fromToken.getTableNameB();
      if (!tableNames.contains(tableName1)) {
        Table table1 = database.get(tableName1);
        if (table1 == null) {
          throw new TableNotExistException(tableName1);
        } else {
          tables.add(table1);
          tableNames.add(tableName1);
        }
      }
      if (tableName2 != null) {
        if (!tableNames.contains(tableName2)) {
          Table table2 = database.get(tableName2);
          if (table2 == null) {
            throw new TableNotExistException(tableName2);
          } else {
            tables.add(table2);
            tableNames.add(tableName2);
          }
        }
      }
    }
    tableNames = null;

    int tableNum = tables.size();
    Table firstTable = tables.get(0);
    System.out.print("firsttable " + firstTable.index.size() + '\n');
    QueryTable targetTable = new QueryTable(firstTable);
    System.out.print("querytable" + targetTable.results.size() + '\n');
    if (tableNum > 1) {
      int oldNow = firstTable.getFileNow();
      for (int i = 0; i < firstTable.fileRows.size(); i++) {
        if (firstTable.fileRows.get(i) == 0) continue;
        firstTable.changeto(i);

        targetTable = new QueryTable(firstTable.getColumnFullNameTable());
        process(tables, targetTable, 1);
      }
      firstTable.changeto(oldNow);
    } else {
      int oldNow = firstTable.getFileNow();
      for (int i = 0; i < firstTable.fileRows.size(); i++) {
        if (firstTable.fileRows.get(i) == 0) continue;
        firstTable.changeto(i);
        //        for (Row row : firstTable) {
        //          System.out.print(row.toString());
        //        }
        targetTable = new QueryTable(firstTable);
        process(tables, targetTable, 1);
      }
      firstTable.changeto(oldNow);
    }

    //    Table firstTable = tables.get(0);
    //    QueryTable targetTable = new QueryTable(firstTable);
    //    if (tables.size() > 1) { // TODO
    //      //      System.out.println(tables.size());
    //      targetTable = new QueryTable(firstTable.getColumnFullNameTable());
    //      for (int i = 1; i < tables.size(); i++) {
    //        targetTable = targetTable.join(tables.get(i));
    //      }
    //    }

    //    System.out.println("successfully execute from part!");
    //    System.out.println("target table:");
    //    for (Column column : targetTable.columns) {
    //      System.out.println(column.getName());
    //    }
    //    for (Row row : targetTable.results) {
    //      for (Entry entry : row.getEntries()) {
    //        System.out.println(entry.value);
    //      }
    //    }

    //    System.out.println("进入on:");
    //    for (FromToken fromToken : fromTokenList) {
    //      MultipleConditionToken onToken = fromToken.getMultipleConditionToken();
    //      if (onToken != null) {
    //        //        System.out.println("执行on" + fromTokenList.indexOf(fromToken));
    //        Iterator<Row> rowIterator = targetTable.results.iterator();
    //        ArrayList<String> columnNames = new ArrayList<>();
    //        for (Column column : targetTable.columns) {
    //          columnNames.add(column.getName());
    //        }
    //        List<Row> rowToDelete = new ArrayList<>();
    //        while (rowIterator.hasNext()) {
    //          Row row = rowIterator.next();
    //          if (!onToken.evaluate(row, columnNames)) {
    //            rowToDelete.add(row);
    //          }
    //        }
    //        targetTable.results.removeAll(rowToDelete);
    //      }
    //    }
    //
    //    //    System.out.println("successfully execute on part!");
    //
    //    //    System.out.println("进入where:");
    //    if (whereToken != null) {
    //      Iterator<Row> rowIterator = targetTable.results.iterator();
    //      ArrayList<String> columnNames = new ArrayList<>();
    //      for (Column column : targetTable.columns) {
    //        columnNames.add(column.getName());
    //      }
    //      List<Row> rowToDelete = new ArrayList<>();
    //      while (rowIterator.hasNext()) {
    //        Row row = rowIterator.next();
    //        if (!whereToken.evaluate(row, columnNames)) {
    //          rowToDelete.add(row);
    //        }
    //      }
    //      targetTable.results.removeAll(rowToDelete);
    //    }
    //
    //    //    System.out.println("successfully execute where part!");
    //
    //    ArrayList<Column> selectedColumns = new ArrayList<>();
    //    ArrayList<Row> rowList = new ArrayList<>();
    //    SelectToken firstSelectToken = selectContentToken.getSelectContent().get(0);
    //    if (firstSelectToken.getTableName() == null &&
    // firstSelectToken.getColumnName().equals("*")) {
    //      //      System.out.println("000");
    //      selectedColumns.addAll(targetTable.columns);
    //      rowList.addAll(targetTable.results);
    //    } else {
    //      //      System.out.println("111");
    //      ArrayList<String> selectColumnName = new ArrayList<>();
    //      for (SelectToken selectToken : selectContentToken.getSelectContent()) {
    //        if (selectToken.getTableName() != null) { // 多个表
    //          // A.*的情况暂不考虑
    //          String columnName = selectToken.getTableName() + "." + selectToken.getColumnName();
    //          selectColumnName.add(columnName);
    //        } else { // 一个表
    //          selectColumnName.add(selectToken.getColumnName());
    //        }
    //      }
    //      //      System.out.println("222");
    //      // 获取selectColumnName对应的index
    //      ArrayList<Integer> selectColumnIndex = new ArrayList<>();
    //      for (String columnName : selectColumnName) {
    //        int index = targetTable.Column2Index(columnName);
    //        selectedColumns.add(targetTable.columns.get(index));
    //        selectColumnIndex.add(index);
    //      }
    //      //      System.out.println("333");
    //      // 再对行按列筛选
    //      for (Row row : targetTable.results) {
    //        ArrayList<Entry> Entries = row.getEntries();
    //        ArrayList<Entry> newEntries = new ArrayList<>();
    //        for (int i = 0; i < Entries.size(); i++) {
    //          if (selectColumnIndex.contains(i)) {
    //            // System.out.println(i+"is in selectColumnIndex");
    //            newEntries.add(Entries.get(i));
    //          }
    //        }
    //        Row newRow = new Row(newEntries);
    //        rowList.add(newRow);
    //      }
    //      //      System.out.println("444");
    //    }

    //    System.out.println("successfully execute select part!");

    //    System.out.println("results:");
    //    for (Column column : selectedColumns) {
    //      System.out.println(column.getName());
    //    }
    //    for (Row row : rowList) {
    //      for (Entry entry : row.getEntries()) {
    //        System.out.println(entry.value);
    //      }
    //    }

    QueryTable queryTable = new QueryTable(finalRowList, finalColumnList);
    QueryTable[] queryTables = {queryTable};
    QueryResult queryResult = new QueryResult(queryTables);
    //    System.out.println("464");

    this.data = queryResult.getResults();
    this.columns = queryResult.getColumnNames();
  }

  @Override
  public ArrayList<String> getTableName() {
    FromToken fromToken = fromContentToken.getFromContent().get(0);
    return new ArrayList<>(Arrays.asList(fromToken.getTableNameA(), fromToken.getTableNameB()));
  }

  @Override
  public String getMessage() {
    return "successfully execute select!";
  }

  public List<List<String>> getData() {
    return data;
  }

  public List<String> getColumns() {
    return columns;
  }

  public String getStmt() {
    return stmt;
  }

  public void process(ArrayList<Table> tables, QueryTable inputTable, int index) {
    if (index == tables.size()) {
      System.out.print("handle\n");
      handle(inputTable);
      return;
    }

    Table currentTable = tables.get(index);
    int oldNow = currentTable.getFileNow();
    for (int i = 0; i < currentTable.fileRows.size(); i++) {
      if (currentTable.fileRows.get(i) == 0) continue;
      currentTable.changeto(i);

      QueryTable newTable = inputTable.join(currentTable);
      process(tables, newTable, index + 1);
    }
    currentTable.changeto(oldNow);
  }

  public void handle(QueryTable targetTable) {
    System.out.print("asd");
    System.out.print(targetTable.results.size());
    //    for (Row row : targetTable.results) {
    //      System.out.print(row.toString()+'\n');
    //    }

    for (FromToken fromToken : fromTokenList) {
      MultipleConditionToken onToken = fromToken.getMultipleConditionToken();
      if (onToken != null) {
        //        System.out.println("执行on" + fromTokenList.indexOf(fromToken));
        Iterator<Row> rowIterator = targetTable.results.iterator();
        ArrayList<String> columnNames = new ArrayList<>();
        for (Column column : targetTable.columns) {
          columnNames.add(column.getName());
        }
        List<Row> rowToDelete = new ArrayList<>();
        while (rowIterator.hasNext()) {
          Row row = rowIterator.next();
          if (!onToken.evaluate(row, columnNames)) {
            rowToDelete.add(row);
          }
        }
        targetTable.results.removeAll(rowToDelete);
      }
    }

    //    System.out.println("successfully execute on part!");

    //    System.out.println("进入where:");
    if (whereToken != null) {
      Iterator<Row> rowIterator = targetTable.results.iterator();
      ArrayList<String> columnNames = new ArrayList<>();
      for (Column column : targetTable.columns) {
        columnNames.add(column.getName());
      }
      List<Row> rowToDelete = new ArrayList<>();
      while (rowIterator.hasNext()) {
        Row row = rowIterator.next();
        if (!whereToken.evaluate(row, columnNames)) {
          rowToDelete.add(row);
        }
      }
      targetTable.results.removeAll(rowToDelete);
    }

    //    System.out.println("successfully execute where part!");

    ArrayList<Column> selectedColumns = new ArrayList<>();
    ArrayList<Row> rowList = new ArrayList<>();
    SelectToken firstSelectToken = selectContentToken.getSelectContent().get(0);
    if (firstSelectToken.getTableName() == null && firstSelectToken.getColumnName().equals("*")) {
      //      System.out.println("000");
      selectedColumns.addAll(targetTable.columns);
      rowList.addAll(targetTable.results);
    } else {
      //      System.out.println("111");
      ArrayList<String> selectColumnName = new ArrayList<>();
      for (SelectToken selectToken : selectContentToken.getSelectContent()) {
        if (selectToken.getTableName() != null) { // 多个表
          // A.*的情况暂不考虑
          String columnName = selectToken.getTableName() + "." + selectToken.getColumnName();
          selectColumnName.add(columnName);
        } else { // 一个表
          selectColumnName.add(selectToken.getColumnName());
        }
      }
      //      System.out.println("222");
      // 获取selectColumnName对应的index
      ArrayList<Integer> selectColumnIndex = new ArrayList<>();
      for (String columnName : selectColumnName) {
        int index = targetTable.Column2Index(columnName);
        selectedColumns.add(targetTable.columns.get(index));
        selectColumnIndex.add(index);
      }
      //      System.out.println("333");
      // 再对行按列筛选
      for (Row row : targetTable.results) {
        ArrayList<Entry> Entries = row.getEntries();
        ArrayList<Entry> newEntries = new ArrayList<>();
        for (int i = 0; i < Entries.size(); i++) {
          if (selectColumnIndex.contains(i)) {
            // System.out.println(i+"is in selectColumnIndex");
            newEntries.add(Entries.get(i));
          }
        }
        Row newRow = new Row(newEntries);
        rowList.add(newRow);
      }
      //      System.out.println("444");
    }

    if (finalColumnList.size() == 0) {
      finalColumnList.addAll(selectedColumns);
    }
    finalRowList.addAll(rowList);
  }

  // ....
}
