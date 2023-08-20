package cn.edu.thssdb.schema;

import cn.edu.thssdb.exception.*;
import cn.edu.thssdb.query.QueryResult;
import cn.edu.thssdb.query.QueryTable;
import cn.edu.thssdb.utils.Global;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** Database类是一个数据库，包含了数据库的所有信息，数据库名以及以哈希表的形式存储的Table对象，以及一个读写锁 */
public class Database {

  private String name; // 数据库名称
  private HashMap<String, Table> tables; // 表哈希表
  ReentrantReadWriteLock lock; // 可重入读写锁
  private Meta meta; // 元数据管理
  private Logger logger; // WAL日志

  private ArrayList<Table> droppedTables = new ArrayList<>(); // 删除的表

  public Database(String name) {
    this.name = name;
    this.tables = new HashMap<>();
    this.lock = new ReentrantReadWriteLock();
    String folder = Paths.get(Global.DATA_ROOT_FOLDER, name).toString();
    String meta_name = name + ".meta";
    this.meta = new Meta(folder, meta_name); // 暂时不加载表到内存
    String logger_name = name + ".log";
    this.logger = new Logger(folder, logger_name);
    recover();
  }

  public synchronized void persist() {
    // TODO
    ArrayList<String> keys = new ArrayList<>();
    for (String key : tables.keySet()) {
      tables.get(key).persist();
      keys.add(key);
    }
    for (Table table : droppedTables) {
      table.drop();
    }
    droppedTables.clear();
    this.meta.writeToFile(keys); // 目前 一行一个table名
    this.logger.eraseFile();
  }

  public synchronized void persistMeta() {
    // TODO
    ArrayList<String> keys = new ArrayList<>();
    for (String key : tables.keySet()) {
      tables.get(key).persist();
      keys.add(key);
    }
    this.meta.writeToFile(keys); // 目前 一行一个table名
  }

  public void create(String name, Column[] columns) {
    // TODO
  }

  /**
   * [method] 创建表 [note] 传入的数据需合法
   *
   * @param name {String} 表名称
   * @param columns {Column[]} 列定义
   * @param primaryIndex {int} 主键索引
   * @throws DuplicateTableException 重复表
   */
  public void create(String name, Column[] columns, int primaryIndex) {
    if (tables.containsKey(name)) throw new DuplicateTableException();
    Table temp = new Table(this.name, name, columns, primaryIndex);
    tables.put(name, temp);
  }

  /**
   * [method] 获取表
   *
   * @param name {String} 表名称
   * @return {String} 查询结果,没有返回null
   */
  public Table get(String name) {
    if (!tables.containsKey(name)) return null;
    return tables.get(name);
  }

  public ArrayList<Table> getTables() {
    ArrayList<Table> tables1 = new ArrayList<>();
    for (Table table : tables.values()) {
      tables1.add(table);
    }
    return tables1;
  }

  public void drop(String name) {
    // TODO
    if (!tables.containsKey(name)) throw new TableNotExistException();
    if (tables.get(name).lock.isWriteLocked()) throw new TableOccupiedException();
    droppedTables.add(tables.remove(name)); // .drop();
    meta.writeToFile(tables.keySet());
  }

  public String select(QueryTable[] queryTables) {
    // TODO
    QueryResult queryResult = new QueryResult(queryTables);
    return null;
  }

  /** [method] 恢复数据库 [note] 从持久化数据中恢复数据库 */
  public synchronized void recover() {
    // TODO
    ArrayList<String[]> table_list = this.meta.readFromFile();
    // 目前 一行一个table名
    for (String[] table_info : table_list) {
      tables.put(table_info[0], new Table(this.name, table_info[0]));
    }
    logRecover();
  }

  public void quit() {
    // TODO
  }

  /** [method] 删除数据库 */
  public void wipeData() {
    for (Table table : tables.values()) {
      table.drop();
    }
    tables.clear();
    this.meta.deleteFile();
    this.logger.deleteFile();
    Paths.get(Global.DATA_ROOT_FOLDER, name).toFile().delete();
  }

  public Logger getLogger() {
    return this.logger;
  }

  public void logRecover() {
    try {
      ArrayList<String> logs = logger.readLog();
      System.out.print(logs);
      for (String log : logs) {
        String[] infos = log.split(" ");
        String plan = infos[0]; // TODO
        if (plan.equals("INSERT")) {
          tables.get(infos[1]).insert(infos[2]);
        } else if (plan.equals("DELETE")) {
          tables.get(infos[1]).delete(infos[2]);
        }
      }
    } catch (Exception e) {
      throw new CustomIOException();
    }
  }
}
