package com.yiwan.vaccinedispenser.system.zyc;

import cn.gov.zcy.open.sdk.http.ResponseResult;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yiwan.vaccinedispenser.system.dispensing.DispensingFunction;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacDrug;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacGetVaccine;
import com.yiwan.vaccinedispenser.system.sys.data.RedisDrugListData;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugRecordRequest;
import com.yiwan.vaccinedispenser.system.sys.data.zyc.CmdListData;
import com.yiwan.vaccinedispenser.system.sys.data.zyc.SendVaccineResultData;
import com.yiwan.vaccinedispenser.system.sys.data.zyc.SendVaccineResultRequest;
import com.yiwan.vaccinedispenser.system.sys.data.zyc.VaccineCodeData;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacDrugRecordService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacDrugService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacGetVaccineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/5/31 17:44
 */
@Component
@Slf4j
public class ZcyFunction {
    @Autowired
    private ZcyRequestBuilder zcyRequestBuilder;

    @Autowired
    private VacDrugService vacDrugService;

    @Autowired
    private VacGetVaccineService vacGetVaccineService;

    @Autowired
    private DispensingFunction dispensingFunction;

    @Autowired
    private VacDrugRecordService vacDrugRecordService;

    /**
     *获取疫苗信息
     * @throws Exception
     */
    public  void  getVaccine() throws Exception {
        ResponseResult result = zcyRequestBuilder.getVaccineListRequest();

        // 解析外层 JSON 字符串
        JSONObject outerJson = JSON.parseObject(JSON.toJSONString(result));
        String bodyString = outerJson.getString("body");

        // 解析内层 JSON 字符串
        JSONObject bodyJson = JSON.parseObject(bodyString);
        JSONArray resultArray = bodyJson.getJSONArray("result");

        List<VacDrug> resultList = resultArray.toJavaList(VacDrug.class);

        for(VacDrug vacDrug:resultList){
            vacDrugService.vacSaveOrUpdateDrug(vacDrug);
        }

    }

    /**
     * 根据扫码数据拿到 电子监管码
     */
    public  DrugRecordRequest  getVaccineMsgByCode(String supervisedCode) throws Exception {
        ResponseResult result = zcyRequestBuilder.getScanResultRequest(supervisedCode);
        // 解析外层 JSON 字符串
        JSONObject outerJson = JSON.parseObject(JSON.toJSONString(result));
        String bodyString = outerJson.getString("body");

        log.info("扫码code:{},收到政采云电子监管码回调：{}",supervisedCode,bodyString);
        // 解析内层 JSON 字符串
        JSONObject bodyJson = JSON.parseObject(bodyString);

        //如果电子监管码不存在
        if(bodyJson.getString("success").equals("false")){
            DrugRecordRequest drugRecordData = new DrugRecordRequest();
            drugRecordData.setIsReturn(true);
            drugRecordData.setMsg(bodyJson.getString("message"));
            return  drugRecordData;
        }else {
            //拿到电子监管码信息，根据产品编码拿到疫苗信息
            DrugRecordRequest drugRecordData = JSON.parseObject(bodyJson.getString("result"),DrugRecordRequest.class);
            drugRecordData.setIsReturn(false);
            return drugRecordData;

        }

    }

    /**
     * 获取发苗数据
     */
    public void  getVaccineSendMsg() throws Exception {
        ResponseResult result = zcyRequestBuilder.getSendVaccineRequest();
        // 解析外层 JSON 字符串
        JSONObject outerJson = JSON.parseObject(JSON.toJSONString(result));
        String bodyString = outerJson.getString("body");
//        log.info("获取发苗数据:",JSON.toJSONString(bodyString));
        // 解析内层 JSON 字符串
        JSONObject bodyJson = JSON.parseObject(bodyString);

        JSONArray resultArray = bodyJson.getJSONArray("result");
        if(resultArray!=null){
            log.info("接收到政采云发苗指令:{}",bodyString);
            for (int i = 0; i < resultArray.size(); i++) {
                JSONObject resultObj = resultArray.getJSONObject(i);
//            List<GetVaccineMsgData> resultList = resultObj.toJavaList(GetVaccineMsgData.class);
//            log.info(JSON.toJSONString(resultList));

                VacGetVaccine vacGetVaccineData = new VacGetVaccine();
                //获取请求ID
                vacGetVaccineData.setRequestNo(resultObj.getString("requestNo"));

                //获取工作台编码
                vacGetVaccineData.setWorkbenchNo(resultObj.getString("workbenchNo"));
                //获取cmdList列表消息
                JSONArray cmdListArray = resultObj.getJSONArray("cmdList");
                List<CmdListData> cmdList = JSON.parseArray(cmdListArray.toJSONString(), CmdListData.class);

                for(CmdListData cmdListData:cmdList){
                    VacGetVaccine vacGetVaccine = vacGetVaccineService.getMsgByTaskId(cmdListData.getTaskId());
                    if(vacGetVaccine!=null){
                        //如果数据库中处方数据为待发苗  则给出发药指令  将状态改为0
                        if("-1".equals(vacGetVaccine.getStatus())){
                            //进入发药处方
                            dispensingFunction.addDrugList(vacGetVaccine);
                            //修改发药状态
                            vacGetVaccineService.updateById(vacGetVaccine.getId());
                        }
                    }else {
                        vacGetVaccineData.setTaskId(cmdListData.getTaskId());
                        vacGetVaccineData.setStatus("0");
                        vacGetVaccineData.setPrice(cmdListData.getPrice());
                        vacGetVaccineData.setTag(cmdListData.getTag());
                        vacGetVaccineData.setProductNo(cmdListData.getProductNos().toString());
                        //将新处方加入数据库，直接进入发药中
                        VacGetVaccine vacGetVaccine1 = vacGetVaccineService.insertMsg(vacGetVaccineData, cmdListData.getProductNos());
                        if(vacGetVaccine1!=null){
                            log.info("发苗数据：{}",JSON.toJSONString(vacGetVaccine1));
                            //进入发药处方
                            dispensingFunction.addDrugList(vacGetVaccine1);

                        }else {

                        }
                    }

                }
            }
        }

    }



