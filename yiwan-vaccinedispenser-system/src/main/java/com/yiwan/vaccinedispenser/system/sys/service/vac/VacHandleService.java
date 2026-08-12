package com.yiwan.vaccinedispenser.system.sys.service.vac;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacHandle;
import com.yiwan.vaccinedispenser.system.sys.data.request.IdListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.VacStepRequest;

/**
 * @author slh
 **/
public interface VacHandleService extends IService<VacHandle> {

    /** 根据药盒长度查询匹配的步进配置区间 */
    VacHandle getVacHandleByLen(Integer len);

    /** 分页列表 */
    Page<VacHandle> getVacStepList(VacStepRequest request);

    /** 编辑 */
    Result vacStepEdit(VacHandle request, UserBean user);

    /** 新增 */
    Result vacStepAdd(VacHandle request, UserBean user);

    /** 删除 */
    Result vacStepDel(IdListRequest request, UserBean user);

    /** 判断长度区间是否有冲突 */
    boolean vacStepLenInterval(Integer lenMin, Integer lenMax);
}
