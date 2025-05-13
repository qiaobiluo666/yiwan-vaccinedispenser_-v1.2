package com.yiwan.vaccinedispenser.core.config;

import com.yiwan.vaccinedispenser.core.web.ErrorCode;
import com.yiwan.vaccinedispenser.core.web.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;


/**
 * @Description: @Vaild注解抛出异常
 * @packe: com.joy.ins.config
 * @author: cao taibai
 * @date: 2021/1/28 16:42
 */

@ControllerAdvice
@Slf4j
public class VaildExceptionConfig {


    @ResponseBody
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> throwCustomException(MethodArgumentNotValidException methodArgumentNotValidException) {
        log.error("[ @Vaild异常捕获 ] " + methodArgumentNotValidException.getMessage());
        Result<String> result = new Result<>();
        result.setCode(ErrorCode.REQUEST_PARAM_ERROR_VAILD.getCode());
        result.setMessage(methodArgumentNotValidException.getBindingResult().getFieldError().getDefaultMessage());
        return result;
    }
}

