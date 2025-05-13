package com.yiwan.vaccinedispenser.web.controller.vac;


import com.yiwan.vaccinedispenser.core.security.CurrentUser;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.sys.data.request.IdListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugListRequest;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacDrugService;
import com.yiwan.vaccinedispenser.system.zyc.ZcyFunction;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.HttpHostConnectException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 药品信息相关接口
 * @author slh
 * @date 2023/5/8
 * @Description
 *
 */
@RestController
@Slf4j
@RequestMapping("/drug")
public class DrugController {

    @Autowired
    private VacDrugService vacDrugService;

    @Autowired
    private ZcyFunction zcyFunction;

    /**
     * 疫苗列表
     * */
    @PostMapping("/list")
    public Result boxSpecList(@RequestBody @Validated DrugListRequest request){
        log.info("入参-DrugListRequest:{}",request);
        return Result.success(vacDrugService.vacDrugList(request));
    }


    /**
     * 添加疫苗信息
     * */
    @PostMapping("/add")
    public Result boxSpecAdd(@RequestBody    @Validated DrugListRequest request, @CurrentUser UserBean user){
        log.info("入参-DrugListRequest:{}",request);
        return vacDrugService.vacDrugAdd(request,user);

    }


    /**
     * 编辑疫苗信息
     * */
    @PostMapping("/edit")
    public Result boxSpecEdit(@RequestBody @Validated DrugListRequest request, @CurrentUser UserBean user){
        log.info("入参-DrugListRequest:{}",request);
        return vacDrugService.vacDrugEdit(request,user);

    }


    /**
     * 删除疫苗信息
     * */
    @PostMapping("/del")
    public Result boxSpecDel(@RequestBody @Validated IdListRequest request, @CurrentUser UserBean user){
        log.info("入参-DrugListRequest:{}",request);
        return vacDrugService.vacDrugDel(request,user );

    }


    /**
     * 疫苗测距
     * */
    @GetMapping("/distance")
    public Result handDrugHand(String code) throws Exception {
        log.info("入参-code:{}",code);
        return vacDrugService.drugDistance(code);
    }

    /**
     * 疫苗获取
     * */
    @GetMapping("/updateZcyVaccine")
    public Result updateZcyVaccine() throws Exception {
        log.info("入参-code:");
        try {
                zcyFunction.getVaccine();
            return Result.success();
        } catch (HttpHostConnectException e){
            log.error("政采云通讯异常！");
            return Result.fail("政采云通讯异常！");
        }catch (Exception e) {
            // 捕获其它异常
            log.error("未知异常：",e);
            return Result.fail("未知异常！");
        }

    }


}
