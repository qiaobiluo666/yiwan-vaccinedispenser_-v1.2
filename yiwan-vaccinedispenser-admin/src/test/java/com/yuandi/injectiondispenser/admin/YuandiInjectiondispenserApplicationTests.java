package com.yuandi.injectiondispenser.admin;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yiwan.vaccinedispenser.YiwanVaccinedispenserApplication;
import com.yiwan.vaccinedispenser.core.common.SettingConstants;
import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.core.websocket.WebsocketService;
import com.yiwan.vaccinedispenser.system.camera.CameraSendMsg;
import com.yiwan.vaccinedispenser.system.com.ComPortConfig;
import com.yiwan.vaccinedispenser.system.com.ComService;
import com.yiwan.vaccinedispenser.system.dispensing.ConfigFunction;
import com.yiwan.vaccinedispenser.system.dispensing.DispensingFunction;
import com.yiwan.vaccinedispenser.system.dispensing.SendDrugFunction;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacDrug;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacGetVaccine;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachine;
import com.yiwan.vaccinedispenser.system.plc.manager.PlcSendService;
import com.yiwan.vaccinedispenser.system.plc.manager.PlcService;
import com.yiwan.vaccinedispenser.system.sys.dao.VacGetVaccineMapper;
import com.yiwan.vaccinedispenser.system.sys.dao.VacMachineMapper;
import com.yiwan.vaccinedispenser.system.sys.data.AutoData;
import com.yiwan.vaccinedispenser.system.sys.data.ConfigSetting;
import com.yiwan.vaccinedispenser.system.sys.data.DistanceServoData;
import com.yiwan.vaccinedispenser.system.sys.data.RedisDrugListData;
import com.yiwan.vaccinedispenser.system.sys.data.request.netty.CabinetAHandRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.netty.DropRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugRecordRequest;
import com.yiwan.vaccinedispenser.system.sys.data.zyc.SendVaccineResultData;
import com.yiwan.vaccinedispenser.system.sys.data.zyc.SendVaccineResultRequest;
import com.yiwan.vaccinedispenser.system.sys.service.netty.CabinetAService;
import com.yiwan.vaccinedispenser.system.sys.service.netty.CabinetBService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.*;
import com.yiwan.vaccinedispenser.system.test.UploadController;
import com.yiwan.vaccinedispenser.system.until.NettyUtils;
import com.yiwan.vaccinedispenser.system.zyc.ZcyFunction;
import com.yiwan.vaccinedispenser.system.zyc.ZcyRequestBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

@SpringBootTest(classes = YiwanVaccinedispenserApplication.class)
@RunWith(SpringRunner.class)
@Slf4j
class YuandiInjectiondispenserApplicationTests {
	@Autowired
	private VacMachineMapper vacMachineMapper;

	@Autowired
	private CabinetAService cabinetAService;

	@Autowired
	private CabinetBService cabinetBService;

	@Autowired
	private DispensingFunction dispensingFunction;


	@Autowired
	private VacBoxSpecService vacBoxSpecService;


	@Autowired
	private ZcyRequestBuilder zcyRequestBuilder;
	//IO单独控制

	@Autowired
	private VacDrugRecordService vacDrugRecordService;

	@Autowired
	private ZcyFunction zcyFunction;
	@Autowired
	private SendDrugFunction sendDrugFunction;

	@Autowired
	private VacMachineService vacMachineService;

	@Autowired
	private VacGetVaccineMapper vacGetVaccineMapper;

	@Autowired
	private WebsocketService websocketService;

	@Autowired
	private CameraSendMsg cameraSendMsg;


	@Autowired
	private ComPortConfig comPortConfig;
	@Autowired
	private ComService comService;

	@Autowired
	private ConfigFunction configFunction;

	@Resource(name = "redisTemplate")
	private ValueOperations<String, String> valueOperations;

	@Autowired
	private VacDrugService  vacDrugService;

	@Autowired
	private VacSendDrugRecordService vacSendDrugRecordService;

	@Autowired
	private VacMachineDrugService vacMachineDrugService;

	@Autowired
	private UploadController uploadController;

	@Autowired
	private PlcService plcService;

