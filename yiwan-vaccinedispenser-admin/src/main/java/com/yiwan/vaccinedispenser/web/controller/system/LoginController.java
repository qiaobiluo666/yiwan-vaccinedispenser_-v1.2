package com.yiwan.vaccinedispenser.web.controller.system;


import com.yiwan.vaccinedispenser.core.security.PasswordUtils;
import com.yiwan.vaccinedispenser.core.web.ErrorCode;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.domain.model.system.SysUser;
import com.yiwan.vaccinedispenser.system.security.JwtTokenUtil;
import com.yiwan.vaccinedispenser.system.sys.data.request.sys.LogResquest;
import com.yiwan.vaccinedispenser.system.sys.data.response.LoginResponse;
import com.yiwan.vaccinedispenser.system.sys.service.sys.LoginService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

@Slf4j
@RestController
public class LoginController {

    @Autowired
    private LoginService loginService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;


    /**
     * 登录接口
     * @param logParam
     * @param response
     * @return
     */
    @SneakyThrows
    @PostMapping("/login")
    public Result login(@Validated LogResquest logParam, HttpServletResponse response) {
        log.info("收到的数据是：{}" + logParam);

        response.setCharacterEncoding("utf-8");
        response.setContentType("application/json");
        String username = logParam.getUsername();
        SysUser user = loginService.findUser(username);
        if (user == null) {
            return Result.failure(ErrorCode.USER_ACCOUNT_ERROR);
        } else {

            Long id = user.getId();
            String userName = user.getUserName();
            String password = user.getPassword();
            boolean matches = PasswordUtils.matches(logParam.getPassword(), password);
            // 密码正确
            if (matches) {
                String token = jwtTokenUtil.generateToken(id, userName);
                response.setHeader("token", token);

                LoginResponse res = new LoginResponse();
                res.setUsername(username);
                res.setId(id);
                res.setToken(token);
//                response.setStatus(0);

                return Result.success(res);
            } else {
                return Result.failure(ErrorCode.USER_ACCOUNT_ERROR);
            }
        }
    }

    @PostMapping("/loginOut")
    public Result login() {
        return Result.success();
    }
}
