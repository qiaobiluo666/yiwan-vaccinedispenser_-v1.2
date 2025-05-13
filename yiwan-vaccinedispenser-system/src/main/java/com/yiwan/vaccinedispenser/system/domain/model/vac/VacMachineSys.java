package com.yiwan.vaccinedispenser.system.domain.model.vac;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * @author slh
 * @version 1.0
 * @desc 仓柜配置
 * @date 2024/3/5 18:45
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class VacMachineSys extends Model<VacMachineSys> {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;




    /**
     * 工作模式
     */

    private Integer workMode;

    /**
     * 10 A柜 11B柜 12C柜
     */

    private Integer cabinet;


    /**
     * 1 系统参数 2 步进电机参数 3伺服电机参数 4 时间参数
     */

    private Integer command;


    /**
     * 工作模式
     */

    private Integer mode;



    /**
     * 工作状态
     */

    private Integer status;

    /**
     * 工作仓柜
     */

    private Integer workType;

    /**
     * ip
     */

    private String ip;


    /**
     * 端口号
     */

    private Integer port;

    /**
     * 版本号
     */

    private String version;


    /**
     * 单圈脉冲
     */

    private BigInteger pulse;


    /**
     * 单圈距离
     */

    private BigInteger distance;

    /**
     * 最大运行距离
     */

    private BigInteger maxDistance;


    /**
     * 速度
     */

    private BigInteger speed;

    /**
     * 回原速度
     */

    private BigInteger returnSpeed;


    /**
     * 加速度时间
     */

    private BigInteger accelerationTime;

    /**
     * 减速时间
     */

    private BigInteger decelerationTime;

    /**
     * 加加速度
     */

    private BigInteger acceleration;


    /**
     * 原点信号开关 0开 1 关
     */

    private Integer zeroSwitch;


    /**
     * 原点方向 0 正传 1 反转
     */

    private Integer zero;

    /**
     * 超时时间
     */

    private BigInteger timeLong;

    private LocalDateTime createTime;

    private String createBy;

    private LocalDateTime updateTime;

    private String updateBy;

    /**
     * 是否删除  true/已删除  false/未删除
     */
    private int deleted;

    @Override
    public Serializable pkVal() {
        return this.id;
    }

}
