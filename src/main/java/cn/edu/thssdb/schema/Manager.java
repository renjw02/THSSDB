package cn.edu.thssdb.schema;

import cn.edu.thssdb.exception.*;
import cn.edu.thssdb.utils.Global;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** Manager类是一个管理器，用于管理数据库 */
public class Manager {
  private HashMap<String, Database> databases; // 数据库哈希表
  private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock(); // 读写锁
  private Meta meta; // 元数据管理
  private ArrayList<String> databasesList; // 数据库名称列表
  private HashMap<String, Integer> onlineDatabases; // 正在使用的数据库哈希表，记录了几个客户端正在使用

  /**
   * [method] 获取管理器实例
   *
   * @return 管理器实例
   */
  public static Manager getInstance() {
    return Manager.ManagerHolder.INSTANCE;
  }

  /** [method] 构造方法 */
  public Manager() {
    // TODO
    this.databases = new HashMap<>();
    lock = new ReentrantReadWriteLock();
    databasesList = new ArrayList<>();
    onlineDatabases = new HashMap<>();
    meta = new Meta(Global.DATA_ROOT_FOLDER, "manager.meta"); // 创建manager.meta文件
    ArrayList<String[]> db_list = this.meta.readFromFile(); // 读取manager.meta文件
    System.out.println(db_list);
    for (String[] db_info : db_list) {
      databases.put(db_info[0], new Database(db_info[0])); // 创建数据库
      databasesList.add(db_info[0]); // 将数据库名加入databasesList
    }
  }

  /**
   * [method] 判断数据库是否存在
   *
   * @param databaseName {String} 数据库名称
   * @return {boolean} 数据库是否存在
   */
  public boolean contains(String databaseName) {
    return databases.containsKey(databaseName);
  }

  /** [method] 写元数据 */
  public void writeMeta() {
    ArrayList<String> db_list = new ArrayList<>();
    for (String name : databasesList) {
      db_list.add(name);
    }
    this.meta.writeToFile(db_list);
  }

  /**
   * [method] 创建数据库
   *
   * @param databaseName {String} 数据库名称
   * @exception DuplicateDatabaseException 重复数据库
   */
  public void createDatabaseIfNotExists(String databaseName) {
    if (databases.containsKey(databaseName)) throw new DuplicateDatabaseException();
    databases.put(databaseName, new Database(databaseName));
    databasesList.add(databaseName);
    writeMeta();
  }

  /**
   * [method] 删除数据库
   *
   * @param databaseName {String} 数据库名称
   */
  public void dropDatabaseIfExists(String databaseName) {
    // TODO
    if (!databases.containsKey(databaseName)) throw new DatabaseNotExistException();
    databases.get(databaseName).wipeData();
    databases.remove(databaseName);
    databasesList.remove(databaseName);
    onlineDatabases.remove(databaseName);
    writeMeta();
  }

  /**
   * [method] 退出数据库
   *
   * @param name {String} 数据库名称
   */
  public void quitDatabase(String name) {
    if (onlineDatabases.keySet().contains(name)) {
      int count = onlineDatabases.get(name);
      if (count > 1) onlineDatabases.replace(name, count - 1);
      else {
        onlineDatabases.remove(name);
        getDatabaseByName(name).persist(); // 持久化,即存起来
      }
    }
  }

  /**
   * [method] 切换数据库
   *
   * @param databaseName {String} 数据库名称
   */
  public void useDatabase(String databaseName) {
    // TODO
    if (databasesList.contains(databaseName)) { // 如果数据库存在
      if (!onlineDatabases.keySet().contains(databaseName)) { // 如果数据库没有被打开
        // TODO:databases.get(databaseName).recover();                //恢复数据库
        databases.get(databaseName).recover();
        onlineDatabases.put(databaseName, 1); // 将正在使用的数据库的客户端数置为1
      } else { // 如果数据库已经被打开
        onlineDatabases.replace(
            databaseName, onlineDatabases.get(databaseName) + 1); // 将正在使用的数据库的客户端数加1
      }
    } else {
      throw new DatabaseNotExistException();
    }
  }

  /** [method] 通过名称获取数据库 return {Database} 数据库，没有则返回null */
  public Database getDatabaseByName(String databaseName) {
    if (!databases.containsKey(databaseName)) return null;
    return databases.get(databaseName);
  }

  /** [method] 通过名称获取表 return {Table} 表，没有则返回null */
  public Table getTableByName(String databaseName, String tableName) {
    if (!databases.containsKey(databaseName)) return null;
    return databases.get(databaseName).get(tableName);
  }

  /**
   * [method] 展示所有数据库名称 return {String} 返回一定格式的数据名称信息
   *
   * @return {String}
   */
  public String showAllDatabases() {
    StringBuffer info = new StringBuffer();
    for (String name : databasesList) {
      info.append(name + '\n');
    }
    return info.toString();
  }

  public void persist() {}

  /** [class] 内部类，用于实现单例模式 */
  private static class ManagerHolder {
    private static final Manager INSTANCE = new Manager(); // 静态变量，用于存储单例对象

    private ManagerHolder() {} // 构造函数
  }
}
