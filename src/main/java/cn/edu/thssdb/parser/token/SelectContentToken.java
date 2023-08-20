package cn.edu.thssdb.parser.token;

import java.util.ArrayList;

public class SelectContentToken {
  private ArrayList<SelectToken> selectContent;
  private boolean isDistinct;

  public SelectContentToken(ArrayList<SelectToken> selectContent, boolean isDistinct) {
    this.selectContent = selectContent;
    this.isDistinct = isDistinct;
  }

  public ArrayList<SelectToken> getSelectContent() {
    return selectContent;
  }

  public boolean isDistinct() {
    return isDistinct;
  }
}
