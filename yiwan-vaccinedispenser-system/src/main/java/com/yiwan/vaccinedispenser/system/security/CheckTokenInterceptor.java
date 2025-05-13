package com.yiwan.vaccinedispenser.system.security;

import com.alibaba.fastjson.JSON;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.core.web.ErrorCode;
import com.yiwan.vaccinedispenser.core.web.Result;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;


@Component
public class CheckTokenInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //关于浏览器的请求预检.在跨域的情况下，非简单请求会先发起一次空body的OPTIONS请求，称为"预检"请求，用于向服务器请求权限信息，等预检请求被成功响应后，才发起真正的http请求。
        String method = request.getMethod();
        if("OPTIONS".equalsIgnoreCase(method)){
            return true;
        }
//        String token = request.getParameter("token");放入params才能用这个，放hearder用getHearder
        String token = request.getHeader("token");
        if(token == null){

            Result<Object> fail = Result.fail("需要先进行登录处理");
            doResponse(response,fail);
        }else{
            try {
//                JwtParser parser = Jwts.parser();
//                parser.setSigningKey("ycj123456"); //解析token的SigningKey必须和生成token时设置密码一致
                //如果token检验通过（密码正确，有效期内）则正常执行，否则抛出异常
//                Jws<Claims> claimsJws = parser.parseClaimsJws(token);
                boolean validateToken = jwtTokenUtil.validateToken(token);
                UserBean user = jwtTokenUtil.getUser(token);
                request.setAttribute("currentUser", user); //把当前的用户信息放到request中

                return validateToken;//true就是验证通过，放行
            }catch (ExpiredJwtException e){
//                ResultVO resultVO = new ResultVO(ResStatus.LOGIN_FAIL_OVERDUE, "登录过期，请重新登录！", null);
//                doResponse(response,resultVO);
                Result<Object> failure = Result.failure(ErrorCode.USER_ACCOUNT_ERROR);
//                ResultVO resultVO = new ResultVO(ResStatus.LOGIN_FAIL_NOT, "请先登录！", null);
                doResponse(response,failure);
            }catch (UnsupportedJwtException e){
                Result<Object> failure = Result.failure(ErrorCode.USER_ACCOUNT_ERROR);
//                ResultVO resultVO = new ResultVO(ResStatus.LOGIN_FAIL_NOT, "请先登录！", null);
                doResponse(response,failure);
            }catch (Exception e){
                Result<Object> failure = Result.failure(ErrorCode.USER_ACCOUNT_ERROR);
//                ResultVO resultVO = new ResultVO(ResStatus.LOGIN_FAIL_NOT, "请先登录！", null);
                doResponse(response,failure);

            }

        }
        return false;
    }


    //没带token或者检验失败响应给前端
    private void doResponse(HttpServletResponse response, Result result) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");
        PrintWriter out = response.getWriter();
//        String s = new ObjectMapper().writeValueAsString(resultVO);
        String s = JSON.toJSON(result).toString();

        out.print(s);
        out.flush();
        out.close();
    }

}