	@Autowired
	private PlcSendService plcSendService;

	@Test
	void contextLoads() {
		List<Integer> IoList = new ArrayList<>();
		IoList.add(1);
		IoList.add(2);
		DropRequest dropRequest = new DropRequest();
		dropRequest.setMode(CabinetConstants.IOMode.OUTPUT);
		dropRequest.setCommand(1);
		dropRequest.setIoList(IoList);
		cabinetAService.dropCommand(dropRequest);

	}

	@Test
	void test01() {
//		dispensingFunction.addDrugList("疫苗测试2","1",8);
//		dispensingFunction.addDrugList("疫苗测试2","2");
//		dispensingFunction.addDrugList("疫苗测试2","3");
//		dispensingFunction.addDrugList("疫苗测试2","4");
//		dispensingFunction.addDrugList("疫苗测试2","5");
//		dispensingFunction.addDrugList("疫苗测试2","6");
//		dispensingFunction.addDrugList("疫苗测试2","7");
	}

	@Test
	void test02(){
		String[] arr = {"E8", "03"};  // 低位在前、高位在后的16进制字符串

		log.info(String.valueOf(NettyUtils.parseHexStringArray(arr,0,2)));
	}


	@Test
	void test03(){

		Result x = vacDrugRecordService.drugRecordCount(2);
		log.info( JSON.toJSONString(x));

	}

	@Test
	void test04(){
		DistanceServoData distanceServoData = new DistanceServoData();
		DrugRecordRequest drugRecordData = new DrugRecordRequest();
		distanceServoData.setVaccineWide(45);
		distanceServoData.setVaccineLong(150);
		drugRecordData.setProductNo("123");
		drugRecordData.setExpiredAt(new Date());
		DrugRecordRequest drugRecordRequest = sendDrugFunction.findBox(distanceServoData,drugRecordData);
		log.info(JSON.toJSONString(drugRecordRequest));
	}

	@Test
	void test05() throws Exception {
		zcyRequestBuilder.getVaccineListRequest();
	}

	@Test
	void  test06() throws Exception{
		zcyFunction.getVaccine();
	}

	@Test
	void  test07() throws Exception{
		DrugRecordRequest drugRecordRequest = zcyFunction.getVaccineMsgByCode("81894740913153064523");
		log.info(JSON.toJSONString(drugRecordRequest));
	}


	@Test
	void test08() throws Exception {
//		zcyRequestBuilder.getSendVaccineRequest();
		zcyFunction.getVaccineSendMsg();


//		List<String> x = new ArrayList<>();
//		x.add("82352000201");
//		List<VacGetVaccine> a = vacGetVaccineMapper.findProductNo(x,"69");
//		log.info(JSON.toJSONString(a));
	}

	@Test
	void test09() throws Exception {

		SendVaccineResultRequest sendVaccineResultRequest = new SendVaccineResultRequest();
		sendVaccineResultRequest.setRequestNo("f1329196a6334eefb68b34390b909d6c");
		List<SendVaccineResultData> sendVaccineResultDataList = new ArrayList<>();
		SendVaccineResultData sendVaccineResultData = new SendVaccineResultData();
		sendVaccineResultData.setTaskId("08bd88c36b6b45cbb6eedb4e7f52e23c");
		sendVaccineResultData.setSendResult("1");
		sendVaccineResultDataList.add(sendVaccineResultData);
		sendVaccineResultRequest.setResult(sendVaccineResultDataList);
		zcyRequestBuilder.sendVaccineEndRequest(sendVaccineResultRequest);

	}

	@Test
	void test10() throws Exception{



		DrugRecordRequest drugRecordData = vacDrugService.sendDrugTest("8178380");
		drugRecordData.setExpiredAt(new Date());
		drugRecordData.setBatchNo("测试编号");
		drugRecordData.setPrice(String.valueOf(321));
		drugRecordData.setTag("测试标签");
		drugRecordData.setSupervisedCode("8178380");
		vacDrugRecordService.addDrugRecord(drugRecordData);
//		Result x = vacSendDrugRecordService.weekSendDrug();
//
//		log.info(JSON.toJSONString(x.getData()));
	}

