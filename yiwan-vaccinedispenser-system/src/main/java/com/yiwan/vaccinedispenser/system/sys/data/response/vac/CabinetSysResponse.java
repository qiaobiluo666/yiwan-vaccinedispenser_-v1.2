package com.yiwan.vaccinedispenser.system.sys.data.response.vac;

import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachineSys;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/28 19:09
 */
@Data
public class CabinetSysResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = -6670046244770105049L;


    //id
    private Long id;
    //工作模式
    private Integer workMode;
    //10 A柜 11B柜 12C柜
    private Integer cabinet;
    //ip
    private String ip;
    //端口号
    private Integer port;
    //版本号
    private String version;

    private List<VacMachineSys> stepList;

    private List<VacMachineSys> servoList;

    private List<VacMachineSys> timeList;

    private List<VacMachineSys>  privateList;
}


