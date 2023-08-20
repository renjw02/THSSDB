package cn.edu.thssdb.query;

import cn.edu.thssdb.schema.Column;
import cn.edu.thssdb.schema.Entry;
import cn.edu.thssdb.schema.Row;
import cn.edu.thssdb.utils.Cell;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class QueryResult {

  private List<MetaInfo> metaInfoInfos;
  private List<Integer> index; // index是一个表的索引，用于快速查找
  private List<Cell> attrs; // Cell是一个元组，包含了一个表的一行数据
  public final QueryResultType resultType;
  public final String errorMessage; // If it is an error.
  private List<String> columnNames;

  private List<List<String>> results;

  public enum QueryResultType {
    SELECT,
    MESSAGE
  }

  public List<String> getColumnNames() {
    return columnNames;
  }

  public List<List<String>> getResults() {
    return results;
  }

  /**
   * 构造函数
   *
   * @param queryTables
   */
  public QueryResult(QueryTable[] queryTables) {
    // TODO
    this.index = new ArrayList<>();
    this.attrs = new ArrayList<>();
    this.resultType = QueryResultType.SELECT;
    this.errorMessage = null;
    this.results = new ArrayList<>();
    //    this.results = queryTables[0].results;
    for (Row row : queryTables[0].results) {
      List<String> dataRow = new ArrayList<>();
      for (Entry entry : row.getEntries()) {
        //        System.out.println(entry.getValueType());
        //        System.out.println(entry.toString());
        dataRow.add(entry.toString());
      }
      results.add(dataRow);
    }
    this.columnNames = new ArrayList<>();
    for (Column column : queryTables[0].columns) {
      columnNames.add(column.getName());
    }
  }

  public QueryResult(String errorMessage) {
    resultType = QueryResultType.MESSAGE;
    this.errorMessage = errorMessage;
  }

  /**
   * combineRow用于将多个表的一行数据合并成一个表的一行数据
   *
   * @param rows
   * @return
   */
  public static Row combineRow(LinkedList<Row> rows) {
    // TODO
    return null;
  }

  /**
   * generateQueryRecord用于将一行数据转换成一个元组
   *
   * @param row
   * @return
   */
  public Row generateQueryRecord(Row row) {
    // TODO
    return null;
  }
}
