package com.yiwan.vaccinedispenser.system.sys.service.vac.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.dispensing.ConfigFunction;
import com.yiwan.vaccinedispenser.system.dispensing.SendDrugFunction;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacDrug;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacSendDrugRecord;
import com.yiwan.vaccinedispenser.system.plc.config.PlcConfig;
import com.yiwan.vaccinedispenser.system.plc.manager.PlcSendService;
import com.yiwan.vaccinedispenser.system.sys.dao.VacDrugMapper;
import com.yiwan.vaccinedispenser.system.sys.data.ConfigSetting;
import com.yiwan.vaccinedispenser.system.sys.data.request.IdListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugRecordRequest;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacDrugService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineExceptionService;
import com.yiwan.vaccinedispenser.system.until.VacUntil;
import com.yiwan.vaccinedispenser.system.zyc.ZcyFunction;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author
 **/
@Service
@Slf4j
public class VacDrugServiceImpl extends ServiceImpl<VacDrugMapper, VacDrug> implements VacDrugService {

    /** 手动获取疫苗尺寸等待PLC返回长宽高的最长时间。 */
    private static final long PLC_MANUAL_DISTANCE_TIMEOUT_MS = 35_000L;
    private static final String MANUAL_DISTANCE_RUNNING_MESSAGE = "开始测量，请把药盒放入上药皮带";

    @Autowired
    private  VacDrugMapper vacDrugMapper;

    @Autowired
    private ConfigFunction configFunction;

    @Autowired
    private ZcyFunction zcyFunction;

    @Autowired
    private VacMachineExceptionService vacMachineExceptionService;

    @Autowired
    private SendDrugFunction sendDrugFunction;

    @Resource(name = "redisTemplate")
    private ValueOperations<String, String> valueOperations;

    @Autowired(required = false)
    private PlcSendService plcSendService;

    @Autowired
    private PlcConfig plcConfig;

    @Override
    public Page<VacDrug> vacDrugList(DrugListRequest request) {
        IPage<VacDrug> page= new Page<>(request.getPage(),request.getSize());
        LambdaQueryWrapper<VacDrug> wrapper = new LambdaQueryWrapper<>();
        if(StringUtils.isNotBlank(request.getProductName())){
            wrapper.like(VacDrug::getProductName,request.getProductName());
        }
        if(StringUtils.isNotBlank(request.getProductNo())){
            wrapper.like(VacDrug::getProductNo,request.getProductNo());
        }
        if(StringUtils.isNotBlank(request.getVaccineMinorName())){
            wrapper.like(VacDrug::getVaccineMinorName,request.getVaccineMinorName());
        }
        wrapper.eq(VacDrug::getDeleted,0);
        IPage<VacDrug> vacDrugIPage = vacDrugMapper.selectPage(page, wrapper);

        return (Page<VacDrug>) vacDrugIPage;
    }

    @Override
    public Result vacDrugAdd(DrugListRequest request, UserBean user) {
        List<VacDrug> vacDrugs = vacDrugMapper.selectList(new LambdaQueryWrapper<VacDrug>()
                .eq(VacDrug ::getProductNo,request.getProductNo())
                .eq(VacDrug::getDeleted,0));

        if(!vacDrugs.isEmpty()){
            return Result.fail("该疫苗编号已经存在");
        }
        VacDrug vacDrug = new VacDrug();
        BeanUtils.copyProperties(request, vacDrug);
        vacDrug.setCreateBy(user.getUserName());
        vacDrug.setUpdateBy(user.getUserName());
        int result = vacDrugMapper.insert(vacDrug);
        if(result>0){
            return Result.success();
        }else {
            return Result.fail("添加疫苗异常！");
        }


    }

    @Override
    public Result vacDrugEdit(DrugListRequest request, UserBean user) {
        VacDrug vacDrug = new VacDrug();
        BeanUtils.copyProperties(request, vacDrug);
        vacDrug.setUpdateBy(user.getUserName());
        int result = vacDrugMapper.updateById(vacDrug);

        if(result>0){
            return Result.success();
        }else {
            return Result.fail("编辑疫苗异常！");
        }

    }

    @Override
    public Result vacDrugDel(IdListRequest request, UserBean user) {

        // 查询要删除的记录
        List<VacDrug> vacDrugsToDelete = vacDrugMapper.selectBatchIds(request.getIdList());
        int flag=0;
        int result;
        // 手动设置更新字段值
        for (VacDrug vacDrug : vacDrugsToDelete) {
            vacDrug.setUpdateBy(user.getUserName());
            vacDrug.setDeleted(1);
            vacDrug.setUpdateTime(LocalDateTime.now());
            result = vacDrugMapper.updateById(vacDrug);
            if(result<=0){
                flag=1;
            }
        }

        if(flag==0){
            return Result.success();
        }else {
            return Result.fail("删除疫苗异常！");
        }
    }

