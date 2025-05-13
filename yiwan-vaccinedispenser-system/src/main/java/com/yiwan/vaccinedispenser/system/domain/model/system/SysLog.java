package com.yiwan.vaccinedispenser.system.domain.model.system;


import lombok.Data;
import lombok.ToString;

/**
 *
 * @author mybatis helper
 */
@Data
@ToString
public class SysLog  {

  private Long id;
  //操作人
  private Long sysUserId;
  //用户名
  private String userName;
  //接口描述
  private String comment;
  //用户地址
  private Integer address;
  //操作描述
  private String description;
  //操作对象id
  private Long operationId;
  //详情
  private String details;
  //异常内容
  private String abnormalDetail;
  //创建时间
  private java.util.Date createTime;

}
