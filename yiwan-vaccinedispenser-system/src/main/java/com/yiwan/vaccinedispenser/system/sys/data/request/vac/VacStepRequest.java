package com.yiwan.vaccinedispenser.system.sys.data.request.vac;

import lombok.Data;

import javax.validation.constraints.Min;
import java.io.Serial;
import java.io.Serializable;

@Data
public class VacStepRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = -6595098330587168849L;

    private Long id;

    private Integer lenMax;

    private Integer lenMin;

    private Integer stepExtendDis;

    private Integer stepLiftDis;

    /**
     * 当前页
     */
    @Min(1)
    private Integer page;

    /**
     * 每页大小
     */
    @Min(1)
    private Integer size;
}
