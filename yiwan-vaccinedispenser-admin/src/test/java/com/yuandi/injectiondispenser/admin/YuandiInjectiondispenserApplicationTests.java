package com.yuandi.injectiondispenser.admin;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.yiwan.vaccinedispenser.YiwanVaccinedispenserApplication;
import com.yiwan.vaccinedispenser.core.common.CommandEnums;
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
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacGetVaccine;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachine;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachineDrug;
import com.yiwan.vaccinedispenser.system.sys.dao.VacGetVaccineMapper;
import com.yiwan.vaccinedispenser.system.sys.data.ConfigSetting;
import com.yiwan.vaccinedispenser.system.sys.data.DistanceServoData;
import com.yiwan.vaccinedispenser.system.sys.data.RedisDrugListData;
import com.yiwan.vaccinedispenser.system.sys.data.SendBtnData;
import com.yiwan.vaccinedispenser.system.sys.data.request.netty.DropRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugRecordRequest;
import com.yiwan.vaccinedispenser.system.sys.data.zyc.InventoryReportData;
import com.yiwan.vaccinedispenser.system.sys.data.zyc.SendVaccineResultData;
import com.yiwan.vaccinedispenser.system.sys.data.zyc.SendVaccineResultRequest;
import com.yiwan.vaccinedispenser.system.sys.service.netty.CabinetAService;
import com.yiwan.vaccinedispenser.system.sys.service.netty.CabinetBService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.*;
import com.yiwan.vaccinedispenser.system.until.NettyUtils;
import com.yiwan.vaccinedispenser.system.until.VacUntil;
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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;

@SpringBootTest(classes = YiwanVaccinedispenserApplication.class)
@RunWith(SpringRunner.class)
@Slf4j
class YuandiInjectiondispenserApplicationTests {


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
		Integer sensorNum =1;
		Integer ioNum =3;

		dispensingFunction.dropDrug(sensorNum,ioNum, SettingConstants.IO_DROP_WAIT_TIME);

		sensorNum =2;
		ioNum =2;
		dispensingFunction.dropDrug(sensorNum,ioNum, SettingConstants.IO_DROP_WAIT_TIME);

		sensorNum =3;
		ioNum =3;
		dispensingFunction.dropDrug(sensorNum,ioNum, SettingConstants.IO_DROP_WAIT_TIME);


		sensorNum =4;
		ioNum =2;
		dispensingFunction.dropDrug(sensorNum,ioNum, SettingConstants.IO_DROP_WAIT_TIME);



	}

	@Test
	void test5() throws Exception {
		VacGetVaccine vacGetVaccine = new VacGetVaccine();
		vacGetVaccine.setProductNo("79524002604");
		vacGetVaccine.setProductName("进-轮毒五价-L-默沙东-1/2ml/支-液其他口1");

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

}