    /**
     * 发苗返回结果 成功
     */
    public ResponseResult  sendResult(RedisDrugListData redisDrugListData,String isSuccess) throws Exception {

        SendVaccineResultRequest request = new SendVaccineResultRequest();
        request.setRequestNo(redisDrugListData.getRequestNo());
        List<SendVaccineResultData> sendVaccineResultDataList = new ArrayList<>();
        SendVaccineResultData sendVaccineResultData = new SendVaccineResultData();
        sendVaccineResultData.setTaskId(redisDrugListData.getTaskId());
        sendVaccineResultData.setSendResult(isSuccess);
        sendVaccineResultDataList.add(sendVaccineResultData);
        request.setResult(sendVaccineResultDataList);
        ResponseResult responseResult = zcyRequestBuilder.sendVaccineEndRequest(request);
        log.info("发送政采云接口数据：{}，收到结果：{}",JSON.toJSONString(request),JSON.toJSONString(responseResult));

        return  responseResult;
    }






    /**
     * 发苗返回结果 失败情况
     */
    public ResponseResult  sendResult(RedisDrugListData redisDrugListData,String isSuccess,String desc) throws Exception {
        SendVaccineResultRequest request = new SendVaccineResultRequest();

        request.setRequestNo(redisDrugListData.getRequestNo());
        List<SendVaccineResultData> sendVaccineResultDataList = new ArrayList<>();
        SendVaccineResultData sendVaccineResultData = new SendVaccineResultData();
        sendVaccineResultData.setTaskId(redisDrugListData.getTaskId());
        sendVaccineResultData.setSendResult(isSuccess);
        sendVaccineResultData.setFailReason(desc);
        sendVaccineResultDataList.add(sendVaccineResultData);
        request.setResult(sendVaccineResultDataList);
        ResponseResult responseResult = zcyRequestBuilder.sendVaccineEndRequest(request);
        log.info("发送政采云接口数据：{}，收到结果：{}",JSON.toJSONString(request),JSON.toJSONString(responseResult));
        return  responseResult;
    }



    /**
     * 发苗返回结果 无库存
     */
    public ResponseResult  sendResult(VacGetVaccine vacGetVaccine,String desc) throws Exception {
        SendVaccineResultRequest request = new SendVaccineResultRequest();
        request.setRequestNo(vacGetVaccine.getRequestNo());
        List<SendVaccineResultData> sendVaccineResultDataList = new ArrayList<>();
        SendVaccineResultData sendVaccineResultData = new SendVaccineResultData();
        sendVaccineResultData.setTaskId(vacGetVaccine.getTaskId());
        sendVaccineResultData.setSendResult("0");
        sendVaccineResultData.setFailReason(desc);
        sendVaccineResultDataList.add(sendVaccineResultData);
        request.setResult(sendVaccineResultDataList);
        ResponseResult responseResult = zcyRequestBuilder.sendVaccineEndRequest(request);
        log.info("发送政采云接口数据：{}，收到结果：{}",JSON.toJSONString(request),JSON.toJSONString(responseResult));
        return  responseResult;
    }



    /**
     * 发苗机现有库存上报
     * @return
     * @throws Exception
     */
    public ResponseResult  inventoryMsg() throws Exception {
        ResponseResult responseResult = zcyRequestBuilder.getInventoryRequest(vacDrugRecordService.getInventoryReport());
        log.info("发送政采云接口数据：{}，收到结果：{}",JSON.toJSONString(vacDrugRecordService.getInventoryReport()),JSON.toJSONString(responseResult));
        return  responseResult;
    }


    /**
     * 发苗机-当日门诊损耗监管码查询
     */

    public VaccineCodeData vaccineCodeMsg(LocalDate time) throws Exception {
        ResponseResult result = zcyRequestBuilder.getVaccineCodeRequest(time);
        log.info("每日监管码：{}",JSON.toJSONString(result));
        // 解析外层 JSON 字符串
        JSONObject outerJson = JSON.parseObject(JSON.toJSONString(result));
        String bodyString = outerJson.getString("body");
        VaccineCodeData vaccineCodeData = new VaccineCodeData();
        // 解析内层 JSON 字符串
        JSONObject bodyJson = JSON.parseObject(bodyString);
        if (bodyJson.getString("success").equals("false")) {
            vaccineCodeData.setSuccess(false);
            vaccineCodeData.setMsg(bodyJson.getString("message"));
            return vaccineCodeData;
        } else {

            JSONArray resultArray = bodyJson.getJSONArray("result");
            if (resultArray != null) {
                log.info("接收到政采云发苗指令:{}", bodyString);
                for (int i = 0; i < resultArray.size(); i++) {
                    List<String> codeList = vaccineCodeData.getResult();
                    JSONObject obj = resultArray.getJSONObject(i);
                    String supervisedCode = obj.getString("supervisedCode");
                    codeList.add(supervisedCode);
                    vaccineCodeData.setResult(codeList);
                    System.out.println(supervisedCode);
                }
                vaccineCodeData.setSuccess(true);
                return vaccineCodeData;
            }else {
                vaccineCodeData.setResult(null);
                vaccineCodeData.setSuccess(true);
                return vaccineCodeData;
            }
        }

    }



}
