package cn.edu.thssdb.query;

import cn.edu.thssdb.schema.Column;
import cn.edu.thssdb.schema.Entry;
import cn.edu.thssdb.schema.Row;
import cn.edu.thssdb.schema.Table;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class QueryTable implements Iterator<Row> {
  public List<Row> results;
  public List<Column> columns;

  private String folder; // 文件夹
  public ArrayList<Integer> fileRows; // 记录持久化文件中分别有多少行的list
  private int fileNow; // 记录现在的b+树使用的是哪个文件

  public QueryTable(Table table) {
    // TODO
    results = new ArrayList<>();
    columns = new ArrayList<>();
    columns.addAll(table.columns);
    //    int oldNow = table.getFileNow();
    //    for (int i = 0; i < table.fileRows.size(); i++) {
    ////      if (table.fileRows.get(i) == 0) continue;
    ////      table.changeto(i);
    //      for (Row row : table) {
    //        results.add(row);
    //      }
    //    }
    for (Row row : table) {
      results.add(row);
    }
    //    table.changeto(oldNow);
  }

  public QueryTable(List<Row> rows, List<Column> columns) {
    this.results = new ArrayList<>();
    this.columns = new ArrayList<>();
    this.results.addAll(rows);
    this.columns.addAll(columns);
  }

  @Override
  public boolean hasNext() {
    // TODO
    return results.iterator().hasNext();
  }

  @Override
  public Row next() {
    // TODO
    return results.iterator().next();
  }

  public int Column2Index(String columnName) {
    ArrayList<String> columnNames = new ArrayList<>();
    for (Column column : this.columns) {
      columnNames.add(column.getName());
    }
    return columnNames.indexOf(columnName);
  }
  // 笛卡尔积，列名是ColumnFullName
  public QueryTable combineQueryTable(QueryTable queryTable) {
    List<Row> newRowList = new ArrayList<>();
    List<Column> newColumnList = new ArrayList<>(this.columns);
    newColumnList.addAll(queryTable.columns);
    for (Row row1 : this.results) {
      if (queryTable.results.size() == 0) {
        Row newRow = new Row(row1);
        ArrayList<Entry> nullEntries = new ArrayList<>();
        for (int i = 0; i < queryTable.columns.size(); i++) {
          nullEntries.add(new Entry("null"));
        }
        newRow.appendEntries(nullEntries);
        newRowList.add(newRow);
      } else {
        for (Row row2 : queryTable.results) {
          Row newRow = new Row(row1);
          newRow.appendEntries(row2.getEntries());
          newRowList.add(newRow);
        }
      }
    }
    return new QueryTable(newRowList, newColumnList);
  }

  public QueryTable join(Table table) {
    Table newTable = table.getColumnFullNameTable();
    QueryTable newTargetTable = new QueryTable(newTable);
    return this.combineQueryTable(newTargetTable);
  }
}
