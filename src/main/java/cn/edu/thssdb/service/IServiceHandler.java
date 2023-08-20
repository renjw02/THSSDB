package cn.edu.thssdb.service;

import cn.edu.thssdb.User.UserManager;
import cn.edu.thssdb.User.UserService;
import cn.edu.thssdb.exception.DuplicateUserException;
import cn.edu.thssdb.exception.UserNotExistException;
import cn.edu.thssdb.exception.WrongPasswordException;
import cn.edu.thssdb.rpc.thrift.ConnectReq;
import cn.edu.thssdb.rpc.thrift.ConnectResp;
import cn.edu.thssdb.rpc.thrift.DisconnectReq;
import cn.edu.thssdb.rpc.thrift.DisconnectResp;
import cn.edu.thssdb.rpc.thrift.ExecuteStatementReq;
import cn.edu.thssdb.rpc.thrift.ExecuteStatementResp;
import cn.edu.thssdb.rpc.thrift.GetTimeReq;
import cn.edu.thssdb.rpc.thrift.GetTimeResp;
import cn.edu.thssdb.rpc.thrift.IService;
import cn.edu.thssdb.rpc.thrift.Status;
import cn.edu.thssdb.utils.Global;
import cn.edu.thssdb.utils.StatusUtil;
import org.apache.thrift.TException;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class IServiceHandler implements IService.Iface {

  private static final AtomicInteger sessionCnt =
      new AtomicInteger(0); // sessionCnt是一个原子类，用于生成sessionID

  UserManager userManager = UserManager.getInstance();

  @Override
  public GetTimeResp getTime(GetTimeReq req) throws TException {
    GetTimeResp resp = new GetTimeResp();
    resp.setTime(new Date().toString());
    resp.setStatus(new Status(Global.SUCCESS_CODE));
    return resp;
  }

  @Override
  public ConnectResp connect(ConnectReq req) throws TException {
    // 请求解析 & 响应创建
    String username = req.getUsername();
    String password = req.getPassword();
    ConnectResp resp = new ConnectResp();
    // 操作执行
    try {
      long sessionId = userManager.login(username, password);
      // 成功
      resp.setSessionId(sessionId);
      resp.setStatus(StatusUtil.success());
    } catch (UserNotExistException e) {
      // 失败
      resp.setSessionId(-1);
      resp.setStatus(StatusUtil.fail(Global.FAILURE_CONNECT_1));
    } catch (WrongPasswordException e) {
      // 失败
      resp.setSessionId(-1);
      resp.setStatus(StatusUtil.fail(Global.FAILURE_CONNECT_2));
    } catch (DuplicateUserException e) {
      // 失败
      resp.setSessionId(-1);
      resp.setStatus(StatusUtil.fail(Global.FAILURE_CONNECT_3));
    }
    // 响应回复
    return resp;
  }
  /**
   * [method] 接口 - 关闭连接
   *
   * @param req {DisconnectReq} 请求
   * @return {DisconnectResp} 响应
   */
  @Override
  public DisconnectResp disconnect(DisconnectReq req) throws TException {
    // 请求解析 & 响应创建
    long sessionId = req.getSessionId();
    DisconnectResp resp = new DisconnectResp();
    // 操作执行
    if (userManager.logout(sessionId)) {
      // 成功
      resp.setStatus(StatusUtil.success(Global.SUCCESS_DISCONNECT));
    } else {
      // 失败
      resp.setStatus(StatusUtil.fail(Global.FAILURE_DISCONNECT));
    }
    // 响应回复
    return resp;
  }

  /**
   * @param req
   * @return
   * @throws TException
   */
  @Override
  public ExecuteStatementResp executeStatement(ExecuteStatementReq req) throws TException {
    //    if (req.getSessionId() < 0) {
    //      return new ExecuteStatementResp(
    //          StatusUtil.fail("You are not connected. Please connect first."), false);
    //    }

    ExecuteStatementResp resp = new ExecuteStatementResp();
    // TODO: implement execution logic
    UserService userService = userManager.getUserService(req.getSessionId());
    if (userService != null) {
      return userService.handle(req.statement);
    } else {
      resp.setStatus(StatusUtil.fail(Global.FAILURE_FORBIDDEN));
      //      resp.setIsAbort(true);
      resp.setHasResult(false);
    }
    return resp;
  }
}
