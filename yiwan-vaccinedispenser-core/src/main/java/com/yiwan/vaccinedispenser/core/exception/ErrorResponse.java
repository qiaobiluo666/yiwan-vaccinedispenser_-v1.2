package com.yiwan.vaccinedispenser.core.exception;

import lombok.Data;

@Data
public class ErrorResponse {
    private int code;
    private String message;

    public ErrorResponse(int code, String message) {
        this.code = code;
        this.message = message;
    }

    // Getter and setter methods...
}
