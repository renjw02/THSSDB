package cn.edu.thssdb.exception;

public class TableOccupiedException extends RuntimeException {
  @Override
  public String getMessage() {
    return "Exception: table is under use!";
  }
}
