package com.yiwan.vaccinedispenser.core.exception;

/**
 * 业务异常
 * 
 * @author gaigeshen
 */
public class ServiceException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private Object data;
  
  public ServiceException() {
  }

  public ServiceException(String message) {
    super(message);
  }

  public ServiceException(Throwable cause) {
    super(cause);
  }

  public ServiceException(String message, Throwable cause) {
    super(message, cause);
  }

  public ServiceException(String message,
                          Throwable cause,
                          boolean enableSuppression,
                          boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  /**
   * 获取附加的数据
   * 
   * @return 附加的数据
   */
  public Object getData() {
    return data;
  }

  /**
   * 携带附加的数据
   * 
   * @param data 附加的数据
   * @return 当前的异常
   */
  public ServiceException withData(Object data) {
    this.data = data;
    return this;
  }

}
