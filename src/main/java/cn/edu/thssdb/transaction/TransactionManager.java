package cn.edu.thssdb.transaction;

import cn.edu.thssdb.exception.DatabaseNotExistException;
import cn.edu.thssdb.exception.DatabaseOccupiedException;
import cn.edu.thssdb.plan.*;
import cn.edu.thssdb.plan.impl.DropDatabasePlan;
import cn.edu.thssdb.plan.impl.RollBackPlan;
import cn.edu.thssdb.plan.impl.SavePointPlan;
import cn.edu.thssdb.schema.Database;
import cn.edu.thssdb.schema.Logger;
import cn.edu.thssdb.schema.Manager;
import cn.edu.thssdb.schema.Table;
import cn.edu.thssdb.utils.Global;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TransactionManager {
  private String databaseName; // 数据库名称
  private Manager manager; // 管理器对象
  private LinkedList<LogicalPlan> plans; // 操作列表
  private HashMap<String, Integer> savepoints; // 检查点散列表
  private LinkedList<ReentrantReadWriteLock.ReadLock> readLockList; // 读锁列表
  private LinkedList<ReentrantReadWriteLock.WriteLock> writeLockList; // 写锁列表
  private boolean underTransaction = false; // 是否在transaction过程
  private Logger logger; // WAL日志

  public TransactionManager(String databaseName, Logger logger) {
    this.databaseName = databaseName;
    this.manager = Manager.getInstance();
    this.plans = new LinkedList<>();
    this.savepoints = new HashMap<>();
    this.readLockList = new LinkedList<>();
    this.writeLockList = new LinkedList<>();
    this.logger = logger;
  }

  /**
   * [method] 获取当前事务状态
   *
   * @param plan
   * @return
   */
  public TransactionStatus exec(LogicalPlan plan) {
    switch (plan.getType()) {
      case SHOW_DB:
      case SHOW_TABLE:
        return showTransaction(plan);
      case BEGIN_TRANSACTION:
        return beginTransaction();
      case COMMIT_TYPE:
        return commitTransaction();
      case INSERT:
      case DELETE:
      case UPDATE:
        return writeTransaction(plan);
      case SELECT:
        return selectTransaction(plan);
      case ROLLBACK:
        return rollbackTransaction(plan);
      case SAVEPOINT:
        return savepointTransaction(plan);
      default:
        return endTransaction(plan);
    }
  }

  private TransactionStatus rollbackTransaction(LogicalPlan aplan) {
    if (databaseName == null) throw new DatabaseNotExistException();
    RollBackPlan temp = (RollBackPlan) aplan;
    String savepointName = temp.getSavepoint();
    int index = 0;
    if (savepointName != null) {
      Integer tmp = savepoints.get(savepointName);
      if (tmp == null) {
        return new TransactionStatus(false, "Savepoint不存在");
      }
      index = tmp;
    }
    try {
      for (int i = plans.size(); i > index; i--) {
        LogicalPlan plan = plans.removeLast();
        ArrayList<String> tableNames = plan.getTableName();
        if (tableNames != null)
          for (String tableName : tableNames) {
            this.releaseTransactionWriteLock(tableName);
          }
        plan.undo();
      }
      if (index == 0) underTransaction = false;
    } catch (Exception e) {
      return new TransactionStatus(false, e.getMessage());
    }
    return new TransactionStatus(true, "Success");
  }

  private TransactionStatus savepointTransaction(LogicalPlan plan) {
    if (databaseName == null) throw new DatabaseNotExistException();
    if (!underTransaction)
      return new TransactionStatus(false, "Exception: No transaction ongoing!");
    SavePointPlan temp = (SavePointPlan) plan;
    String savepointName = temp.getSavepoint();
    if (savepointName == null)
      return new TransactionStatus(false, "Exception: No savepoint given.");
    savepoints.put(savepointName, plans.size());
    return new TransactionStatus(true, "Success");
  }

  private TransactionStatus beginTransaction() {
    try {
      if (databaseName == null) throw new DatabaseNotExistException();
      if (underTransaction) return new TransactionStatus(false, "Exception: Transaction ongoing!");
      else {
        underTransaction = true;
        return new TransactionStatus(true, "");
      }
    } catch (Exception e) {
      return new TransactionStatus(false, e.getMessage());
    }
  }

  private TransactionStatus commitTransaction() {
    try {
      if (databaseName == null) throw new DatabaseNotExistException();
      // 解非即时读写锁
      this.releaseTransactionReadWriteLock();
      LinkedList<String> log = new LinkedList<>();
      // TODO:写入外存的Log
      while (!plans.isEmpty()) {
        LogicalPlan plan = plans.getFirst();
        log.addAll(plan.getLog());
        plans.removeFirst();
      }
      log.add("COMMIT");
      logger.writeLines(log);
      underTransaction = false;
      return new TransactionStatus(true, "Success");
    } catch (Exception e) {
      return new TransactionStatus(false, e.getMessage());
    }
  }

  /** @param databaseName */
  public void setDatabase(String databaseName) {
    this.databaseName = databaseName;
    Database db = manager.getDatabaseByName(databaseName);
    if (db == null) throw new DatabaseNotExistException();
    this.logger = db.getLogger();
  }

  private TransactionStatus showTransaction(LogicalPlan plan) {
    if (Global.DATABASE_ISOLATION_LEVEL == Global.ISOLATION_LEVEL.READ_COMMITTED) {
      // 获取读锁
      ArrayList<String> tableNames = plan.getTableName();
      if (tableNames != null)
        for (String tableName : tableNames) {
          this.getTransactionReadLock(tableName);
        }
      // 执行
      try {
        plan.exec();
      } catch (Exception e) {
        return new TransactionStatus(false, e.getMessage());
      }
      // 执行完后释放读锁
      if (tableNames != null)
        for (String tableName : tableNames) {
          this.releaseTransactionReadLock(tableName);
        }
      return new TransactionStatus(true, "");
    }
    if (Global.DATABASE_ISOLATION_LEVEL == Global.ISOLATION_LEVEL.SERIALIZABLE) {
      // 获取读锁
      ArrayList<String> tableNames = plan.getTableName();
      if (tableNames != null)
        for (String tableName : tableNames) {
          this.getTransactionReadLock(tableName);
        }
      // 执行完后释放读锁
      try {
        plan.exec();
      } catch (Exception e) {
        return new TransactionStatus(false, e.getMessage());
      }
      return new TransactionStatus(true, "");
    }
    return new TransactionStatus(false, "Exception: Unknown isolation level!");
  }

  private synchronized TransactionStatus writeTransaction(LogicalPlan plan) { // TODO
    if ((Global.DATABASE_ISOLATION_LEVEL == Global.ISOLATION_LEVEL.READ_COMMITTED)
        || (Global.DATABASE_ISOLATION_LEVEL == Global.ISOLATION_LEVEL.SERIALIZABLE)) {
      ArrayList<String> tableNames = plan.getTableName();
      boolean addplan = false;
      try {
        if (tableNames != null) {
          System.out.print("startget writelock");
          for (String tableName : tableNames) {
            this.getTransactionWriteLock(tableName);
          }
        }
        plan.exec();
        plans.add(plan);
        addplan = true;
      } catch (Exception e) {
        return new TransactionStatus(false, e.getMessage());
      } finally {
        if (!isUnderTransaction()) {
          System.out.print("finally release writelock");
          this.releaseTransactionReadWriteLock();
        }
      }
      return new TransactionStatus(true, "Success");
    }
    return new TransactionStatus(false, "Exception: Unknown isolation level!");
  }

  private synchronized TransactionStatus selectTransaction(LogicalPlan plan) {
    if (Global.DATABASE_ISOLATION_LEVEL == Global.ISOLATION_LEVEL.READ_COMMITTED) {
      // 获取读锁
      ArrayList<String> tableNames = plan.getTableName();
      // 执行
      try {

        //        for(String tn:tableNames){
        //          System.out.print("select "+tn);
        //        }
        if (tableNames != null) {
          System.out.print(plan.getStmt() + '\n');
          System.out.print("size" + tableNames.size());
          if (tableNames.size() > 1) {
            while (true) {
              int n = tableNames.size();
              int i = 0;
              boolean flag = false;
              for (String tableName : tableNames) {
                if (this.getTransactionReadLock(tableName) == false) {
                  this.releaseTransactionReadWriteLock();
                  break;
                }
                i++;
                if (i == n) {
                  flag = true;
                }
                System.out.print("select " + tableName);
              }
              if (flag) break;
            }
          } else {
            for (String tableName : tableNames) {
              this.getTransactionReadLock(tableName);
              System.out.print("select " + tableName);
            }
          }
        }
        plan.exec();
      } catch (Exception e) {
        return new TransactionStatus(false, e.getMessage());
      } finally {
        //        if (!isUnderTransaction()) {
        //          this.releaseTransactionReadWriteLock();
        //        }
        //        else{
        //          if (tableNames != null)
        //            for (String tableName : tableNames) {
        //              this.releaseTransactionReadLock(tableName);
        //            }
        //        }
        this.releaseTransactionReadWriteLock();
      }
      // 执行完后释放读锁
      //      if (tableNames != null)
      //        for (String tableName : tableNames) {
      //          this.releaseTransactionReadLock(tableName);
      //        }
      return new TransactionStatus(true, "", plan.getData(), plan.getColumns(), plan.getStmt());
    }
    if (Global.DATABASE_ISOLATION_LEVEL == Global.ISOLATION_LEVEL.SERIALIZABLE) {
      // 获取读锁
      ArrayList<String> tableNames = plan.getTableName();
      if (tableNames != null)
        for (String tableName : tableNames) {
          this.getTransactionReadLock(tableName);
        }
      // 执行完后释放读锁
      try {
        plan.exec();
      } catch (Exception e) {
        return new TransactionStatus(false, e.getMessage());
      }
      return new TransactionStatus(true, "", plan.getData(), plan.getColumns(), plan.getStmt());
    }
    return new TransactionStatus(false, "Exception: Unknown isolation level!");
  }

  private TransactionStatus endTransaction(LogicalPlan plan) {
    if (underTransaction) { // 如果其他事务正在执行
      commitTransaction();
    }
    try {
      // TODO:检查删除的数据库此时是否被占用
      if (plan instanceof DropDatabasePlan) {
        for (Table table :
            Manager.getInstance()
                .getDatabaseByName(((DropDatabasePlan) plan).getDatabaseName())
                .getTables()) {
          if (table.lock.isWriteLocked()) {
            System.out.print(table.tableName);
            throw new DatabaseOccupiedException();
          }
        }
      } else { // TODO
        // logger.writeLines(plan.getLog());
      }
      plan.exec();
      underTransaction = false;
    } catch (Exception e) {
      return new TransactionStatus(false, e.getMessage());
    }
    return new TransactionStatus(true, "");
  }

  public boolean isUnderTransaction() {
    return underTransaction;
  }
  // *****************************************************************
  /**
   * [method] 获取事务读锁（READ_UNCOMMITTED | READ_COMMITTED）
   *
   * @param tableName
   * @return
   */
  private synchronized boolean getTransactionReadLock(String tableName) {
    if (!Global.ISOLATION_STATUS) return true;
    // 获取锁对象
    Table table = manager.getTableByName(this.databaseName, tableName);
    if (table == null) return true;
    ReentrantReadWriteLock.ReadLock readLock = table.lock.readLock();
    if (readLockList.contains(readLock)) return true;
    if (!readLock.tryLock()) return false;
    else {
      readLockList.add(readLock);
      return true;
    }
  }

  /**
   * [method] 获取事务写锁
   *
   * @param tableName
   * @return
   */
  private synchronized boolean getTransactionWriteLock(String tableName)
      throws InterruptedException {
    if (!Global.ISOLATION_STATUS) return true;
    // 获取锁对象
    Table table = manager.getTableByName(this.databaseName, tableName);
    if (table == null) return false;
    ReentrantReadWriteLock.WriteLock writeLock = table.lock.writeLock();
    if (this.writeLockList.contains(writeLock)) return true;
    // 获取写锁
    if (!writeLock.tryLock()) {
      // 获取写锁失败 排除自身读锁的阻塞
      System.out.print("fail get writelock");
      while (true) {
        if (!this.releaseTransactionReadLock(tableName)) break;
      }
      System.out.print("readlock release?");
      ;
      // 再次获取
      writeLock.lock();
      System.out.print("fail and then get writelock");
    }
    System.out.print("success get writelock");
    writeLockList.add(writeLock);
    return true;
  }

  /**
   * [method] 释放事务读锁（READ_COMMITTED）
   *
   * @param tableName
   * @return
   */
  private synchronized boolean releaseTransactionReadLock(String tableName) {
    if (!Global.ISOLATION_STATUS) return true;
    // 获取锁对象
    Table table = manager.getTableByName(this.databaseName, tableName);
    if (table == null) return false;
    ReentrantReadWriteLock.ReadLock readLock = table.lock.readLock();
    // 释放读锁
    if (readLockList.remove(readLock)) {
      readLock.unlock();
      return true;
    }
    return false;
  }

  /**
   * [method] 释放事务写锁（NONE）
   *
   * @param tableName
   * @return
   */
  private synchronized boolean releaseTransactionWriteLock(String tableName) {
    if (!Global.ISOLATION_STATUS) return true;
    // 获取锁对象
    Table table = manager.getTableByName(this.databaseName, tableName);
    if (table == null) return false;
    ReentrantReadWriteLock.WriteLock writeLock = table.lock.writeLock();
    // 释放写锁
    //    if(writeLock.isHeldByCurrentThread()){
    //
    //    }
    writeLock.unlock();
    if (writeLockList.remove(writeLock)) {
      return true;
    }
    return false;
  }

  /** [method] 释放事务读写锁（READ_UNCOMMITTED | READ_COMMITTED | SERIALIZABLE） */
  private synchronized void releaseTransactionReadWriteLock() {
    if (!Global.ISOLATION_STATUS) return;
    // 释放写锁
    while (!writeLockList.isEmpty()) {
      writeLockList.remove().unlock();
    }
    // 释放读锁
    while (!readLockList.isEmpty()) {
      readLockList.remove().unlock();
    }
  }
}
