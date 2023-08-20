package cn.edu.thssdb.utils;

public class Global {
  public static final boolean ISOLATION_STATUS = true;
  public static final String SUCCESS_DISCONNECT = "[Success] Disconnect succeed.";
  public static final String FAILURE_DISCONNECT =
      "[Failure] Disconnect failed: user is not online!";
  public static final String FAILURE_CONNECT_1 = "[Failure] Connect failed: user doesn't exist!";
  public static final String FAILURE_CONNECT_2 = "[Failure] Connect failed: wrong password!";
  public static final String FAILURE_CONNECT_3 = "[Failure] Connect failed: User already online!";
  public static final String FAILURE_FORBIDDEN = "[Failure] Forbidden!";

  public enum ISOLATION_LEVEL {
    READ_COMMITTED,
    SERIALIZABLE
  }

  public static ISOLATION_LEVEL DATABASE_ISOLATION_LEVEL = ISOLATION_LEVEL.READ_COMMITTED;
  //  public static ISOLATION_LEVEL DATABASE_ISOLATION_LEVEL = ISOLATION_LEVEL.SERIALIZABLE;
  public static int fanout = 129;

  public static int SUCCESS_CODE = 0;
  public static int FAILURE_CODE = -1;

  public static String DEFAULT_SERVER_HOST = "127.0.0.1";
  public static int DEFAULT_SERVER_PORT = 6667;
  public static String DEFAULT_USER_NAME = "root";
  public static String DEFAULT_PASSWORD = "root";

  public static String CLI_PREFIX = "ThssDB2023>";
  public static final String SHOW_TIME = "show time;";
  public static final String CONNECT = "connect";
  public static final String DISCONNECT = "disconnect;";
  public static final String QUIT = "quit;";

  public static final String S_URL_INTERNAL = "jdbc:default:connection";
  public static final String ENTRY_NULL = "null";

  public static final String DATA_ROOT_FOLDER = "data";

  public static final String DATABASE_META = "DATABASE_NAME";
  public static final String TABLE_META = "TABLE_NAME";
  public static final String PRIMARY_INDEX_META = "PRIMARY_KEY_INDEX";
  public static final int ROWS_A_FILE = 1000;
}
