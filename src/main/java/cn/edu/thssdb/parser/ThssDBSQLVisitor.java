/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package cn.edu.thssdb.parser;

import cn.edu.thssdb.exception.IllegalTypeException;
import cn.edu.thssdb.exception.WrongCreateTableException;
import cn.edu.thssdb.parser.token.*;
import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.plan.impl.*;
import cn.edu.thssdb.schema.Column;
import cn.edu.thssdb.sql.SQLBaseVisitor;
import cn.edu.thssdb.sql.SQLParser;
import cn.edu.thssdb.type.ColumnType;
import cn.edu.thssdb.type.ComparerType;
import cn.edu.thssdb.utils.VisitorFunc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThssDBSQLVisitor extends SQLBaseVisitor<LogicalPlan> {

  //  public static LogicalPlan getPlan(String statement) {
  //    SQLLexer lexer = new SQLLexer(CharStreams.fromString(statement));
  ////    lexer.removeErrorListeners();
  ////    lexer.addErrorListener(ErrorListener.INSTANCE);
  //    CommonTokenStream tokenStream = new CommonTokenStream(lexer);
  //    SQLParser parser = new SQLParser(tokenStream);
  ////    parser.removeErrorListeners();
  ////    parser.addErrorListener(MyErrorListener.INSTANCE);
  //    LogicalPlan res;
  //    try {
  //      ThssDBSQLVisitor visitor = new ThssDBSQLVisitor();
  //      res = visitor.visit(parser.parse());
  //    } catch (Exception e) {
  //      throw e; // 这里抛出异常让上层处理
  //    }
  //    return res;
  //  }

  /** 创建数据库 */
  @Override
  public LogicalPlan visitCreateDbStmt(SQLParser.CreateDbStmtContext ctx) {
    return new CreateDatabasePlan(ctx.databaseName().getText());
  }

  // TODO: parser to more logical plan
  @Override
  public LogicalPlan visitShowDbStmt(SQLParser.ShowDbStmtContext ctx) {
    return new ShowDatabasePlan();
  }

  @Override
  public LogicalPlan visitUseDbStmt(SQLParser.UseDbStmtContext ctx) {
    return new UseDatabasePlan(ctx.databaseName().getText());
  }

  @Override
  public LogicalPlan visitCreateUserStmt(SQLParser.CreateUserStmtContext ctx) {
    return new CreateUserPlan(ctx.userName().getText(), ctx.password().getText());
  }

  @Override
  public LogicalPlan visitDropDbStmt(SQLParser.DropDbStmtContext ctx) {
    return new DropDatabasePlan(ctx.databaseName().getText());
  }

  @Override
  public LogicalPlan visitCreateTableStmt(SQLParser.CreateTableStmtContext ctx) {
    // TODO
    System.out.println(ctx.getText());
    int n = ctx.getChildCount();
    String primaryKey = null;
    String tableName = ctx.tableName().getText();
    ArrayList<ColumnDefToken> columnDefTokens = new ArrayList<>();

    // 获取 columnDef
    List<SQLParser.ColumnDefContext> columnDefContexts = ctx.columnDef();
    for (SQLParser.ColumnDefContext cDC : columnDefContexts) {
      // 获取type
      SQLParser.TypeNameContext tNC = cDC.typeName();
      TypeToken typeToken = null;
      ColumnType typeName = null;
      if (tNC.getChildCount() == 1) {
        try {
          // 打印出typeName
          System.out.println(cDC.getText().toUpperCase());
          typeToken =
              new TypeToken(ColumnType.string2ColumnType(cDC.getChild(1).getText().toUpperCase()));
        } catch (Exception e) {
          throw new IllegalTypeException();
        }
      } else {
        try {
          System.out.println(cDC.getText());
          System.out.println(cDC.getChild(1).getText());
          System.out.println(cDC.getChild(1).getChild(0).getText());
          System.out.println(cDC.getChild(1).getChild(1).getText());
          System.out.println(cDC.getChild(1).getChild(2).getText());
          int strLen = Integer.parseInt(cDC.getChild(1).getChild(2).getText());
          typeToken =
              new TypeToken(
                  ColumnType.string2ColumnType(cDC.getChild(1).getChild(0).getText().toUpperCase()),
                  strLen);
        } catch (Exception e) {
          throw new IllegalTypeException();
        }
      }
      // 获取column
      String columnName = cDC.columnName().getText().toLowerCase();
      ;
      boolean isPrimaryKey = false;
      boolean isNotNull = false;
      // System.out.print(cDC.getChildCount());
      if (cDC.getChildCount() > 2) {
        System.out.println(cDC.getChild(2).getText().toUpperCase());
        if (cDC.getChild(2).getText().equalsIgnoreCase("PRIMARYKEY")) {
          isPrimaryKey = true;
          primaryKey = columnName;
          System.out.println(primaryKey);
        }
        if (cDC.getChild(2).getText().equalsIgnoreCase("NOTNULL")) {
          isNotNull = true;
        }
        columnDefTokens.add(new ColumnDefToken(columnName, typeToken, isPrimaryKey, isNotNull));
      } else {
        columnDefTokens.add(new ColumnDefToken(columnName, typeToken, isPrimaryKey, isNotNull));
      }
    }
    try { // 获取tableconstraint
      SQLParser.TableConstraintContext temp = ctx.tableConstraint();
      if (temp != null) {
        if (temp.columnName().size() > 1) {
          throw new WrongCreateTableException("multi-primarykey error");
        }
        for (ColumnDefToken asd : columnDefTokens) {
          if (asd.getColumnName().equalsIgnoreCase(temp.columnName().get(0).getText())) {
            asd.setPrimaryKey(true);
          }
        }
      }
    } catch (Exception e) {
      throw new WrongCreateTableException("multi-primarykey error");
    }
    // 获取 primaryKeyIndex
    int primaryKeyIndex = -1;
    ArrayList<Column> columns = new ArrayList<>();
    for (int i = 0; i < columnDefTokens.size(); i++) {
      ColumnDefToken c = columnDefTokens.get(i);

      if (c.isPrimaryKey()) {
        primaryKeyIndex = i;
        c.setNotNull(true);
      }

      if (primaryKey != null) {
        if (primaryKey.equalsIgnoreCase(c.getColumnName())) {
          primaryKeyIndex = i;
          c.setPrimaryKey(true);
          c.setNotNull(true);
        }
      }
      System.out.println(
          c.getColumnName()
              + " "
              + c.getTypeToken().getColumnType()
              + " "
              + c.isPrimaryKey()
              + " "
              + c.isNotNull()
              + " "
              + c.getTypeToken().getStrLen());
      Column column =
          new Column(
              c.getColumnName(),
              c.getTypeToken().getColumnType(),
              c.isPrimaryKey(),
              c.isNotNull(),
              c.getTypeToken().getStrLen());
      columns.add(column);
    }
    Column[] pColumns = new Column[columns.size()];
    for (int i = 0; i < columns.size(); i++) {
      pColumns[i] = columns.get(i);
    }
    return new CreateTablePlan(
        tableName, pColumns, primaryKeyIndex, new VisitorFunc().getFullText(ctx));
  }

  @Override
  public LogicalPlan visitShowTableStmt(SQLParser.ShowTableStmtContext ctx) {
    String tableName = ctx.tableName().getText();
    return new ShowTablePlan(tableName);
  }

  @Override
  public LogicalPlan visitInsertStmt(SQLParser.InsertStmtContext ctx) {
    System.out.print(ctx.getText() + "\n");
    String tableName = ctx.tableName().getText();
    ArrayList<String> columnNames = new ArrayList<>();
    ArrayList<ArrayList<LiteralValueToken>> literalValues = new ArrayList<>();
    boolean flag = false;
    if (ctx.columnName() != null) {
      List<SQLParser.ColumnNameContext> columnNameContexts = ctx.columnName();
      for (SQLParser.ColumnNameContext asd : columnNameContexts) {
        columnNames.add(asd.getText());
      }
      flag = true;
    }
    List<SQLParser.ValueEntryContext> valueEntryContexts = ctx.valueEntry();
    for (SQLParser.ValueEntryContext ve : valueEntryContexts) {
      List<SQLParser.LiteralValueContext> literalValueContexts = ve.literalValue();
      ArrayList<LiteralValueToken> literalValueTokens = new ArrayList<>();
      literalValueTokens.clear();
      for (SQLParser.LiteralValueContext lv : literalValueContexts) {
        Object child = lv.getChild(0);
        String str = lv.getChild(0).getText();
        if (str.equalsIgnoreCase("NULL")) {
          literalValueTokens.add(new LiteralValueToken(LiteralValueToken.Type.NULL, "null"));
        } else if (str.charAt(0) == '\'') {
          literalValueTokens.add(
              new LiteralValueToken(
                  LiteralValueToken.Type.STRING, str.substring(1, str.length() - 1)));
        } else {
          literalValueTokens.add(
              new LiteralValueToken(LiteralValueToken.Type.FLOAT_OR_DOUBLE, str));
        }
      }
      literalValues.add(literalValueTokens);
    }
    if (flag) {
      return new InsertPlan(tableName, columnNames, literalValues);
    }
    return new InsertPlan(tableName, literalValues);
  }

  @Override
  public LogicalPlan visitDropTableStmt(SQLParser.DropTableStmtContext ctx) {
    String tableName;
    if (ctx.getChildCount() > 3) {
      tableName = ctx.getChild(4).getText();
    } else {
      tableName = ctx.getChild(2).getText();
    }
    return new DropTablePlan(tableName);
  }

  @Override
  public LogicalPlan visitBeginStmt(SQLParser.BeginStmtContext ctx) {
    return new BeginTansactionPlan();
  }

  @Override
  public LogicalPlan visitCommitStmt(SQLParser.CommitStmtContext ctx) {
    return new CommitPlan();
  }

  @Override
  public LogicalPlan visitSelectStmt(SQLParser.SelectStmtContext ctx) {
    //    System.out.println("进入visitor");
    System.out.println(ctx.getText());
    //    System.out.println(ctx.resultColumn());

    // select 部分
    ArrayList<SelectToken> selectTokens = new ArrayList<>();
    boolean isDistinct_ = false;
    if (ctx.K_DISTINCT() != null) isDistinct_ = true;

    for (SQLParser.ResultColumnContext RC : ctx.resultColumn()) {
      // *
      if (RC.getChild(0).getText().equals("*")) {
        ColumnFullNameToken tmp = new ColumnFullNameToken(null, "*");
        selectTokens.add(new SelectToken(tmp));
      }
      // tableName.*
      else if (RC.tableName() != null) {
        ColumnFullNameToken tmp = new ColumnFullNameToken(RC.tableName().getText(), "*");
        selectTokens.add(new SelectToken(tmp));
      }
      // ColumnFullName
      else if (RC.columnFullName() != null) {
        SQLParser.ColumnFullNameContext RCC = RC.columnFullName();
        String tableName = null;
        if (RCC.tableName() != null) {
          tableName = RCC.tableName().getText();
        }
        ColumnFullNameToken tmp = new ColumnFullNameToken(tableName, RCC.columnName().getText());
        selectTokens.add(new SelectToken(tmp));
      }
    }

    SelectContentToken select_content_token = new SelectContentToken(selectTokens, isDistinct_);

    // from 部分
    ArrayList<FromToken> fromTokens = new ArrayList<>();
    for (SQLParser.TableQueryContext TQ : ctx.tableQuery()) {
      if (TQ.getChildCount() == 1) {
        fromTokens.add(new FromToken(TQ.getChild(0).getText(), null, null));
        continue;
      } else {
        if (TQ.K_JOIN() != null) {
          String table1 = TQ.getChild(0).getText();
          String table2 = TQ.getChild(2).getText();

          SQLParser.ConditionContext conditionContext = TQ.multipleCondition().condition();
          // multipleCondition 只有一个比较， 即只有condition
          // condition :
          //    expression comparator expression;
          String comparator = conditionContext.comparator().getText();
          // 处理expression, 对应comparerToken
          // 目前仅支持 expression: comparer
          // comparer : columnFullName | literalValue ;

          ComparerToken compToken1 = myVisitComparer(conditionContext.expression(0).comparer());
          ComparerToken compToken2 = myVisitComparer(conditionContext.expression(1).comparer());

          MultipleConditionToken multipleConditionToken =
              new MultipleConditionToken(new ConditionToken(compToken1, compToken2, comparator));
          fromTokens.add(new FromToken(table1, table2, multipleConditionToken));
        } else {
          String table1 = TQ.getChild(0).getText();

          SQLParser.ConditionContext conditionContext = TQ.multipleCondition().condition();
          // multipleCondition 只有一个比较， 即只有condition
          // condition :
          //    expression comparator expression;
          String comparator = conditionContext.comparator().getText();
          // 处理expression, 对应comparerToken
          // 目前仅支持 expression: comparer
          // comparer : columnFullName | literalValue ;

          ComparerToken compToken1 = myVisitComparer(conditionContext.expression(0).comparer());
          ComparerToken compToken2 = myVisitComparer(conditionContext.expression(2).comparer());

          MultipleConditionToken multipleConditionToken =
              new MultipleConditionToken(new ConditionToken(compToken1, compToken2, comparator));
          fromTokens.add(new FromToken(table1, null, multipleConditionToken));
        }
      }
    }

    FromContentToken from_content_token = new FromContentToken(fromTokens);

    // where 部分
    MultipleConditionToken where_content_token = null;
    if (ctx.K_WHERE() != null) {
      //      System.out.println("visitor where");
      //      SQLParser.ConditionContext conditionContext = ctx.multipleCondition().condition();
      //      //      System.out.println(conditionContext.getText());
      //      // multipleCondition 只有一个比较， 即只有condition
      //      // condition :
      //      //    expression comparator expression;
      //      String comparator = conditionContext.comparator().getText();
      //      // 处理expression, 对应comparerToken
      //      // 目前仅支持 expression: comparer
      //      // comparer : columnFullName | literalValue ;
      //
      //      ComparerToken compToken1 = myVisitComparer(conditionContext.expression(0).comparer());
      //      ComparerToken compToken2 = myVisitComparer(conditionContext.expression(1).comparer());

      //      System.out.println("创建where token");
      where_content_token = myVisitMultipleCondition(ctx.multipleCondition());
    }

    return new SelectPlan(
        select_content_token, from_content_token, where_content_token, ctx.getText());
  }

  public MultipleConditionToken myVisitMultipleCondition(SQLParser.MultipleConditionContext ctx) {
    if (ctx.getChildCount() == 1) {
      SQLParser.ConditionContext conditionContext = ctx.condition();
      //    expression comparator expression;
      String comparator = conditionContext.comparator().getText();
      // 处理expression, 对应comparerToken
      // 目前仅支持 expression: comparer
      // comparer : columnFullName | literalValue ;
      ComparerToken compToken1 = myVisitComparer(conditionContext.expression(0).comparer());
      ComparerToken compToken2 = myVisitComparer(conditionContext.expression(1).comparer());

      return new MultipleConditionToken(new ConditionToken(compToken1, compToken2, comparator));
    }

    MultipleConditionToken m1 = myVisitMultipleCondition(ctx.multipleCondition(0));
    MultipleConditionToken m2 = myVisitMultipleCondition(ctx.multipleCondition(1));
    return new MultipleConditionToken(m1, m2, ctx.getChild(1).getText());
  }

  public ComparerToken myVisitComparer(SQLParser.ComparerContext ctx) {
    //    System.out.println(ctx.getText());
    if (ctx.columnFullName() != null) {
      //      System.out.println("type: COLUMN");
      String tableName = null;
      if (ctx.columnFullName().tableName() != null) {
        tableName = ctx.columnFullName().tableName().getText();
      }
      String columnName = ctx.columnFullName().columnName().getText();

      return new ComparerToken(ComparerType.COLUMN, tableName, columnName);
    } else if (ctx.literalValue() != null) {
      String literalValue = "null";
      if (ctx.literalValue().NUMERIC_LITERAL() != null) {
        literalValue = ctx.literalValue().NUMERIC_LITERAL().getText();
        return new ComparerToken(ComparerType.NUMBER, literalValue);
      } else if (ctx.literalValue().STRING_LITERAL() != null) {
        literalValue = ctx.literalValue().STRING_LITERAL().getText();
        return new ComparerToken(ComparerType.STRING, literalValue);
      }
      return new ComparerToken(ComparerType.NULL, literalValue);
    }
    // 如果该项为null的话，return null
    return null;
  }

  @Override
  public LogicalPlan visitDeleteStmt(SQLParser.DeleteStmtContext ctx) {
    String tableName = ctx.tableName().getText();
    if (ctx.children.size() > 3) {
      MultipleConditionToken where_content_token = null;
      SQLParser.ConditionContext conditionContext = ctx.multipleCondition().condition();
      String comparator = conditionContext.comparator().getText();
      ComparerToken compToken1 = myVisitComparer(conditionContext.expression(0).comparer());
      ComparerToken compToken2 = myVisitComparer(conditionContext.expression(1).comparer());
      where_content_token =
          new MultipleConditionToken(new ConditionToken(compToken1, compToken2, comparator));
      return new DeletePlan(tableName, where_content_token);
    } else {
      return new DeletePlan(tableName);
    }
  }

  @Override
  public LogicalPlan visitUpdateStmt(SQLParser.UpdateStmtContext ctx) { // TODO
    String tableName = ctx.tableName().getText();
    String columnName = ctx.columnName().getText();
    ComparerToken ct = myVisitExpression(ctx.expression());
    Map<ComparerType, LiteralValueToken.Type> asd = new HashMap<>();
    asd.put(ComparerType.NUMBER, LiteralValueToken.Type.FLOAT_OR_DOUBLE);
    asd.put(ComparerType.STRING, LiteralValueToken.Type.STRING);
    asd.put(ComparerType.NULL, LiteralValueToken.Type.NULL);
    if (ctx.children.size() > 6) {
      MultipleConditionToken where_content_token = null;
      SQLParser.ConditionContext conditionContext = ctx.multipleCondition().condition();
      String comparator = conditionContext.comparator().getText();
      ComparerToken compToken1 = myVisitComparer(conditionContext.expression(0).comparer());
      ComparerToken compToken2 = myVisitComparer(conditionContext.expression(1).comparer());
      where_content_token =
          new MultipleConditionToken(new ConditionToken(compToken1, compToken2, comparator));
      return new UpdatePlan(
          tableName,
          columnName,
          new LiteralValueToken(asd.get(ct.type), ct.literalValue),
          where_content_token);
    }
    return new UpdatePlan(
        tableName, columnName, new LiteralValueToken(asd.get(ct.type), ct.literalValue));
  }

  public ComparerToken myVisitExpression(SQLParser.ExpressionContext ctx) {
    if (ctx.comparer() != null) {
      return myVisitComparer(ctx.comparer());
    } else if (ctx.expression().size() > 1) {
      if (ctx.MUL() != null) {
        ComparerToken left = myVisitExpression(ctx.expression(0));
        ComparerToken right = myVisitExpression(ctx.expression(1));
        return new ComparerToken(left, right, "*");
      } else if (ctx.DIV() != null) {
        ComparerToken left = myVisitExpression(ctx.expression(0));
        ComparerToken right = myVisitExpression(ctx.expression(1));
        return new ComparerToken(left, right, "/");
      } else if (ctx.ADD() != null) {
        ComparerToken left = myVisitExpression(ctx.expression(0));
        ComparerToken right = myVisitExpression(ctx.expression(1));
        return new ComparerToken(left, right, "+");
      } else if (ctx.SUB() != null) {
        ComparerToken left = myVisitExpression(ctx.expression(0));
        ComparerToken right = myVisitExpression(ctx.expression(1));
        return new ComparerToken(left, right, "-");
      }
    } else {
      return myVisitExpression(ctx.expression(0));
    }
    return null;
  }

  @Override
  public LogicalPlan visitRollBackStmt(SQLParser.RollBackStmtContext ctx) {
    if (ctx.getChildCount() > 1) return new RollBackPlan(ctx.children.get(3).getText());
    return new RollBackPlan();
  }

  @Override
  public LogicalPlan visitSavePointStmt(SQLParser.SavePointStmtContext ctx) {
    return new SavePointPlan(ctx.children.get(1).getText());
  }
}
