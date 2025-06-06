package com.yiwan.vaccinedispenser.system.security;


import com.yiwan.vaccinedispenser.core.security.UserBean;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 资料来源：https://juejin.cn/post/7176803095525982269
 * JwtToken生成的工具类 JWT token的格式：header.payload.signature header的格式（算法、token的类型）： {"alg":
 * "HS512","typ": "JWT"} payload的格式（用户名、创建时间、生成时间）： {"sub":"wang","created":1489079981393,"exp":1489684781}
 * signature的生成算法： HMACSHA512(base64UrlEncode(header) + "." +base64UrlEncode(payload),secret)
 */
@Slf4j
@Component
public class JwtTokenUtil {

    private String CLAIM_KEY_CREATED = "yiwan";


    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.tokenHead}")
    private String tokenHead;

    /**
     * 根据负责生成JWT的token
     */
    private String generateToken(Map<String, Object> claims) {
        return tokenHead+ " " + Jwts.builder()
                .setClaims(claims)
                .setExpiration(generateExpirationDate())
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    /**
     * 从token中获取JWT中的负载
     */
    private Claims getClaimsFromToken(String token) {
        String tokenBody = token.split(" ")[1];
        Claims claims = null;
        try {
            claims = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(tokenBody)
                    .getBody();
        } catch (Exception e) {
            log.info("JWT格式验证失败:{}", tokenBody);
        }
        return claims;
    }


    /**
     * 生成token的过期时间
     */
    private Date generateExpirationDate() {
        return new Date(System.currentTimeMillis() + expiration * 1000);
    }

    /**
     * 从token中获取登录用户名
     */
    public UserBean getUser(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            Long id = claims.get("id",Long.class);
            String username = claims.get("userName",String.class);

            UserBean userBean = new UserBean();
            userBean.setId(id);
            userBean.setUserName(username);
            return userBean;
        } catch (Exception e) {
            e.printStackTrace();
        }
       return null;
    }

    /**
     * 验证token是否还有效
     *
     * @param token       客户端传入的token
     * @param -userDetails 从数据库中查询出来的用户信息
     */
//    public boolean validateToken(String token, UserDetails userDetails) {
//        String username = getUserNameFromToken(token);
//        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
//    }
    public boolean validateToken(String token) {
        return isTokenExpired(token);
    }

    /**
     * 判断token是否已经失效
     */
    private boolean isTokenExpired(String token) {
        Date expiredDate = getExpiredDateFromToken(token);
        return expiredDate.after(new Date());
    }

    /**
     * 从token中获取过期时间
     */
    private Date getExpiredDateFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration();
    }

    /**
     * 根据用户信息生成token
     */
    public String generateToken(Long id, String userName) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", id);
        claims.put("userName", userName);
        return generateToken(claims);
    }

    /**
     * 当原来的token没过期时是可以刷新的
     *
     * @param oldToken 带tokenHead的token
     */
    public String refreshHeadToken(String oldToken) {
//    if (StrUtil.isEmpty(oldToken)) {
//      return null;
//    }
        if (StringUtils.isBlank(oldToken)) {
            return null;
        }
        String token = oldToken.substring(tokenHead.length());
//    if (StrUtil.isEmpty(token)) {
//      return null;
//    }
        if (StringUtils.isBlank(token)) {
            return null;
        }
        //token校验不通过
        Claims claims = getClaimsFromToken(token);
        if (Objects.isNull(claims)) {
            return null;
        }
        //如果token已经过期，不支持刷新
        if (isTokenExpired(token)) {
            return null;
        }
        //如果token在30分钟之内刚刷新过，返回原token
        if (tokenRefreshJustBefore(token, 30 * 60)) {
            return token;
        } else {
            claims.put(CLAIM_KEY_CREATED, new Date());
            return generateToken(claims);
        }
    }

    /**
     * 判断token在指定时间内是否刚刚刷新过
     *
     * @param token 原token
     * @param time  指定时间（秒）
     */
    private boolean tokenRefreshJustBefore(String token, int time) {
        Claims claims = getClaimsFromToken(token);
        Date created = claims.get(CLAIM_KEY_CREATED, Date.class);
        Date refreshDate = new Date();
        //刷新时间在创建时间的指定时间内
        if (refreshDate.after(created) && refreshDate.before(cn.hutool.core.date.DateUtil.offsetSecond(created, time))) {
            return true;
        }
        return false;
    }
}