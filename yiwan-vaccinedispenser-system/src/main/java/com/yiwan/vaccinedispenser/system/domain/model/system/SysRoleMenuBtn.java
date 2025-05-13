package com.yiwan.vaccinedispenser.system.domain.model.system;


import lombok.Data;
import lombok.ToString;

import java.util.Date;

@Data
@ToString
public class SysRoleMenuBtn  {

  private Long id;

  private Long roleId; 
  private Long menuId; 
  private Long menuBtnId; 
  private String createBy; 
  private Date createTime; 
}