	@Test
	void test11() throws Exception{
//		vacMachineService.machineInventoryCount();
//		List<InventoryReportData> data = vacDrugRecordService.getInventoryReport();
//		log.info(JSON.toJSONString(data));
		ConfigSetting configSetting1 = configFunction.getSettingConfigData();
		log.info(configSetting1.getZcyAuto());
	}

	@Test
	void test12() throws Exception{
////		dispensingFunction.moveBlock(CabinetConstants.CabinetCSendDrugBlockStatus.OPEN);
//		dispensingFunction.moveBlock(CabinetConstants.CabinetCSendDrugBlockStatus.QUERY);
////		dispensingFunction.moveBlock(CabinetConstants.CabinetCSendDrugBlockStatus.CLOSE);
//		VacUntil.sleep(5000);

		ConfigSetting configSetting1 = configFunction.getSettingConfigData();
		if("true".equals(configSetting1.getBFindX())){
			log.info("111");
		}else {
			log.info("222");
		}

	}


	@Test
	void test13() {

	}

	@Test
	void test5() throws Exception {
		VacGetVaccine vacGetVaccine = new VacGetVaccine();
		vacGetVaccine.setProductNo("01202000303");
		vacGetVaccine.setProductName("乙脑");

		vacGetVaccine.setTaskId(String.valueOf(UUID.randomUUID()));
		vacGetVaccine.setRequestNo("requestNo");
		vacGetVaccine.setWorkbenchName("接种台6");
		vacGetVaccine.setWorkbenchNum(1);
		vacGetVaccine.setWorkbenchNo("69");
		dispensingFunction.addDrugList(vacGetVaccine);

	}
	@Test
	void test6() throws Exception {
		//查找药仓
		String dateStr = "2025-09-01 00:00:00";
		SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date fullDate = dateTimeFormat.parse(dateStr); // 转换为 Date
		long i = 15;
		List<Long> boxSpecIds = new ArrayList<>();
		boxSpecIds.add(i);
		DrugRecordRequest request = new DrugRecordRequest();
		request.setProductNo("79524002604");
		request.setProductName("进-轮毒五价-L-默沙东-1/2ml/支-液其他口1");
		request.setExpiredAt(fullDate);
		request.setSupervisedCode("测试电子监管码");
		int num = 7;
		DrugRecordRequest request1 = vacMachineService.findPeople(boxSpecIds,num ,request);
		log.info(JSON.toJSONString(request1));

		//机械手上有药，仓位药品数量+1，新增上药记录
		sendDrugFunction.addDrugRecord(request1,2);


	}

	@Test
	void test7() throws Exception {
		RedisDrugListData drugListData = new RedisDrugListData();
		drugListData.setMachineId(685L);
		drugListData.setWorkbenchNum(1);
		dispensingFunction.dropRecordAndMachine(drugListData,1,"发药正常");
	}

	@Test
	void test8() throws Exception{

		vacMachineDrugService.delMachineByCreatTime(1);

	}

	@Test
		void test9() throws Exception{

		String x = "{\"batchNo\":\"Y0A361M\",\"beltNum\":3,\"boxNo\":\"A0513\",\"expiredAt\":1761840000000,\"lineNum\":5,\"machineId\":1032,\"machineStatus\":1,\"positionNum\":15,\"productName\":\"进-五联疫苗-巴斯德股份-1/复0.5ml/瓶-液冻其他注1\",\"productNo\":\"78977010201\",\"requestNo\":\"0ee9b334cb2d4ee694b18351988cae30\",\"taskId\":\"bf73970fac7e4e5b9667f98b96502e9b\",\"uuid\":\"c5c8bd37-4395-49fa-9413-e0fc0aff7658\",\"workbenchName\":\"接种台2\",\"workbenchNo\":\"S02\",\"workbenchNum\":4}";
		RedisDrugListData data = JSONUtil.toBean(x, RedisDrugListData.class);
		log.info(JSON.toJSONString(data));
		dispensingFunction.dropRecordAndMachine(data,1,"发药正常");

		}

	@Test
		void test20() throws  Exception{
		DrugRecordRequest drugRecordRequest = uploadController.getZcyCode("81905360040864463519");
		log.info(JSON.toJSONString(drugRecordRequest));
	}

