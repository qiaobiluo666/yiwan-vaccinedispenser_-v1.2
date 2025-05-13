package com.yiwan.vaccinedispenser.core.web;

import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.Min;

/**
 * 数据分页请求参数
 * 
 * @author gaigeshen
 */
@Data
@ToString
public  class PageRequest {

  @Min(1)
  private Integer page= 1;
  
  @Min(1)
  private Integer size=10;


}
