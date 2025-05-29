package com.yiwan.vaccinedispenser.system.sys.service.vac;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacGetVaccine;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachine;
import com.yiwan.vaccinedispenser.system.sys.data.SendBtnData;
import com.yiwan.vaccinedispenser.system.sys.data.request.IdListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.OtherRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugRecordRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.MachineListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.VacMachineRequest;
import com.yiwan.vaccinedispenser.system.sys.data.response.vac.InventoryResponse;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author slh
 **/
public interface VacMachineService extends IService<VacMachine>{

    /**
     * 仓柜列表查询
     */
    List<VacMachine> getVacMachineListByProductNo(String getProductName);

    Result vacMachineList();


    Result   vacMachineAdd(MachineListRequest request, UserBean user);


    Result   vacMachineEdit(MachineListRequest request, UserBean user);


    Result   vacMachineDel(IdListRequest request, UserBean user);


    //禁用仓位
    void vacMachineIOById(Long id,Integer status);

    //开启仓位
    void vacMachineIOByBoxNo(String boxNo);


    Result   vacMachineBatchAdd(MachineListRequest request, UserBean user);



    //多人份 查找对应仓位
    DrugRecordRequest findPeople(List<Long> boxSepcIds, Integer num, DrugRecordRequest request);

    //自动上药查找 对应的仓位
    DrugRecordRequest findBox(List<Long> boxSepcIds, Integer num, DrugRecordRequest request);



    //自动上药查找 对应的仓位
    DrugRecordRequest findBoxTest(List<Long> boxSepcIds, Integer num, DrugRecordRequest request);



    //更新药仓 新增或者药品+1
    void updateBox( DrugRecordRequest request, int status);

    //发药-1
    void decrementNumById(Long id);

    //库存管理
    Page<InventoryResponse> vacMachineInventoryList(String productName, Integer page, Integer size);



    List<InventoryResponse>  vacMachineInventoryListPdf(String productName);

    //库存明细
    Page<VacMachine> vacMachineInventoryDetail(String productNo, Integer status,Integer page, Integer size);

    //手动上药  人工上药
    Result handDrugHand(String code) throws Exception;

    //手动上药  机械手上药
    Result handDrugMachine(String code) throws Exception;




    Result handDrugPeople(String code,Integer bulkNum) throws Exception;

    //库存盘点
    Result machineInventoryCount() throws Exception;


    //根据machineId来更新仓位里面的可用数量
    void  updateByIdAndNum(Long id ,Integer num);


    //自动退药 将医生选中的疫苗退出到机器外
    Result autoBackVaccine(VacMachineRequest request) throws ExecutionException, InterruptedException;


    //测试发药处方
    VacMachine testDrop(Integer lineNum);

    //测试10层IO
    void  testIOAll(int ioTime);


    //测试led
    List<VacMachine> cabinetLedTest(Integer lineNum);



    //测试仓位几个到几个
    void testCabinet(OtherRequest request);


    //机械手对仓位、 x根据仓位自动增加
    void handAutoX(OtherRequest request);


    //获取单机版疫苗信息
    List<SendBtnData>  getSendBtnMSg();


    void machineSendDrugAlone(VacGetVaccine vacGetVaccine ,UserBean userBean) throws Exception;

}
