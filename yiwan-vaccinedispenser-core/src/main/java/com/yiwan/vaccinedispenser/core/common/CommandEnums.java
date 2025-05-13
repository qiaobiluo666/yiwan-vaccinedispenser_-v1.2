package com.yiwan.vaccinedispenser.core.common;

/**
 * websocket指令标识枚举
 * @author Administrator
 */
public enum CommandEnums {

  /**
   * 上柜页面websocket
   */
  ON_CABINET_WEB(1000,"上柜页面websocket的页面id"),
  ON_CABINET_RESULT(1001, "上柜结果"),
  STATUS_UPDATE(1002, "药柜状态更新"),
  SHORTAGE_FINISH(1003, "缺药推送大屏"),
  DOSAGE_BUTTON_FINISH(1004, "分拣按钮显示推送 0暂停发药 1取消发药"),
  MACHINE_STATUS_COMMAND(1005, "机器异常"),
  LACK_AGE_FINISH(180, "缺药药品推送"),
  ONCABINET_TOAST(180001, "上柜页面Toast弹窗提示 默认"),




  /**
   * 数据驾驶舱页面
   */
  SHOW_MSG_WEB(2000,"展示大屏页面id"),

  //data sucess 成功  fail 失败

  DEVICE_STATUS_A_CONTR(2010,"发药控制器"),

  DEVICE_STATUS_B_CONTR(2020,"上药控制器"),
  DEVICE_STATUS_B_SCAN_HIGH(2021,"上方扫码"),
  DEVICE_STATUS_B_SCAN_BELOW(2022,"下方扫码"),
  DEVICE_STATUS_B_SCAN_SIDE(2023,"侧方扫码"),
  DEVICE_STATUS_B_DISTANCE_LEFT(2024,"左边测距"),
  DEVICE_STATUS_B_DISTANCE_RIGHT(2025,"右边测距"),
  DEVICE_STATUS_B_DISTANCE_HIGH(2026,"上方测距"),
  DEVICE_STATUS_C_CONTR(2030,"运输控制器"),

  //taskID 为准
  DEVICE_STATUS_SEND_DRUG_LIST_START(2040,"发药记录未完成"),
  DEVICE_STATUS_SEND_DRUG_LIST_END(2041,"发药记录已完成"),
  DEVICE_STATUS_SEND_DRUG_LIST_ERROR(2042,"发药报警");


  private int code;
  private String description;
  
  private CommandEnums(int code, String description) {
    this.code = code;
    this.description = description;
  }
  
  /**
   * 返回指令标识
   * 
   * @return 指令标识
   */
  public int getCode() {
    return code;
  }
  
  /**
   * 返回指令描述
   * 
   * @return 指令描述
   */
  public String getDescription() {
    return description;
  }

  
  @Override
  public String toString() {
    return String.format("Command(%d, %s)", code, description);
  }
}
