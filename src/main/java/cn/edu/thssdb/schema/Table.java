package cn.edu.thssdb.schema;

import cn.edu.thssdb.exception.*;
import cn.edu.thssdb.index.BPlusTree;
import cn.edu.thssdb.index.BPlusTreeIterator;
import cn.edu.thssdb.type.ColumnType;
import cn.edu.thssdb.utils.Global;
import cn.edu.thssdb.utils.Pair;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// import cn.edu.thssdb.type.ComparisonType;

// Table类是一个表，包含了表的所有信息，包括表名、列名、列类型、列是否为主键、列是否为非空、列是否为唯一、列的索引、列的值等
public class Table implements Iterable<Row> {
  public ReentrantReadWriteLock lock; // 可重入读写锁
  private String databaseName; // 数据库名
  public String tableName; // 表名
  private String folder; // 文件夹
  public ArrayList<Column> columns; // 列名
  public BPlusTree<Entry, Row> index; // 索引
  public int primaryIndex; // 主键索引
  private PersistStorage<Row> persistStorageData;
  private Meta tableMeta; // 表的元数据,用于持久化
  public ArrayList<Integer> fileRows; // 记录持久化文件中分别有多少行的list
  private int fileNow; // 记录现在的b+树使用的是哪个文件
  boolean needPersist = false; // 记录这个文件对应的表是否改动过，改动过则切换时需要persist

  /**
   * [method] 无metadata构造方法
   *
   * @param databaseName {String} 数据库名称
   * @param tableName {String} 表名称
   * @param columns {Column[]} 列定义表
   */
  public Table(String databaseName, String tableName, Column[] columns) {
    // TODO
    this.databaseName = databaseName;
    this.tableName = tableName;
    this.columns = new ArrayList<>(columns.length);
    for (Column column : columns) {
      this.columns.add(column);
    }
    this.index = new BPlusTree<>();
    this.lock = new ReentrantReadWriteLock();
    this.primaryIndex = -1;
    for (int i = 0; i < columns.length; i++) {
      if (columns[i].isPrimary()) {
        this.primaryIndex = i;
        break;
      }
    }
    this.fileRows = new ArrayList<>();
    this.fileRows.add(0);
    this.fileNow = 0;
  }

  /**
   * [method] 无metadata构造方法
   *
   * @param databaseName {String} 数据库名称
   * @param tableName {String} 表名称
   * @param columns {Column[]} 列定义表
   * @param primaryIndex {int} 主键索引
   */
  public Table(String databaseName, String tableName, Column[] columns, int primaryIndex) {
    initData(databaseName, tableName);
    this.lock = new ReentrantReadWriteLock();
    this.columns = new ArrayList<>(Arrays.asList(columns));
    this.primaryIndex = primaryIndex;
    this.index = new BPlusTree<>();
    this.fileRows = new ArrayList<>();
    this.fileRows.add(0);
    this.fileNow = 0;
    persistMeta();
  }

  /**
   * [method] 读取metadata构造方法
   *
   * @param databaseName {String} 数据库名称
   * @param tableName {String} 表名称
   */
  public Table(String databaseName, String tableName) {
    initData(databaseName, tableName);
    this.lock = new ReentrantReadWriteLock();
    this.columns = new ArrayList<>();
    this.index = new BPlusTree<>();
    this.fileNow = 0;
    recoverMeta();
    recover(deserialize());
  }

  private void persistMeta() throws CustomIOException {
    ArrayList<String> meta_data = new ArrayList<>();
    meta_data.add(Global.DATABASE_META + " " + databaseName);
    meta_data.add(Global.TABLE_META + " " + tableName);
    String toString = fileRows.toString(); // ArrayList转换为String，带中括号的
    String substring = toString.substring(1, toString.length() - 1); // 去除中括号。小心java的substring是左闭右开的
    String result = substring.replaceAll("\\s", ""); // 去除所有的空字符
    System.out.print(result);
    meta_data.add(result);
    meta_data.add(Global.PRIMARY_INDEX_META + " " + primaryIndex);
    for (Column column : columns) {
      meta_data.add(column.toString(" "));
    }
    this.tableMeta.writeToFile(meta_data);
  }

