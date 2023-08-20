package cn.edu.thssdb.parser.token;

import cn.edu.thssdb.exception.IndexExceedLimitException;
import cn.edu.thssdb.schema.Row;

import java.util.ArrayList;

public class MultipleConditionToken {
  public String op;
  public MultipleConditionToken multiConditionItem1 = null;
  public MultipleConditionToken multiConditionItem2 = null;
  public ConditionToken conditionItem;
  public Boolean hasChild = false;

  public MultipleConditionToken(ConditionToken c) {
    this.hasChild = false;
    this.conditionItem = c;
  }

  public MultipleConditionToken(MultipleConditionToken m1, MultipleConditionToken m2, String op) {
    this.multiConditionItem1 = m1;
    this.multiConditionItem2 = m2;
    this.op = op;
    this.hasChild = true;
  }

  public Boolean hasChild() {
    return this.hasChild;
  }

  public MultipleConditionToken getChild(int i) {
    if (i == 0) {
      return this.multiConditionItem1;
    } else if (i == 1) {
      return this.multiConditionItem2;
    } else {
      throw new IndexExceedLimitException();
    }
  }

  /** 判断一行是否满足 multiConditions 条件 方法：递归地判断子结点是否满足 */
  public Boolean evaluate(Row row, ArrayList<String> ColumnName) {
    if (!hasChild) {
      return conditionItem.evaluate(row, ColumnName);
    } else {
      Boolean leftCond = multiConditionItem1.evaluate(row, ColumnName);
      Boolean rightCond = multiConditionItem2.evaluate(row, ColumnName);
      if (op.equals("&&")) {
        return (leftCond && rightCond);
      } else {
        return (leftCond || rightCond);
      }
    }
  }
}
