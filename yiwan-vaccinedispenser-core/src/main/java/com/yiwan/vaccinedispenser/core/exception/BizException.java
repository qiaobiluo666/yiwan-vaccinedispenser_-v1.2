package com.yiwan.vaccinedispenser.core.exception;


import com.yiwan.vaccinedispenser.core.web.ErrorCode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * 业务类异常
 * @author slh
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BizException extends RuntimeException {

    private int code;
    private String msg;

    public BizException(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public BizException(ErrorCode errorCode) {
        this.code = errorCode.getCode();
        this.msg = errorCode.getMessage();
    }

    public BizException(String msg) {
        this.code = ErrorCode.SYSTEM_ERROR.getCode();
        this.msg = msg;
    }

}
