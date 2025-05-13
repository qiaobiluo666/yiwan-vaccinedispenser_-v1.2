package com.yiwan.vaccinedispenser.system.sys.data.response;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class LoginResponse {

    // 用户的id
    private Long id;

    // 用户的名称
    private String username;

    // 用户的token
    private String token;

}
