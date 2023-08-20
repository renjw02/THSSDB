package cn.edu.thssdb.parser.token;

import java.util.ArrayList;

public class FromContentToken {
  private ArrayList<FromToken> fromContent;

  public FromContentToken(ArrayList<FromToken> fromTokens) {
    this.fromContent = fromTokens;
  }

  public ArrayList<FromToken> getFromContent() {
    return fromContent;
  }

  public void setFromContent(ArrayList<FromToken> fromContent) {
    this.fromContent = fromContent;
  }
}