	@Test
	void test21(){
		CabinetAHandRequest request = new CabinetAHandRequest();
		request.setWorkMode(CabinetConstants.Cabinet.CAB_A);
		request.setServoX(11);
		request.setDistanceX(747776);
		request.setServoZ(12);
		request.setDistanceZ(7244);
		request.setDistance(1200);
		cabinetAService.handGetDrug(request);
	}


	@Test
	void test22(){
		AutoData autoData = new AutoData();
		autoData.setStartBoxNo("A0101");
		autoData.setEndBoxNo("A0123");
		autoData.setLineNum(1);
		autoData.setSpeed(10);
		autoData.setThreshold(10);
		//偏移2个mm
		autoData.setOffsetDis(20);
		autoData.setSensorDis(70);

		vacMachineService.AutoProofread(autoData);

	}

	@Test
	void test23(){
		LambdaQueryWrapper<VacMachine> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(VacMachine::getDeleted,0)
				.eq(VacMachine::getBoxSpecId,14);
			queryWrapper.orderByAsc(VacMachine::getLineNum);
			queryWrapper.notIn(VacMachine::getLineNum, Arrays.asList(8, 10));
		queryWrapper .apply("line_num % 2 = 0");
		 List<VacMachine> vacMachineList =	vacMachineMapper.selectList(queryWrapper);
		 log.info(JSON.toJSONString(vacMachineList));
	}


	@Test
	void test24(){
		vacDrugRecordService.updateBatchNo();
	}


	@Test
	void test35(){

		List<Long> boxSepcIds =new ArrayList<>();
		boxSepcIds.add(13L);
		boxSepcIds.add(14L);

		LambdaQueryWrapper<VacMachine> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(VacMachine::getDeleted,0)
				//可用量要小于最大存储量
				.eq(VacMachine::getProductNo,"78977010201")
				.lt(VacMachine::getVaccineNum,7)
				.in(VacMachine::getBoxSpecId,boxSepcIds)
				.eq(VacMachine::getExpiredAt,Date.from(Instant.ofEpochMilli(1782748800000L)))
				.eq(VacMachine::getBatchNo,"Y0C281M")
				.eq(VacMachine::getDeleted,0)
				.eq(VacMachine::getStatus,1)
				//使用量、层数 升序排列
				.orderByDesc(VacMachine::getVaccineNum);

		queryWrapper.orderByAsc(VacMachine::getLineNum);
		if (boxSepcIds != null && boxSepcIds.contains(14L)) {
			queryWrapper.notIn(VacMachine::getLineNum, Arrays.asList(8, 10));
		}
		queryWrapper.orderByAsc(VacMachine::getBoxNo);
		List<VacMachine> vacMachineList =  vacMachineMapper.selectList(queryWrapper);

		log.info(String.valueOf(Date.from(Instant.ofEpochMilli(1782748800000L))));
		log.info(JSON.toJSONString(vacMachineList));

	}


	@Test
	void test44(){

		String result = "{\"body\":\"{\\\"success\\\":true,\\\"result\\\":{\\\"batchNo\\\":\\\"202505027A\\\",\\\"expiredAt\\\":1840809600000,\\\"price\\\":6880,\\\"productNo\\\":\\\"01109000910\\\",\\\"supervisedCode\\\":\\\"81901900532076362854\\\",\\\"tag\\\":null},\\\"code\\\":null,\\\"message\\\":null}\",\"headers\":{\"Connection\":\"keep-alive\",\"Content-Length\":\"193\",\"Date\":\"Fri, 30 Jan 2026 00:04:49 GMT\",\"Content-Type\":\"application/json\"},\"httpStatus\":200}\n";
		JSONObject outerJson = JSON.parseObject(result);
		String bodyString = outerJson.getString("body");

		// 解析内层 JSON 字符串
		JSONObject bodyJson = JSON.parseObject(bodyString);

		// result 是 JSON 对象，不是 JSON 数组
		JSONObject resultObj = bodyJson.getJSONObject("result");

		// 将 JSON 对象转换为 Java 对象
		VacDrug vacDrug = resultObj.toJavaObject(VacDrug.class);

		// 或者如果你需要列表形式，可以创建包含单个对象的列表
		List<VacDrug> resultList = Collections.singletonList(vacDrug);


		for(VacDrug vacDrugs :resultList){
			vacDrugService.vacSaveOrUpdateDrug(vacDrugs);
		}
	}


