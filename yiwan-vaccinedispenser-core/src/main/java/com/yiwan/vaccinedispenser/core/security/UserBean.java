package com.yiwan.vaccinedispenser.core.security;

import lombok.Data;
import lombok.ToString;

/**
 * 用户详情包装类
 * 
 * @author gaigeshen
 */
@Data
@ToString
public class UserBean {

    /**
     * 登录的用户的id
     */
    private Long id;

    /**
     * 登录用户的用户名
     */
    private String userName;

}