    @Override
    public VacDrug vacDrugGetByproductNo(String productNo) {
        if (StringUtils.isBlank(productNo)) {
            return null;
        }
        List<VacDrug> vacDrugList = vacDrugMapper.selectList(new LambdaQueryWrapper<VacDrug>()
                .eq(VacDrug::getProductNo,productNo)
                .eq(VacDrug::getDeleted,0));
        return vacDrugList.isEmpty() ? null : vacDrugList.get(0);
    }

    @Override
    public VacDrug vacDrugGetValidDimensionsByProductNo(String productNo) {
        if (StringUtils.isBlank(productNo)) {
            return null;
        }
        List<VacDrug> vacDrugList = vacDrugMapper.selectList(new LambdaQueryWrapper<VacDrug>()
                .eq(VacDrug::getProductNo, productNo)
                .eq(VacDrug::getDeleted, 0)
                .gt(VacDrug::getVaccineLong, 0)
                .gt(VacDrug::getVaccineWide, 0)
                .gt(VacDrug::getVaccineHigh, 0)
                .orderByDesc(VacDrug::getUpdateTime)
                .orderByDesc(VacDrug::getId));
        VacDrug selectedDrug = selectPreferredDrug(vacDrugList);
        if (vacDrugList.size() > 1) {
            log.warn("产品编码{}存在{}条未删除药品数据，本次选择vaccineId={}，尺寸={}x{}x{}",
                    productNo,
                    vacDrugList.size(),
                    selectedDrug == null ? null : selectedDrug.getId(),
                    selectedDrug == null ? null : selectedDrug.getVaccineLong(),
                    selectedDrug == null ? null : selectedDrug.getVaccineWide(),
                    selectedDrug == null ? null : selectedDrug.getVaccineHigh());
        }
        return selectedDrug;
    }

    /**
     * 同一产品编码出现重复数据时，只允许选择长宽高都大于0的数据；
     * 多条数据都完整时选择更新时间和ID最新的数据。
     */
    static VacDrug selectPreferredDrug(List<VacDrug> vacDrugList) {
        if (vacDrugList == null || vacDrugList.isEmpty()) {
            return null;
        }
        Comparator<VacDrug> newestFirst = Comparator
                .comparing(VacDrug::getUpdateTime, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(VacDrug::getId, Comparator.nullsLast(Comparator.reverseOrder()));
        return vacDrugList.stream()
                .filter(VacDrugServiceImpl::hasCompleteDimensions)
                .sorted(newestFirst)
                .findFirst()
                .orElse(null);
    }

    static boolean hasValidLength(VacDrug vacDrug) {
        return vacDrug != null
                && vacDrug.getVaccineLong() != null
                && vacDrug.getVaccineLong() > 0;
    }

    static boolean hasCompleteDimensions(VacDrug vacDrug) {
        return vacDrug != null
                && vacDrug.getVaccineLong() != null && vacDrug.getVaccineLong() > 0
                && vacDrug.getVaccineWide() != null && vacDrug.getVaccineWide() > 0
                && vacDrug.getVaccineHigh() != null && vacDrug.getVaccineHigh() > 0;
    }

    @Override
    public void vacSaveOrUpdateDrug(VacDrug vacDrug) {
        LambdaQueryWrapper<VacDrug> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(VacDrug::getDeleted,0)
                .eq(VacDrug::getProductNo,vacDrug.getProductNo())
                .eq(VacDrug::getProductName,vacDrug.getProductName())
                .eq(VacDrug::getVaccineTypeCode,vacDrug.getVaccineTypeCode());
        List<VacDrug> vacDrugList= vacDrugMapper.selectList(lambdaQueryWrapper);
        if(vacDrugList.isEmpty()){
            vacDrugMapper.insert(vacDrug);
        }else {
            vacDrug.setId(vacDrugList.get(0).getId());
            vacDrugMapper.updateById(vacDrug);
        }
    }

    @Override
    public DrugRecordRequest sendDrugTest(String code) {

        LambdaQueryWrapper<VacDrug> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VacDrug::getDeleted,0)
                .like(VacDrug::getVaccineCode,code.substring(0, 7));

        List<VacDrug> vacDrugList = vacDrugMapper.selectList(queryWrapper);
        DrugRecordRequest request = new DrugRecordRequest();
        if(vacDrugList.isEmpty()){
            request.setIsReturn(true);
        }else {
            VacDrug vacDrug = vacDrugList.get(0);
            BeanUtils.copyProperties(vacDrug, request);
            request.setVaccineId(vacDrug.getId());
            request.setIsReturn(false);
        }
        return  request;


    }

