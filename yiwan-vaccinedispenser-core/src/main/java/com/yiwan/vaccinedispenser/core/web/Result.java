package com.yiwan.vaccinedispenser.core.web;

import lombok.Data;

import java.io.Serializable;

/**
 * 响应结果
 * 
 * @author gaigeshen
 */
@Data
public  class Result<T> implements Serializable {

  private static final long serialVersionUID = 5506805686767391936L;

  private int code;
  private String message;

  private T data;

  @Override
  public String toString() {
    return "Result{" +
            "code=" + code +
            ", message='" + message + '\'' +
            ", data=" + data +
            '}';
  }

  public static <T> Result<T> success() {
    Result<T> result = new Result<T>();
    result.setCode(ErrorCode.SUCCESS.getCode());
    return result;
  }

  public static <T> Result<T> success(T data) {
    Result<T> result = new Result<T>();
    result.setCode(ErrorCode.SUCCESS.getCode());
    result.setData(data);
    return result;
  }


  public static<T>  Result<T>  failure(int code) {
    return failure(code, null);
  }
  
  public static<T>  Result<T>  failure(int code, String message) {
    return failure(code, message, null);
  }

  public static<T>  Result<T>  failure(ErrorCode errorCode) {
    return failure(errorCode.getCode(), errorCode.getMessage(), null);
  }
  
  public static<T>  Result<T>  failure(int code, String message, T data) {
    Result<T> result = new Result<T>();
    result.setCode(code);
    result.setMessage(message);
    result.setData(data);
    return result;
  }

  public static <T> Result<T> fail(String message) {
    Result<T> result = new Result<T>();
    result.setCode(ErrorCode.SYSTEM_ERROR.getCode());
    result.setMessage(message);
    return result;
  }

  public static <T> Result<T> defineFail(int code, String message){
    Result<T> result = new Result<T>();
    result.setCode(code);
    result.setMessage(message);
    return result;
  }

  public static <T> Result<T> define(int code, String message, T data){
    Result<T> result = new Result<T>();
    result.setCode(code);
    result.setMessage(message);
    result.setData(data);
    return result;
  }
  
}
