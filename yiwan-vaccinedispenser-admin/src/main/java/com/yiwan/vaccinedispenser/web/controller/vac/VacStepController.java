package com.yiwan.vaccinedispenser.web.controller.vac;

import com.yiwan.vaccinedispenser.core.security.CurrentUser;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacHandle;
import com.yiwan.vaccinedispenser.system.sys.data.request.IdListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.VacStepRequest;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacHandleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author slh
 */
@RestController
@Slf4j
@RequestMapping("/step")
public class VacStepController {

    @Autowired
    private VacHandleService vacHandleService;

    /**
     * 步进配置列表
     */
    @PostMapping("/list")
    public Result stepList(@RequestBody @Validated VacStepRequest request) {
        return Result.success(vacHandleService.getVacStepList(request));
    }

    /**
     * 编辑
     */
    @PostMapping("/edit")
    public Result stepEdit(@RequestBody VacHandle request, @CurrentUser UserBean user) {
        return vacHandleService.vacStepEdit(request, user);
    }

    /**
     * 新增
     */
    @PostMapping("/add")
    public Result stepAdd(@RequestBody VacHandle request, @CurrentUser UserBean user) {
        return vacHandleService.vacStepAdd(request, user);
    }

    /**
     * 删除
     */
    @PostMapping("/del")
    public Result stepDel(@RequestBody IdListRequest request, @CurrentUser UserBean user) {
        log.info("入参-StepRequest:{}", request);
        return vacHandleService.vacStepDel(request, user);
    }
}
