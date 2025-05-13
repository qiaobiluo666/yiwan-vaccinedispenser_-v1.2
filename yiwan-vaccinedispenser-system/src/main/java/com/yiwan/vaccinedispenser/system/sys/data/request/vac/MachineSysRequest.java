package com.yiwan.vaccinedispenser.system.sys.data.request.vac;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/28 16:24
 */
@Data
public class MachineSysRequest {
    //id
    private Long id;
    //工作模式
    private Integer workMode;
    //10 A柜 11B柜 12C柜
    private Integer cabinet;
    //1 系统参数 2 步进电机参数 3伺服电机参数 4 时间参数
    private Integer command;
    //工作模式
    private Integer mode;
    //工作状态
    private Integer status;
    //ip
    private String ip;
    //端口号
    private Integer port;
    //版本号
    private String version;
    //单圈脉冲
    private Integer pulse;
    //单圈距离
    private Integer distance;
    //最大运行距离
    private Integer maxDistance;
    //速度
    private Integer speed;
    //回原速度
    private Integer returnSpeed;
    //加速度时间
    private Integer accelerationTime;
    //减速时间
    private Integer decelerationTime;
    //加加速度
    private Integer acceleration;
    //原点信号开关 0开 1 关
    private Integer zeroSwitch;
    //原点方向 0 正传 1 反转
    private Integer zero;
    //超时时间
    private Integer timeLong;

    private LocalDateTime createTime;

    private String createBy;

    private LocalDateTime updateTime;

    private String updateBy;

    //是否删除  true/已删除  false/未删除
    private int deleted;
}
