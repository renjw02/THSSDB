package cn.edu.thssdb.query;

import cn.edu.thssdb.schema.Column;

import java.util.ArrayList;
import java.util.List;

class MetaInfo {

  private String tableName;
  private List<Column> columns;

  /**
   * MetaInfo是一个表的元信息，包括表名和列名
   *
   * @param tableName 表名
   * @param columns 列名
   */
  MetaInfo(String tableName, ArrayList<Column> columns) {
    this.tableName = tableName;
    this.columns = columns;
  }

  /**
   * columnFind用于查找列名在表中的位置
   *
   * @param name 列名
   * @return 列名在表中的索引
   */
  int columnFind(String name) {
    // TODO
    // 根据列名查找列在表中的位置
    for (int i = 0; i < columns.size(); i++) {
      if (columns.get(i).getName().equals(name)) {
        return i;
      }
    }
    return -1;
  }
}
