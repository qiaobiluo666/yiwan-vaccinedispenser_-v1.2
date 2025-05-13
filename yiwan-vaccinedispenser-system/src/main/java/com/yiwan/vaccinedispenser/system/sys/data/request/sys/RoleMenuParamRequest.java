package com.yiwan.vaccinedispenser.system.sys.data.request.sys;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
public class RoleMenuParamRequest {
    @NotNull
    private Long roleId;
    private List<Long> menuIds;
}
