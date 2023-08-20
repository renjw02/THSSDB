package cn.edu.thssdb.type;

import cn.edu.thssdb.exception.IllegalTypeException;

public enum ColumnType {
  INT,
  LONG,
  FLOAT,
  DOUBLE,
  STRING;

  public static String columnType2String(ColumnType type) {
    if (type == INT) return "INT";
    else if (type == LONG) return "LONG";
    else if (type == FLOAT) return "FLOAT";
    else if (type == DOUBLE) return "DOUBLE";
    else if (type == STRING) return "STRING";
    else throw new IllegalTypeException();
  }

  public static ColumnType string2ColumnType(String s) {
    if (s.equals("INT")) return INT;
    else if (s.equals("LONG")) return LONG;
    else if (s.equals("FLOAT")) return FLOAT;
    else if (s.equals("DOUBLE")) return DOUBLE;
    else if (s.equals("STRING")) return STRING;
    else throw new IllegalTypeException();
  }

  public static Comparable getColumnTypeValue(ColumnType c, String val) {
    if (val.equalsIgnoreCase("null")) return null;
    if (c == INT) return Double.valueOf(val).intValue();
    else if (c == LONG) return Double.valueOf(val).longValue();
    else if (c == FLOAT) return Double.valueOf(val).floatValue();
    else if (c == DOUBLE) return Double.valueOf(val).doubleValue();
    else if (c == STRING) return val;
    else throw new IllegalTypeException();
  }
}
