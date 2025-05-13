package com.yiwan.vaccinedispenser.system.sys.data.response;


import com.yiwan.vaccinedispenser.system.domain.model.system.SysUser;
import lombok.Data;

@Data
public class SysUserDataResponse {
    //id
    private Long id;
    //真实姓名
    private String realName;
    //用户名
    private String userName;
    private String mailbox;
    //状态 0 解冻 1  冻结
    private Integer status;
    //创建时间
    private java.util.Date createTime;
    //手机号
    private String mobile;
    private String remark;
    //角色名
    private String roleName;

    public SysUserDataResponse(SysUser user, String roleName) {
        this.id = user.getId();
        this.realName = user.getRealName();
        this.userName = user.getUserName();
        this.mailbox = user.getMailbox();
        this.status = user.getStatus();
        this.createTime = user.getCreateTime();
        this.mobile = user.getMobile();
        this.remark = user.getRemark();
        this.roleName = roleName;
    }

    public SysUserDataResponse() {}

}