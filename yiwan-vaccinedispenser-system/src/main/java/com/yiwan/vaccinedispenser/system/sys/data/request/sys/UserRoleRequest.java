package com.yiwan.vaccinedispenser.system.sys.data.request.sys;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;


@Data
public class UserRoleRequest {


    @NotNull
    private Long userId;
    private List<Long> roleIds;
}
