package com.yiwan.vaccinedispenser.system.dispensing;

import com.yiwan.vaccinedispenser.core.common.CommandEnums;
import com.yiwan.vaccinedispenser.core.common.SettingConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.core.exception.ServiceException;
import com.yiwan.vaccinedispenser.core.websocket.WebsocketService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineExceptionService;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SendDrugThreadManagerHandAlarmTest {

    @Test
    @SuppressWarnings("unchecked")
    void handDrugBeforeStartIsSavedToExceptionManagementAndPushedToFrontend() throws Exception {
        SendDrugThreadManager manager = new SendDrugThreadManager(mock(TaskExecutor.class));
        SendDrugFunction function = mock(SendDrugFunction.class);
        VacMachineExceptionService exceptionService = mock(VacMachineExceptionService.class);
        WebsocketService websocketService = mock(WebsocketService.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);

        when(function.handHasDrug()).thenReturn(true);
        ReflectionTestUtils.setField(manager, "sendDrugFunction", function);
        ReflectionTestUtils.setField(manager, "vacMachineExceptionService", exceptionService);
        ReflectionTestUtils.setField(manager, "websocketService", websocketService);
        ReflectionTestUtils.setField(manager, "valueOperations", values);

        assertThatThrownBy(manager::validateHandEmptyBeforeInit)
                .isInstanceOf(ServiceException.class)
                .hasMessage("初始化异常：机械手上有苗");

        verify(values).set(RedisKeyConstant.autoDrug.AUTO_DRUG_START, "false");
        verify(values).set(RedisKeyConstant.autoDrug.AUTO_DRUG_BELT_START, "false");
        verify(exceptionService).sendException(
                SettingConstants.MachineException.SEND.code, "", "初始化异常：机械手上有苗");
        verify(websocketService).sendInfo(
                eq(CommandEnums.ON_CABINET_WEB.getCode()), anyMap());
    }
}
