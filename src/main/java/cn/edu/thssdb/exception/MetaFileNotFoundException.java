package cn.edu.thssdb.exception;

/** ************* [Exception] 元数据文件没有找到 ************* */
public class MetaFileNotFoundException extends RuntimeException {
  @Override
  public String getMessage() {
    return "Exception: Meta file for this table is not found!";
  }
}