  /**
   * [method] 恢复metadata
   *
   * @exception MetaFileNotFoundException, CustomIOException
   */
  private void recoverMeta() {
    // TODO
    ArrayList<String[]> meta_data = this.tableMeta.readFromFile();
    try {
      String[] fdatabase = meta_data.get(0);
      if (!fdatabase[0].equals(Global.DATABASE_META)) {
        throw new WrongMetaFormatException();
      }
      if (!this.databaseName.equals(fdatabase[1])) {
        throw new WrongMetaFormatException();
      }
    } catch (Exception e) {
      throw new WrongMetaFormatException();
    }

    try {
      String[] ftable = meta_data.get(1);
      if (!ftable[0].equals(Global.TABLE_META)) {
        throw new WrongMetaFormatException();
      }
      if (!this.tableName.equals(ftable[1])) {
        throw new WrongMetaFormatException();
      }
    } catch (Exception e) {
      throw new WrongMetaFormatException();
    }
    try {
      String[] frows = meta_data.get(2);
      String[] split = frows[0].split(","); // 转为String[]
      // 再将String[]转为Integer[]
      Integer[] ints1 = new Integer[split.length];
      for (int i = 0; i < ints1.length; i++) {
        ints1[i] = Integer.parseInt(split[i]);
      }
      this.fileRows = new ArrayList<>(Arrays.asList(ints1));
    } catch (Exception e) {
      throw new WrongMetaFormatException();
    }
    try {
      String[] fprimary_index = meta_data.get(3);
      if (!fprimary_index[0].equals(Global.PRIMARY_INDEX_META)) {
        throw new WrongMetaFormatException();
      }
      this.primaryIndex = Integer.parseInt(fprimary_index[1]);
    } catch (Exception e) {
      throw new WrongMetaFormatException();
    }
    for (int i = 4; i < meta_data.size(); i++) {
      String[] column_info = meta_data.get(i);
      try { // 这里需要根据column的tostring来处理
        String name = column_info[0];
        ColumnType type = ColumnType.string2ColumnType(column_info[1]);
        boolean primary = column_info[2].equals("true");
        boolean notNull = column_info[3].equals("true");
        int maxLength = Integer.parseInt(column_info[4]);
        this.columns.add(new Column(name, type, primary, notNull, maxLength));
      } catch (Exception e) {
        throw new WrongMetaFormatException();
      }
    }
  }

  /**
   * [method] 初始化元数据和数据存储相关
   *
   * @param databaseName {String} 数据库名称
   * @param tableName {String} 表名称
   * @exception IllegalArgumentException
   */
  private void initData(String databaseName, String tableName) throws CustomIOException {
    this.databaseName = databaseName;
    this.tableName = tableName;
    folder = Paths.get(Global.DATA_ROOT_FOLDER, databaseName, tableName).toString();
    String meta_name = tableName + ".meta";
    String data_name = tableName;
    this.persistStorageData = new PersistStorage<>(folder, data_name);
    this.tableMeta = new Meta(folder, meta_name);
  }