	@Test
	void test45(){
		vacSendDrugRecordService.getDrugBoxStatistics();


	}

	@Test
	void test46(){
		plcService.sendReadCommand(270,1);
	}


	@Test
	void test47(){
		//送药指令下发
		plcSendService.sendACabinetDispenseCmd();
	}

	/**
	 * 测试多个产品编码 productNoList 处理逻辑
	 * 验证：productNo 逗号分隔格式 + .in 查询
	 */
	@Test
	void test48() {
		// 模拟政采云 JSON 中某条 cmdList 的 productNos 含多个编码
		String json = "{\"success\":true,\"result\":[{\"cmdList\":[{\"price\":6880,\"tag\":null,\"class\":\"cn.gov.zcy.vaccine.vaccination.api.open.seedlings.dto.SeedlingMachineSubTaskDTO\",\"taskId\":\"09e58ebe98874a69b3bee5421a6c88a2\",\"productNos\":[\"01109000910\",\"01202000303\"]}],\"requestNo\":\"test-multi-productNo\",\"workbenchNo\":\"S04\"}],\"code\":null,\"message\":null}";

		// === 模拟 ZcyFunction 中的解析逻辑 ===
		JSONObject bodyJson = JSON.parseObject(json);
		JSONArray resultArray = bodyJson.getJSONArray("result");
		JSONObject resultObj = resultArray.getJSONObject(0);
		JSONArray cmdListArray = resultObj.getJSONArray("cmdList");

		// 解析 cmdList
		com.yiwan.vaccinedispenser.system.sys.data.zyc.CmdListData cmdListData =
				JSON.parseObject(cmdListArray.getJSONObject(0).toJSONString(),
						com.yiwan.vaccinedispenser.system.sys.data.zyc.CmdListData.class);

		// === 验证 productNos 包含多个编码 ===
		List<String> productNos = cmdListData.getProductNos();
		log.info("productNos: {}", productNos);
		assert productNos.size() == 2 : "应包含2个产品编码";
		assert productNos.contains("01109000910") : "应包含 01109000910";
		assert productNos.contains("01202000303") : "应包含 01202000303";

		// === 验证 productNo 逗号分隔格式 ===
		String productNoStr = String.join(",", productNos);
		log.info("productNo(入库格式): {}", productNoStr);
		assert productNoStr.equals("01109000910,01202000303") : "应存为逗号分隔格式";

		// === 验证 .in 查询（模拟三个发药函数的逻辑） ===
		VacGetVaccine vacGetVaccine = new VacGetVaccine();
		vacGetVaccine.setProductNo(productNoStr);
		vacGetVaccine.setProductNoList(productNos);

		LambdaQueryWrapper<VacMachine> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(VacMachine::getDeleted, "0")
				.gt(VacMachine::getVaccineUseNum, 0)
				.in(VacMachine::getStatus, 1, 2);
		if (vacGetVaccine.getProductNoList() != null && !vacGetVaccine.getProductNoList().isEmpty()) {
			wrapper.in(VacMachine::getProductNo, vacGetVaccine.getProductNoList());
			log.info("使用 .in 查询: productNoList={}", vacGetVaccine.getProductNoList());
		} else {
			wrapper.eq(VacMachine::getProductNo, vacGetVaccine.getProductNo());
			log.info("使用 .eq 查询: productNo={}", vacGetVaccine.getProductNo());
		}

		// 执行查询并记录结果
		List<VacMachine> drugList = vacMachineMapper.selectList(wrapper);
		log.info("查询结果数: {}", drugList != null ? drugList.size() : 0);
		if (drugList != null && !drugList.isEmpty()) {
			drugList.forEach(d -> log.info("  仓位={} 产品编码={} 名称={}",
					d.getBoxNo(), d.getProductNo(), d.getProductName()));
		} else {
			log.warn("未查到匹配仓位（可能数据库中无对应编码的库存）");
		}
	}
}
