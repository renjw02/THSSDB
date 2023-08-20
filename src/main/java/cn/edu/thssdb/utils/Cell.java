package cn.edu.thssdb.utils;

import cn.edu.thssdb.type.AlignType;

/** Cell类是用来描述表中的单元格的，包括单元格的值、对齐方式等信息 */
public class Cell {
  private AlignType align; // align是枚举类型，用于指示对齐方式
  private String value; // value是字符串类型，用于存储单元格的值

  public Cell(String value) {
    this.align = AlignType.LEFT;
    this.value = value;
  }

  AlignType getAlign() {
    return align;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return String.format("{%s: %s,%s: %s}", "value", value, "align", align.name());
  }
}
