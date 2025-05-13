package com.yiwan.vaccinedispenser.system.sys.data.request.sys;

import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.NotBlank;

@Data
@ToString
public class LogResquest {

    @NotBlank
    private String username;

    @NotBlank
    private String password;
    private String type;
    private String identifyingCode;
    private String identifyingCodeId;



}