    @Override
    public Result drugDistance(String code) throws Exception {
        if ("true".equals(valueOperations.get(RedisKeyConstant.autoDrug.AUTO_DRUG_START))) {
            String msg = "正在自动上药，不能获取疫苗尺寸，请先停止自动上药";
            log.warn("{}，电子监管码={}", msg, code);
            return Result.fail(msg);
        }

        if ("true".equals(valueOperations.get(RedisKeyConstant.bJudge.MANUAL_RUNNING))) {
            return Result.fail("正在获取疫苗尺寸，请等待当前测量完成");
        }

        valueOperations.set(RedisKeyConstant.bJudge.MANUAL_RUNNING, "true", 2, TimeUnit.MINUTES);
        valueOperations.set(RedisKeyConstant.bJudge.MANUAL_MESSAGE,
                MANUAL_DISTANCE_RUNNING_MESSAGE, 2, TimeUnit.MINUTES);
        try {
            Result result = executeDrugDistance(code);
            String resultMessage = result.getCode() == 0
                    ? "疫苗尺寸测量完成"
                    : result.getMessage();
            valueOperations.set(RedisKeyConstant.bJudge.MANUAL_MESSAGE,
                    resultMessage, 2, TimeUnit.MINUTES);
            return result;
        } catch (Exception e) {
            String msg = "获取疫苗尺寸异常：" + e.getMessage();
            valueOperations.set(RedisKeyConstant.bJudge.MANUAL_MESSAGE,
                    msg, 2, TimeUnit.MINUTES);
            throw e;
        } finally {
            valueOperations.set(RedisKeyConstant.bJudge.MANUAL_RUNNING,
                    "false", 2, TimeUnit.MINUTES);
        }
    }

