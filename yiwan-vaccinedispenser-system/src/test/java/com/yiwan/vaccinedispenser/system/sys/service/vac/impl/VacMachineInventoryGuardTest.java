package com.yiwan.vaccinedispenser.system.sys.service.vac.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yiwan.vaccinedispenser.core.common.CommandEnums;
import com.yiwan.vaccinedispenser.core.common.SettingConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.core.exception.ServiceException;
import com.yiwan.vaccinedispenser.core.websocket.WebsocketService;
import com.yiwan.vaccinedispenser.system.dispensing.ConfigFunction;
import com.yiwan.vaccinedispenser.system.dispensing.SendDrugFunction;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacDrug;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachine;
import com.yiwan.vaccinedispenser.system.plc.manager.PlcSendService;
import com.yiwan.vaccinedispenser.system.sys.dao.VacMachineMapper;
import com.yiwan.vaccinedispenser.system.sys.data.ConfigSetting;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.MachineInventoryRequest;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacDrugService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineExceptionService;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VacMachineInventoryGuardTest {

    @Test
    void inventoryUsesExactLoadedVaccineIdInsteadOfProductNumberLookup() {
        VacDrugService vacDrugService = mock(VacDrugService.class);
        VacMachineExceptionService exceptionService = mock(VacMachineExceptionService.class);
        VacDrug loadedDrug = validDrug(2556L, "05734001501");
        when(vacDrugService.getById(2556L)).thenReturn(loadedDrug);

        VacMachineServiceImpl service = serviceWith(vacDrugService, exceptionService);
        VacDrug selected = service.resolveInventoryDrug(2556L, "05734001501", "测试疫苗", "A0326");

        assertSame(loadedDrug, selected);
        verify(vacDrugService).getById(2556L);
        verify(vacDrugService, never()).vacDrugGetValidDimensionsByProductNo(anyString());
    }

    @Test
    void invalidDimensionsAreReportedWithWarehouseAndBadValues() {
        VacDrugService vacDrugService = mock(VacDrugService.class);
        VacMachineExceptionService exceptionService = mock(VacMachineExceptionService.class);
        VacDrug badDrug = validDrug(2631L, "05734001501");
        badDrug.setVaccineWide(null);
        when(vacDrugService.getById(2631L)).thenReturn(badDrug);

        VacMachineServiceImpl service = serviceWith(vacDrugService, exceptionService);
        VacDrug selected = service.resolveInventoryDrug(2631L, "05734001501", "测试疫苗", "A0326");

        assertNull(selected);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(exceptionService).sendException(anyInt(), eq("测试疫苗"), messageCaptor.capture());
        String message = messageCaptor.getValue();
        assertTrue(message.contains("仓位号=A0326"));
        assertTrue(message.contains("vaccineId=2631"));
        assertTrue(message.contains("产品编码=05734001501"));
        assertTrue(message.contains("宽=null"));
        assertTrue(message.contains("已跳过该仓位"));
    }

    @Test
    void inventoryFallsBackToCompleteDimensionsForHistoricalBadVaccineId() {
        VacDrugService vacDrugService = mock(VacDrugService.class);
        VacMachineExceptionService exceptionService = mock(VacMachineExceptionService.class);
        VacDrug badDrug = validDrug(2631L, "05734001501");
        badDrug.setVaccineLong(null);
        badDrug.setVaccineWide(null);
        badDrug.setVaccineHigh(null);
        VacDrug completeDrug = validDrug(2556L, "05734001501");
        when(vacDrugService.getById(2631L)).thenReturn(badDrug);
        when(vacDrugService.vacDrugGetValidDimensionsByProductNo("05734001501")).thenReturn(completeDrug);

        VacMachineServiceImpl service = serviceWith(vacDrugService, exceptionService);
        VacDrug selected = service.resolveInventoryDrug(
                2631L, "05734001501", "测试疫苗", "A0326");

        assertSame(completeDrug, selected);
        verify(exceptionService, never()).sendException(anyInt(), anyString(), anyString());
    }

    @Test
    void inventoryCountCalculationUsesValidatedLength() {
        assertEquals(3, VacMachineServiceImpl.calculateInventoryNum(197, 600, 135));
    }

    @Test
    void inventoryStatusIsResetWhenAnUnexpectedExceptionOccurs() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "inventory-guard-test"),
                VacMachine.class);
        VacMachineMapper machineMapper = mock(VacMachineMapper.class);
        ConfigFunction configFunction = mock(ConfigFunction.class);
        SendDrugFunction sendDrugFunction = mock(SendDrugFunction.class);
        PlcSendService plcSendService = mock(PlcSendService.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

        ConfigSetting config = new ConfigSetting();
        config.setInventoryStart(true);
        config.setInventoryUpdate(false);
        config.setInventoryCountLen(600);
        when(machineMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(configFunction.getSettingConfigData()).thenReturn(config);
        when(valueOperations.get(anyString())).thenReturn("false");
        when(sendDrugFunction.handHasDrug()).thenReturn(false);
        doThrow(new IllegalStateException("PLC清盘指令异常"))
                .when(plcSendService).sendACabinetInventoryClear();

        VacMachineServiceImpl service = new VacMachineServiceImpl();
        ReflectionTestUtils.setField(service, "vacMachineMapper", machineMapper);
        ReflectionTestUtils.setField(service, "configFunction", configFunction);
        ReflectionTestUtils.setField(service, "sendDrugFunction", sendDrugFunction);
        ReflectionTestUtils.setField(service, "plcSendService", plcSendService);
        ReflectionTestUtils.setField(service, "valueOperations", valueOperations);

        assertThrows(IllegalStateException.class, () -> service.machineInventoryCount(null));

        InOrder inOrder = inOrder(valueOperations);
        inOrder.verify(valueOperations).set(RedisKeyConstant.DRUG_INVENTORY_START, "true");
        inOrder.verify(valueOperations).set(RedisKeyConstant.DRUG_INVENTORY_START, "false");
    }

    @Test
    @SuppressWarnings("unchecked")
    void handDrugBlocksInventoryAndRaisesWarehouseAlarm() throws Exception {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "inventory-hand-drug-test"),
                VacMachine.class);
        VacMachineMapper machineMapper = mock(VacMachineMapper.class);
        ConfigFunction configFunction = mock(ConfigFunction.class);
        SendDrugFunction sendDrugFunction = mock(SendDrugFunction.class);
        PlcSendService plcSendService = mock(PlcSendService.class);
        VacMachineExceptionService exceptionService = mock(VacMachineExceptionService.class);
        WebsocketService websocketService = mock(WebsocketService.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

        ConfigSetting config = inventoryConfig();
        when(machineMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(configFunction.getSettingConfigData()).thenReturn(config);
        when(valueOperations.get(anyString())).thenReturn("false");
        when(sendDrugFunction.handHasDrug()).thenReturn(true);

        VacMachineServiceImpl service = inventoryService(
                machineMapper, configFunction, sendDrugFunction, plcSendService,
                exceptionService, websocketService, valueOperations);

        ServiceException error = assertThrows(
                ServiceException.class,
                () -> service.machineInventoryCount(null));

        assertTrue(error.getMessage().contains("机械手上还有药，不能盘点"));
        verify(plcSendService, never()).sendACabinetInventoryClear();
        verify(exceptionService).sendException(
                eq(SettingConstants.MachineException.COUNTWARING.code),
                eq(""),
                eq(error.getMessage()));
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Map> commandCaptor = ArgumentCaptor.forClass(Map.class);
        verify(websocketService).sendInfo(
                eq(CommandEnums.MACHINE_STATUS_COMMAND.getCode()),
                commandCaptor.capture());
        assertEquals(CommandEnums.DEVICE_STATUS_SEND_DRUG_LIST_ERROR.getCode(),
                commandCaptor.getValue().get("code"));
        assertTrue(String.valueOf(commandCaptor.getValue().get("data"))
                .contains("机械手上还有药，不能盘点"));

        InOrder inOrder = inOrder(valueOperations);
        inOrder.verify(valueOperations).set(RedisKeyConstant.DRUG_INVENTORY_START, "true");
        inOrder.verify(valueOperations).set(RedisKeyConstant.DRUG_INVENTORY_START, "false");
    }

    @Test
    @SuppressWarnings("unchecked")
    void unknownHandSensorStateBlocksInventoryAndReleasesStatus() throws Exception {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "inventory-hand-sensor-error-test"),
                VacMachine.class);
        VacMachineMapper machineMapper = mock(VacMachineMapper.class);
        ConfigFunction configFunction = mock(ConfigFunction.class);
        SendDrugFunction sendDrugFunction = mock(SendDrugFunction.class);
        PlcSendService plcSendService = mock(PlcSendService.class);
        VacMachineExceptionService exceptionService = mock(VacMachineExceptionService.class);
        WebsocketService websocketService = mock(WebsocketService.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

        ConfigSetting config = inventoryConfig();
        when(machineMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(configFunction.getSettingConfigData()).thenReturn(config);
        when(valueOperations.get(anyString())).thenReturn("false");
        when(sendDrugFunction.handHasDrug())
                .thenThrow(new IllegalStateException("机械手传感器状态为空"));

        VacMachineServiceImpl service = inventoryService(
                machineMapper, configFunction, sendDrugFunction, plcSendService,
                exceptionService, websocketService, valueOperations);

        ServiceException error = assertThrows(
                ServiceException.class,
                () -> service.machineInventoryCount(null));

        assertTrue(error.getMessage().contains("没读到机械手传感器状态，不能盘点"));
        verify(plcSendService, never()).sendACabinetInventoryClear();
        verify(exceptionService).sendException(
                eq(SettingConstants.MachineException.COUNTWARING.code),
                eq(""),
                eq(error.getMessage()));
        verify(websocketService).sendInfo(
                eq(CommandEnums.MACHINE_STATUS_COMMAND.getCode()),
                any(Map.class));

        InOrder inOrder = inOrder(valueOperations);
        inOrder.verify(valueOperations).set(RedisKeyConstant.DRUG_INVENTORY_START, "true");
        inOrder.verify(valueOperations).set(RedisKeyConstant.DRUG_INVENTORY_START, "false");
    }

    @Test
    @SuppressWarnings("unchecked")
    void nullDistanceDuringRetrySkipsWarehouseWithoutUpdatingInventory() throws Exception {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "inventory-distance-null-test"),
                VacMachine.class);
        VacMachineMapper machineMapper = mock(VacMachineMapper.class);
        ConfigFunction configFunction = mock(ConfigFunction.class);
        SendDrugFunction sendDrugFunction = mock(SendDrugFunction.class);
        VacDrugService vacDrugService = mock(VacDrugService.class);
        VacMachineExceptionService exceptionService = mock(VacMachineExceptionService.class);
        WebsocketService websocketService = mock(WebsocketService.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        ListOperations<String, String> listOperations = mock(ListOperations.class);

        ConfigSetting config = new ConfigSetting();
        config.setInventoryStart(true);
        config.setInventoryUpdate(true);
        config.setInventoryCountLen(1245);
        config.setInventoryCountValue(1);

        VacMachine machine = new VacMachine();
        machine.setId(679L);
        machine.setBoxNo("A0222");
        machine.setCountX(11130);
        machine.setCountZ(1181);
        machine.setVaccineId(2335L);
        machine.setProductNo("00080001701_1");
        machine.setProductName("测试疫苗");
        machine.setVaccineNum(6);
        machine.setVaccineUseNum(6);

        VacDrug drug = validDrug(2335L, machine.getProductNo());
        drug.setVaccineLong(133);
        when(machineMapper.selectList(any())).thenReturn(Collections.singletonList(machine));
        when(configFunction.getSettingConfigData()).thenReturn(config);
        when(valueOperations.get(anyString())).thenReturn("false");
        when(valueOperations.get(RedisKeyConstant.DRUG_INVENTORY_START))
                .thenReturn("false", "true");
        when(listOperations.index(RedisKeyConstant.SEND_LIST, 0)).thenReturn(null);
        when(vacDrugService.getById(2335L)).thenReturn(drug);
        when(sendDrugFunction.handHasDrug()).thenReturn(false);
        //首次测距可计算出2只，第一次偏移复测返回null，复现现场日志。
        when(sendDrugFunction.getDistanceCount()).thenReturn(935, null);

        VacMachineServiceImpl service = new VacMachineServiceImpl();
        ReflectionTestUtils.setField(service, "vacMachineMapper", machineMapper);
        ReflectionTestUtils.setField(service, "configFunction", configFunction);
        ReflectionTestUtils.setField(service, "sendDrugFunction", sendDrugFunction);
        ReflectionTestUtils.setField(service, "vacDrugService", vacDrugService);
        ReflectionTestUtils.setField(service, "vacMachineExceptionService", exceptionService);
        ReflectionTestUtils.setField(service, "websocketService", websocketService);
        ReflectionTestUtils.setField(service, "valueOperations", valueOperations);
        ReflectionTestUtils.setField(service, "listOps", listOperations);

        service.machineInventoryCount((MachineInventoryRequest) null);

        verify(machineMapper, never()).updateNullById(any(VacMachine.class));
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(exceptionService).sendException(anyInt(), eq("测试疫苗"), messageCaptor.capture());
        assertTrue(messageCaptor.getValue().contains("仓位号=A0222"));
        assertTrue(messageCaptor.getValue().contains("传感器测量距离=null"));
        assertTrue(messageCaptor.getValue().contains("本次未修改库存"));
        verify(sendDrugFunction).moveHandServoInit(any());
    }

    private ConfigSetting inventoryConfig() {
        ConfigSetting config = new ConfigSetting();
        config.setInventoryStart(true);
        config.setInventoryUpdate(false);
        config.setInventoryCountLen(600);
        return config;
    }

    private VacMachineServiceImpl inventoryService(
            VacMachineMapper machineMapper,
            ConfigFunction configFunction,
            SendDrugFunction sendDrugFunction,
            PlcSendService plcSendService,
            VacMachineExceptionService exceptionService,
            WebsocketService websocketService,
            ValueOperations<String, String> valueOperations) {
        VacMachineServiceImpl service = new VacMachineServiceImpl();
        ReflectionTestUtils.setField(service, "vacMachineMapper", machineMapper);
        ReflectionTestUtils.setField(service, "configFunction", configFunction);
        ReflectionTestUtils.setField(service, "sendDrugFunction", sendDrugFunction);
        ReflectionTestUtils.setField(service, "plcSendService", plcSendService);
        ReflectionTestUtils.setField(service, "vacMachineExceptionService", exceptionService);
        ReflectionTestUtils.setField(service, "websocketService", websocketService);
        ReflectionTestUtils.setField(service, "valueOperations", valueOperations);
        return service;
    }

    private VacMachineServiceImpl serviceWith(
            VacDrugService vacDrugService,
            VacMachineExceptionService exceptionService) {
        VacMachineServiceImpl service = new VacMachineServiceImpl();
        ReflectionTestUtils.setField(service, "vacDrugService", vacDrugService);
        ReflectionTestUtils.setField(service, "vacMachineExceptionService", exceptionService);
        return service;
    }

    private VacDrug validDrug(Long id, String productNo) {
        VacDrug drug = new VacDrug();
        drug.setId(id);
        drug.setProductNo(productNo);
        drug.setVaccineLong(135);
        drug.setVaccineWide(34);
        drug.setVaccineHigh(28);
        drug.setDeleted(0);
        return drug;
    }
}
