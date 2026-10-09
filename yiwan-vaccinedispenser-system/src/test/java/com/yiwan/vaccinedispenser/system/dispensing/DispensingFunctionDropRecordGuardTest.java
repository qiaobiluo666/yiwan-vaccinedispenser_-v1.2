package com.yiwan.vaccinedispenser.system.dispensing;

import com.yiwan.vaccinedispenser.core.common.SettingConstants;
import com.yiwan.vaccinedispenser.system.sys.data.RedisDrugListData;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineExceptionService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacSendDrugRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DispensingFunctionDropRecordGuardTest {

    private VacSendDrugRecordService sendDrugRecordService;
    private VacMachineService machineService;
    private VacMachineExceptionService exceptionService;
    private DispensingFunction function;

    @BeforeEach
    void setUp() {
        sendDrugRecordService = mock(VacSendDrugRecordService.class);
        machineService = mock(VacMachineService.class);
        exceptionService = mock(VacMachineExceptionService.class);
        function = new DispensingFunction();
        ReflectionTestUtils.setField(function, "vacSendDrugRecordService", sendDrugRecordService);
        ReflectionTestUtils.setField(function, "vacMachineService", machineService);
        ReflectionTestUtils.setField(function, "vacMachineExceptionService", exceptionService);
    }

    @Test
    void recordInsertFailureDoesNotBlockConfirmedDropCommit() {
        RedisDrugListData data = queueData();
        doThrow(new RuntimeException("machine_no不能为空"))
                .when(sendDrugRecordService).sendDrugRecordAdd(data, 1, "发药正常");

        assertDoesNotThrow(() -> function.dropRecordAndMachine(data, 1, "发药正常"));

        verify(exceptionService).dropException(
                eq(SettingConstants.MachineException.SENDDRUG.code), eq(data),
                contains("继续扣减库存并完成当前任务"));
        verify(machineService).decrementNumById(665L);
    }

    @Test
    void alarmInsertFailureAlsoDoesNotBlockConfirmedDropCommit() {
        RedisDrugListData data = queueData();
        doThrow(new RuntimeException("machine_no不能为空"))
                .when(sendDrugRecordService).sendDrugRecordAdd(data, 1, "发药正常");
        doThrow(new RuntimeException("异常管理写入失败"))
                .when(exceptionService).dropException(
                        eq(SettingConstants.MachineException.SENDDRUG.code), eq(data),
                        contains("继续扣减库存并完成当前任务"));

        assertDoesNotThrow(() -> function.dropRecordAndMachine(data, 1, "发药正常"));

        verify(machineService).decrementNumById(665L);
    }

    private RedisDrugListData queueData() {
        RedisDrugListData data = new RedisDrugListData();
        data.setMachineId(665L);
        data.setMachineStatus(1);
        data.setBoxNo("A0208");
        data.setProductNo("00518000402_1_1");
        data.setProductName("流感疫苗");
        data.setRequestNo("request-1");
        data.setTaskId("task-1");
        data.setUuid(UUID.fromString("cd2bbc12-60c1-4571-a0c6-3b3c584b768b"));
        return data;
    }
}
