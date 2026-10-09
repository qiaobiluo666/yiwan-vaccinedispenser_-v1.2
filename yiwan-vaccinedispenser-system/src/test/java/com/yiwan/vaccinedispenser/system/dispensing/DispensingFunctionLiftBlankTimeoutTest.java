package com.yiwan.vaccinedispenser.system.dispensing;

import com.yiwan.vaccinedispenser.core.common.SettingConstants;
import com.yiwan.vaccinedispenser.system.sys.service.netty.CabinetAService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineExceptionService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DispensingFunctionLiftBlankTimeoutTest {

    @Test
    void openTimeoutIsReportedAsIoException() {
        Fixture fixture = fixture();

        fixture.function.openLiftBlank(1);

        verify(fixture.exceptionService).sendException(
                eq(SettingConstants.MachineException.IO.code),
                contains("抬升小挡板开启回包超时：A柜输出口12"));
    }

    @Test
    void closeTimeoutIsLoggedWithoutCreatingBlockingIoException() {
        Fixture fixture = fixture();

        fixture.function.closeLiftBlank(1);

        verifyNoInteractions(fixture.exceptionService);
    }

    @SuppressWarnings("unchecked")
    private Fixture fixture() {
        DispensingFunction function = new DispensingFunction();
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        CabinetAService cabinetAService = mock(CabinetAService.class);
        VacMachineExceptionService exceptionService = mock(VacMachineExceptionService.class);

        when(valueOperations.get(org.mockito.ArgumentMatchers.anyString())).thenReturn(null);
        ReflectionTestUtils.setField(function, "valueOperations", valueOperations);
        ReflectionTestUtils.setField(function, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(function, "cabinetAService", cabinetAService);
        ReflectionTestUtils.setField(function, "vacMachineExceptionService", exceptionService);
        return new Fixture(function, exceptionService);
    }

    private record Fixture(
            DispensingFunction function,
            VacMachineExceptionService exceptionService) {
    }
}
