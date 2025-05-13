package com.yiwan.vaccinedispenser.core.security;


/**
 * 密码工具类
 * 
 * @author gaigeshen
 */
public final class PasswordUtils {
  
  private PasswordUtils() { }

  /**
   * 密码编码器
   */
  private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);

  /**
   * 
   * 返回预定义的编码器
   * 
   * @return 编码器
   */
  public static PasswordEncoder encoder() {
    return ENCODER;
  }

  /**
   * 编码明文
   * 
   * @param plainText
   *          明文字符串内容
   * @return 编码后的密文
   */
  public static String encode(String plainText) {
    return encoder().encode(plainText);
  }

  /**
   * 比较明文和密文
   * 
   * @param plainText
   *          明文字符串内容
   * @param encodedText
   *          密文字符串内容
   * @return 是否匹配
   */
  public static boolean matches(String plainText, String encodedText) {
    return encoder().matches(plainText, encodedText);
  }
}