  public synchronized void persist() {
    Thread t =
        new Thread(
            new Runnable() {
              public void run() {
                serialize();
                persistMeta(); // 执行需要等待的函数
              }
            });
    t.start(); // 启动线程
    // 等待线程执行完毕
    try {
      t.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /** [method] 恢复表 [note] 从持久化数据中恢复表 */
  private synchronized void recover(ArrayList<Row> rows) {
    for (Row row : rows) {
      index.put(row.getEntries().get(primaryIndex), row); //
    }
  }

  public void insert(Row row) {
    // TODO
    // index.put(row.getEntries().get(primaryIndex), row);
    // persist();
    int oldNow = fileNow;
    try {
      if (index.contains(row.getEntries().get(primaryIndex))) {
        throw new WrongInsertException("");
      }
      for (int i = (oldNow + 1) % fileRows.size(); i % fileRows.size() != oldNow; i++) {
        changeto(i % fileRows.size());
        if (index.contains(row.getEntries().get(primaryIndex))) {
          throw new WrongInsertException("");
        }
      }
    } finally {
      if (fileRows.size() != 1) {
        changeto(oldNow);
      }
    }
    if (fileRows.get(fileNow) + 1 <= Global.ROWS_A_FILE) {
      fileRows.set(fileNow, fileRows.get(fileNow) + 1);
      index.put(row.getEntries().get(primaryIndex), row);
      needPersist = true;
    } else {
      for (int j = 0; j < fileRows.size(); j++) {
        if (fileRows.get(j) < Global.ROWS_A_FILE) {
          if (needPersist) {
            persist();
          }
          index = new BPlusTree<>();
          fileNow = j;
          recover(deserialize());
          fileRows.set(fileNow, fileRows.get(fileNow) + 1);
          index.put(row.getEntries().get(primaryIndex), row);
          needPersist = true;
          break;
        }
        if (j == fileRows.size() - 1) {
          if (needPersist) {
            persist();
          }
          fileRows.add(1);
          fileNow = fileRows.size() - 1;
          index = new BPlusTree<>();
          index.put(row.getEntries().get(primaryIndex), row);
          persist();
          needPersist = false;
          break;
        }
      }
    }
  }

  //  public void insert(ArrayList<Row> rows){
  //    if(rows.size()+fileRows.get(fileNow)>Global.ROWS_A_FILE){
  //      if(needPersist){
  //        persist();
  //      }
  //      fileRows.add(rows.size());
  //      fileNow = fileRows.size()-1;
  //      index = new BPlusTree<>();
  //      for(Row row : rows){
  //        index.put(row.getEntries().get(primaryIndex),row);
  //      }
  //    }
  //    else{
  //      fileRows.set(fileNow,fileRows.get(fileNow)+rows.size());
  //      for(Row row : rows){
  //        index.put(row.getEntries().get(primaryIndex),row);
  //      }
  //    }
  //  }

  public void insert(String row) {
    try {
      String[] values = row.split(",");
      ArrayList<Entry> entries = new ArrayList<>();
      int i = 0;
      for (Column c : columns) {
        entries.add(new Entry(ColumnType.getColumnTypeValue(c.getType(), values[i])));
        i++;
      }
      int oldNow = fileNow;
      try {
        if (index.contains(entries.get(primaryIndex))) {
          throw new WrongInsertException("");
        }
        for (int j = (oldNow + 1) % fileRows.size(); j % fileRows.size() != oldNow; j++) {
          changeto(j % fileRows.size());
          if (index.contains(entries.get(primaryIndex))) {
            throw new WrongInsertException("");
          }
        }
      } finally {
        if (fileRows.size() != 1) {
          changeto(oldNow);
        }
      }
      if (fileRows.get(fileNow) + 1 <= Global.ROWS_A_FILE) {
        fileRows.set(fileNow, fileRows.get(fileNow) + 1);
        needPersist = true;
        index.put(entries.get(primaryIndex), new Row(entries));
      } else {
        for (int j = 0; j < fileRows.size(); j++) {
          if (fileRows.get(j) < Global.ROWS_A_FILE) {
            if (needPersist) {
              persist();
            }
            index = new BPlusTree<>();
            fileNow = j;
            recover(deserialize());
            break;
          }
          if (j == fileRows.size() - 1) {
            if (needPersist) {
              persist();
            }
            fileRows.add(1);
            fileNow = fileRows.size() - 1;
            index = new BPlusTree<>();
            index.put(entries.get(primaryIndex), new Row(entries));
            persist();
            needPersist = false;
          }
        }
      }
    } catch (Exception e) {
      throw e;
    }
  }

  public void delete(Row row) {
    // TODO

    index.remove(row.getEntries().get(primaryIndex));
    fileRows.set(fileNow, fileRows.get(fileNow) - 1);
    needPersist = true;
  }

  public void delete(String pval) {
    ColumnType c = columns.get(primaryIndex).getType();
    Entry primaryEntry = new Entry(ColumnType.getColumnTypeValue(c, pval));
    index.remove(primaryEntry);
    fileRows.set(fileNow, fileRows.get(fileNow) - 1);
    needPersist = true;
  }

  public void delete() {
    needPersist = true;
    index.clear();
    index = new BPlusTree<>();
    fileRows.set(fileNow, 0);
  }

  public void deleteUndo(Row row) {
    int oldNow = fileNow;
    try {
      if (index.contains(row.getEntries().get(primaryIndex))) {
        index.remove(row.getEntries().get(primaryIndex));
        needPersist = true;
      } else {
        for (int j = (oldNow + 1) % fileRows.size(); j % fileRows.size() != oldNow; j++) {
          changeto(j % fileRows.size());
          if (index.contains(row.getEntries().get(primaryIndex))) {
            index.remove(row.getEntries().get(primaryIndex));
            needPersist = true;
            break;
          }
        }
      }
    } finally {
      if (fileRows.size() != 1) {
        changeto(oldNow);
      }
    }
  }

  public void update(Row oldRow, Row newRow) {
    //  主键不变不需要删除后插入
    if (oldRow.getEntries().get(primaryIndex).compareTo(newRow.getEntries().get(primaryIndex))
        == 0) {
      index.update(newRow.getEntries().get(primaryIndex), newRow);
    } else {
      try {
        delete(oldRow);
        insert(newRow);
      } catch (DuplicateKeyException e) {
        throw e;
      }
    }
    needPersist = true;
  }

  //  public void updateNoRemove(Row oldRow, Row newRow) {
  //    index.updateNoRemove(oldRow.getEntries().get(primaryIndex), newRow);
  //  }

  public void updateAll(int idx, Comparable val) {
    BPlusTreeIterator<Entry, Row> it = index.iterator();
    while (it.hasNext()) {
      it.next().right.getEntries().set(idx, new Entry(val));
    }
    needPersist = true;
  }

  public void updateAllData(Row oldRow, Row newRow) {
    int oldNow = fileNow;
    try {
      if (index.contains(oldRow.getEntries().get(primaryIndex))) {
        index.update(new Entry(oldRow.getEntries().get(primaryIndex)), newRow);
        needPersist = true;
      }
      for (int j = (oldNow + 1) % fileRows.size(); j % fileRows.size() != oldNow; j++) {
        changeto(j % fileRows.size());
        if (index.contains(oldRow.getEntries().get(primaryIndex))) {
          index.update(new Entry(oldRow.getEntries().get(primaryIndex)), newRow);
          needPersist = true;
        }
      }
    } finally {
      if (fileRows.size() != 1) {
        changeto(oldNow);
      }
    }
  }

  private void serialize() {
    persistStorageData.serialize(iterator(), fileNow);
  }

  private ArrayList<Row> deserialize() {
    final ArrayList<Row>[] result = new ArrayList[] {new ArrayList<>()};
    Thread t =
        new Thread(
            new Runnable() {
              public void run() {
                result[0] = persistStorageData.deserialize(fileNow); // 执行需要等待的函数
              }
            });
    t.start(); // 启动线程

    // 等待线程执行完毕
    try {
      t.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return result[0];
  }

  /** [method] 删除table（包括表本身） */
  public void drop() {
    for (int i = fileRows.size() - 1; i >= 0; i--) {
      this.index.clear();
      this.persistStorageData.deleteFile(i);
    }
    this.tableMeta.deleteFile();
    new File(this.folder).delete();
  }

  public void changeto(int fileName) {
    Iterator<Row> rowIterator = this.iterator();
    //    Thread t =
    //        new Thread(
    //            new Runnable() {
    //              public void run() {
    //                //        while(rowIterator.hasNext()){
    //                //          Row row = rowIterator.next();
    //                //          System.out.print(row.getEntries().get(primaryIndex));
    //                //        }
    //                if (needPersist) {
    //                  persist();
    //                }
    //                fileNow = fileName;
    //                index = new BPlusTree<>();
    //                recover(deserialize());
    //                needPersist = false;
    //              }
    //            });
    //    t.start(); // 启动线程
    //
    //    // 等待线程执行完毕
    //    try {
    //      t.join();
    //    } catch (InterruptedException e) {
    //      e.printStackTrace();
    //    }
    if (needPersist) {
      persist();
    }
    fileNow = fileName;
    index = new BPlusTree<>();
    recover(deserialize());
    needPersist = false;
  }

  private class TableIterator implements Iterator<Row> {
    private Iterator<Pair<Entry, Row>> iterator;

    TableIterator(Table table) {
      this.iterator = table.index.iterator();
    }

    @Override
    public boolean hasNext() {
      return iterator.hasNext();
    }

    @Override
    public Row next() {
      return iterator.next().right;
    }
  }

  public Table getColumnFullNameTable() {
    ArrayList<Column> newColumns = new ArrayList<>();
    for (Column column : columns) {
      String newColumnName = this.tableName + "." + column.getName();
      Column newColumn =
          new Column(
              newColumnName,
              column.getType(),
              column.isPrimary(),
              column.isNotNull(),
              column.getMaxLength());
      newColumns.add(newColumn);
    }
    Column[] newColumn = newColumns.toArray(new Column[0]);
    Table newTable = new Table(this.databaseName, this.tableName, newColumn);
    newTable.index = this.index;
    // newTable.lock = this.lock;?
    return newTable;
  }

  @Override
  public Iterator<Row> iterator() {
    return new TableIterator(this);
  }

  public String getTableName() {
    return tableName;
  }

  public ArrayList<Column> getColumns() {
    return columns;
  }

  public int getFileNow() {
    return fileNow;
  }
}
