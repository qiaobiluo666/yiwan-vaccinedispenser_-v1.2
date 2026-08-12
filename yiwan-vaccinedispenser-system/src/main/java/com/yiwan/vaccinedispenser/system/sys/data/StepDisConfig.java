package com.yiwan.vaccinedispenser.system.sys.data;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 步进距离配置（伸出距离 + 升高距离）
 *
 * @author slh
 */
@Data
@AllArgsConstructor
public class StepDisConfig {
    /** 步进伸出距离 */
    private Integer stepExtendDis;
    /** 步进升高距离 */
    private Integer stepLiftDis;
}
