package com.yiwan.vaccinedispenser.web.controller.vac;

import com.yiwan.vaccinedispenser.core.security.CurrentUser;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.core.web.ErrorCode;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacGetVaccine;
import com.yiwan.vaccinedispenser.system.sys.data.request.IdListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.MachineListRequest;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author slh
 * @version 1.0
 * @desc 仓柜配置
 * @date 2024/3/7 10:05
 */

@RestController
@Slf4j
@RequestMapping("/machine")
public class MachineController {

    @Autowired
    private VacMachineService vacMachineService;


    /**
     * 疫苗列表
     * */
    @PostMapping("/list")
    public Result machineList(){
        return vacMachineService.vacMachineList();
    }


    /**
     * 添加疫苗信息
     * */
    @PostMapping("/add")
    public Result machineAdd(@RequestBody    @Validated MachineListRequest request, @CurrentUser UserBean user){
        log.info("入参-MachineListRequest:{}",request);
        return vacMachineService.vacMachineAdd(request,user);

    }


    /**
     * 编辑疫苗信息
     * */
    @PostMapping("/edit")
    public Result machineEdit(@RequestBody @Validated MachineListRequest request, @CurrentUser UserBean user){
        log.info("入参-MachineListRequest:{}",request);
        return vacMachineService.vacMachineEdit(request,user);
    }

    //TODO 中间删除仓柜 数据要偏移
    /**
     * 删除疫苗信息
     * */
    @PostMapping("/del")
    public Result machineDel(@RequestBody @Validated IdListRequest request, @CurrentUser UserBean user){
        log.info("入参-DrugListRequest:{}",request);
        return  vacMachineService.vacMachineDel(request,user );

    }

    /**
     * 添加疫苗信息
     * */
    @PostMapping("/batch-add")
    public Result machineBatchAdd(@RequestBody    @Validated MachineListRequest request, @CurrentUser UserBean user){
        log.info("入参-MachineListRequest:{}",request);
        return vacMachineService.vacMachineBatchAdd(request,user);
    }

    /**
     * 库存统计
     */
    @GetMapping("/inventory")
    public Result machineInventory(@RequestParam String productName,@RequestParam Integer page,@RequestParam Integer size){
        log.info("入参-productName:{},page:{},size:{}",productName,page,size);
        return Result.success(vacMachineService.vacMachineInventoryList(productName,page,size));
    }


    /**
     * 库存明细 具体仓位
     */
    @GetMapping("/inventory/detail")
    public Result machineInventoryDetail(@RequestParam String productNo, @RequestParam Integer status, @RequestParam Integer page,@RequestParam Integer size){
        log.info("入参-ProductNo:{},page:{},size:{}",productNo,page,size);
        return Result.success(vacMachineService.vacMachineInventoryDetail(productNo,status,page,size));
    }


    /**
     * 库存盘点
     */
    @GetMapping("/inventory/count")
    public Result machineInventoryCount() throws Exception {
        vacMachineService.machineInventoryCount();
        return Result.success();
    }


    /**
     * 单机发苗列表
     */
    @GetMapping("/sendBtn")
    public Result machineSendBtn() throws Exception {
        return Result.success(vacMachineService.getSendBtnMSg());
    }


    /**
     * 单机发苗按钮
     */
    @PostMapping("/sendDrugAlone")
    public Result machineSendDrugAlone(@RequestBody VacGetVaccine request, @CurrentUser UserBean user ) throws Exception {
        vacMachineService.machineSendDrugAlone(request,user);
        return Result.success();
    }

}
