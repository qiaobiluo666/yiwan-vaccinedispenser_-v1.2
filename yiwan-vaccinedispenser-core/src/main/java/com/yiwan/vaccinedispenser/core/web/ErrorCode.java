package com.yiwan.vaccinedispenser.core.web;

import lombok.Getter;

/**
 * 异常码
 *
 * @author gaigeshen
 */
@Getter
public enum ErrorCode {

  // =============== success 成功 20000
  SUCCESS(0, "成功"),

  // =============== 请求类错误 40000

  /**
   *  登录类错误 41000
   */
  USER_ACCOUNT_ERROR(41000, "用户登录错误"),

  /**
   * 请求参数类 42000
   */
  // 请求参数类
  REQUEST_PARAM_ERROR(42000, "请求参数异常"),
  REQUEST_INTERFACE_MISSING(42001, "请求参数异常"),

  REQUEST_PARAM_ERROR_VAILD(42000, "请求参数数据校验异常"),
  // 请求类型错误
  REQUEST_TYPE_ERROR(43000, "请求类型错误"),

  // 三方系统请求
  SECOND_SYSTEM_ERROR(44000, "请求类型错误"),


  // =============== 业务逻辑类错误
  BUSINESS_ERROR(60000, "系统异常，请稍后重试!"),


  // =============== 流程类错误

  // 出现异常，可以继续
  SYSTEM_WARING(70000, "系统异常"),


  // 严重错误，无法继续
  SYSTEM_ERROR(80000, "系统异常，请稍后重试!"),




  // =============== 硬件类异常
  CONTROL_BOARD_DISCONNECT(90000,"通讯异常,控制板掉线"),
  CONTROL_BOARD_INSTRUCT_MISSING(90001,"通讯异常,指令未回复"),
  CONTROL_BOARD_INSTRUCT_ERROR(90002,"通讯异常,发送指令异常"),


  DROP_NUM_EXCEPTION(90003,"测试掉药异常"),
  DROP_NUM_NORMAL(90004,"测试掉药正常"),

  IS_CANCELL_ERROR(9005,"上柜超时");


  ;


  /**
   * -- GETTER --
   *  返回异常码
   *
   * @return 异常码
   */
  private int code;
  /**
   * -- GETTER --
   *  返回异常消息
   *
   * @return 异常消息
   */
  private String message;

  /**
   * 异常码构造
   *
   * @param code 异常吗代码
   * @param message 异常消息
   */
  ErrorCode(int code, String message) {
    this.code = code;
    this.message = message;
  }

}