    @Override
    public Result drugDistanceStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("running", "true".equals(
                valueOperations.get(RedisKeyConstant.bJudge.MANUAL_RUNNING)));
        status.put("message", valueOperations.get(RedisKeyConstant.bJudge.MANUAL_MESSAGE));
        return Result.success(status);
    }

    private Result executeDrugDistance(String code) throws Exception {

        ConfigSetting configSetting = configFunction.getSettingConfigData();
        DrugRecordRequest drugRecordData;

        if("true".equals(configSetting.getZcyAuto())){
            drugRecordData = zcyFunction.getVaccineMsgByCode(code);
            log.info(JSON.toJSONString(drugRecordData));
            if(drugRecordData.getIsReturn()){
                String msg = "药盒测距异常：政采云电子监管码请求失败："+drugRecordData.getProductNo();
                log.error(msg);
                return Result.fail(drugRecordData.getMsg());
            }

        }else {
            drugRecordData = sendDrugTest(code);
            drugRecordData.setExpiredAt(Date.from(LocalDate.now().atStartOfDay()
                    .atZone(ZoneId.systemDefault()).toInstant()));
            drugRecordData.setBatchNo("测试编号");
            drugRecordData.setPrice(String.valueOf(321));
            drugRecordData.setTag("测试标签");
            drugRecordData.setSupervisedCode(code);
            if(drugRecordData.getIsReturn()){
                String msg = "药盒测距异常：没有测试电子监管码："+code;
                log.error(msg);
                return Result.fail(msg);
            }
        }
        VacDrug vacDrug = vacDrugGetByproductNo(drugRecordData.getProductNo());
        if (vacDrug == null) {
            String msg = "药盒测距异常：疫苗列表没有产品编码：" + drugRecordData.getProductNo();
            log.error(msg);
            return Result.fail(msg);
        }

        //根据plc是否开启选择测距方式
        if (plcSendService != null) {
            log.info("PLC模式测距");
            vacDrug = drugDistancePlc(vacDrug);
        } else {
            log.info("控制板模式测距");
            vacDrug = sendDrugFunction.drugDistance(vacDrug);
        }

        if(vacDrug==null){
            if (plcSendService != null) {
                String alarmMsg = plcDistanceAlarmMessage(
                        valueOperations.get(RedisKeyConstant.bJudge.ALARM));
                if (alarmMsg != null) {
                    return Result.fail(alarmMsg);
                }
                return Result.fail("获取疫苗尺寸超时：35秒内未收到完整的长宽高数据");
            }
            return Result.fail("未检测到药盒");
        }
        log.info("药盒测苗数据：{}",JSON.toJSONString(vacDrug));
        return Result.success(vacDrug);
    }

    /**
     * PLC测距：发送控制字2 bit4通知PLC开始测距，等待PLC完成测量后从Redis读取长宽高
     */
    private VacDrug drugDistancePlc(VacDrug vacDrug) throws Exception {
        return drugDistancePlc(vacDrug, PLC_MANUAL_DISTANCE_TIMEOUT_MS);
    }

    VacDrug drugDistancePlc(VacDrug vacDrug, long timeoutMs) throws Exception {
        //清除上次的测量数据，避免读到旧值
        valueOperations.set(RedisKeyConstant.bJudge.LENGTH, "0");
        valueOperations.set(RedisKeyConstant.bJudge.WIDTH, "0");
        valueOperations.set(RedisKeyConstant.bJudge.HEIGHT, "0");
        valueOperations.set(RedisKeyConstant.bJudge.ALARM, "0");

        //通知PLC开始测试长宽高
        plcSendService.sendBCabinetJudgeTest();

        //等待PLC测量完成，通过Redis中的长宽高数据判断
        long timeout = System.currentTimeMillis();
        while ((System.currentTimeMillis() - timeout) < timeoutMs) {
            String alarmMsg = plcDistanceAlarmMessage(
                    valueOperations.get(RedisKeyConstant.bJudge.ALARM));
            if (alarmMsg != null) {
                plcSendService.sendBCabinetJudgeUnqualified();
                log.warn("[PLC] {}，已回复不合格，写入地址31=1", alarmMsg);
                return null;
            }
            String lengthStr = valueOperations.get(RedisKeyConstant.bJudge.LENGTH);
            String widthStr = valueOperations.get(RedisKeyConstant.bJudge.WIDTH);
            if (lengthStr != null && widthStr != null
                    && Integer.parseInt(lengthStr) != 0
                    && Integer.parseInt(widthStr) != 0) {
                String heightStr = valueOperations.get(RedisKeyConstant.bJudge.HEIGHT);
                if (heightStr != null && Integer.parseInt(heightStr) != 0) {
                    vacDrug.setVaccineLong(Integer.parseInt(lengthStr));
                    vacDrug.setVaccineWide(Integer.parseInt(widthStr));
                    vacDrug.setVaccineHigh(Integer.parseInt(heightStr));
                    plcSendService.sendBCabinetJudgeQualified();
                    log.info("[PLC] 测距完成并回复合格: 长={}, 宽={}, 厚={}，写入地址30=1",
                            lengthStr, widthStr, heightStr);
                    return vacDrug;
                }
            }
            VacUntil.sleep(200);
        }

        plcSendService.sendBCabinetJudgeUnqualified();
        log.warn("[PLC] 获取疫苗尺寸超时，{}ms内未收到完整的长宽高数据，已回复不合格，写入地址31=1",
                timeoutMs);
        return null;
    }

    static String plcDistanceAlarmMessage(String alarmValue) {
        if ("1".equals(alarmValue)) {
            return "B柜检测长宽厚报警：没有检测到药盒";
        }
        if ("2".equals(alarmValue)) {
            return "B柜检测长宽厚报警：距离传感器异常";
        }
        if (StringUtils.isNotBlank(alarmValue) && !"0".equals(alarmValue)) {
            return "B柜检测长宽厚报警：未知报警值=" + alarmValue;
        }
        return null;
    }

    @Override
    public Result getZycDrugDetail(String code) throws Exception {
        ConfigSetting configSetting = configFunction.getSettingConfigData();
        //跟政采云扫码 获得 药品信息
        DrugRecordRequest drugRecordData;

        if("true".equals(configSetting.getZcyAuto())){
//        if("true".equals(isSendOpen)){
            drugRecordData = zcyFunction.getVaccineMsgByCode(code);
            log.info(JSON.toJSONString(drugRecordData));
            if(drugRecordData.getIsReturn()){
                //电子监管码请求失败
                //TODO 没有仓位可以装这个药
                String msg = "获取疫苗异常：政采云电子监管码请求失败："+drugRecordData.getProductNo() ;
                log.error(msg);
                log.error(drugRecordData.getMsg());
                return Result.fail(drugRecordData.getMsg());
            }

        }else {
            //测试使用
            drugRecordData = sendDrugTest(code);
            drugRecordData.setExpiredAt(Date.from(LocalDate.now().atStartOfDay()
                    .atZone(ZoneId.systemDefault()).toInstant()));
            drugRecordData.setBatchNo("测试编号");
            drugRecordData.setPrice(String.valueOf(321));
            drugRecordData.setTag("测试标签");
            drugRecordData.setSupervisedCode(code);
            if(drugRecordData.getIsReturn()){
                String msg = "获取疫苗异常：没有测试电子监管码："+code;
                log.error(msg);
                return Result.fail(msg);
            }
        }
        //拿到疫苗信息
        VacDrug vacDrug = vacDrugGetByproductNo(drugRecordData.getProductNo());
        return Result.success(vacDrug);
    }

    @Override
    public List<VacDrug> getVaccinePdf() {
        LambdaQueryWrapper<VacDrug> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(VacDrug::getVaccineLong);
        wrapper.eq(VacDrug::getDeleted,"0");
        return vacDrugMapper.selectList(wrapper);
    }


}








