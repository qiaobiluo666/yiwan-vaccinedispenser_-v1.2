package com.yiwan.vaccinedispenser.system.dispensing;

import com.yiwan.vaccinedispenser.core.common.SettingConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.core.exception.ServiceException;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachine;
import com.yiwan.vaccinedispenser.system.sys.dao.VacMachineMapper;
import com.yiwan.vaccinedispenser.system.sys.data.RedisDrugListData;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineExceptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DispensingQueueCompensationTest {

    private static final Long MACHINE_ID = 26L;

    private VacMachineMapper vacMachineMapper;
    private ListOperations<String, String> listOps;
    private VacMachineExceptionService vacMachineExceptionService;
    private DispensingFunction dispensingFunction;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        vacMachineMapper = mock(VacMachineMapper.class);
        listOps = mock(ListOperations.class);
        vacMachineExceptionService = mock(VacMachineExceptionService.class);
        dispensingFunction = new DispensingFunction();
        ReflectionTestUtils.setField(dispensingFunction, "vacMachineMapper", vacMachineMapper);
        ReflectionTestUtils.setField(dispensingFunction, "listOps", listOps);
        ReflectionTestUtils.setField(
                dispensingFunction, "vacMachineExceptionService", vacMachineExceptionService);
        when(vacMachineMapper.decrementVaccineUseNumById(MACHINE_ID)).thenReturn(1);
    }

    @Test
    void firstRightPushFailureKeepsReservationAndDoesNotDeleteAnyQueue() {
        String dropKey = String.format(RedisKeyConstant.DROP_LIST, 1);
        when(listOps.rightPush(eq(dropKey), anyString()))
                .thenThrow(new RuntimeException("first push result unknown"));

        ServiceException exception = invokeAndExpectFailure(
                "发药", dropKey, RedisKeyConstant.BELT_LIST, RedisKeyConstant.SEND_LIST);

        assertTrue(exception.getMessage().contains("当前队列=" + dropKey));
        assertTrue(exception.getMessage().contains("已确认队列=[]"));
        assertReservationWasKept();
    }

    @Test
    void secondRightPushFailureKeepsReservationAndConfirmedFirstQueue() {
        when(listOps.rightPush(anyString(), anyString())).thenAnswer(invocation -> {
            if (RedisKeyConstant.SEND_LIST.equals(invocation.getArgument(0))) {
                throw new RuntimeException("second push result unknown");
            }
            return 1L;
        });

        ServiceException exception = invokeAndExpectFailure(
                "手动发药", RedisKeyConstant.DROP_HAND_LIST, RedisKeyConstant.SEND_LIST);

        assertTrue(exception.getMessage().contains("当前队列=" + RedisKeyConstant.SEND_LIST));
        assertTrue(exception.getMessage().contains(
                "已确认队列=[" + RedisKeyConstant.DROP_HAND_LIST + "]"));
        assertReservationWasKept();
    }

    @Test
    void thirdRightPushFailureKeepsReservationAndBothConfirmedQueues() {
        when(listOps.rightPush(anyString(), anyString())).thenAnswer(invocation -> {
            if (RedisKeyConstant.SEND_LIST.equals(invocation.getArgument(0))) {
                throw new RuntimeException("third push result unknown");
            }
            return 1L;
        });

        ServiceException exception = invokeAndExpectFailure(
                "PLC发药",
                RedisKeyConstant.PLC_SEND_LIST,
                RedisKeyConstant.BELT_LIST,
                RedisKeyConstant.SEND_LIST);

        assertTrue(exception.getMessage().contains("当前队列=" + RedisKeyConstant.SEND_LIST));
        assertTrue(exception.getMessage().contains(
                "已确认队列=[" + RedisKeyConstant.PLC_SEND_LIST
                        + ", " + RedisKeyConstant.BELT_LIST + "]"));
        assertReservationWasKept();
    }

    @Test
    void serverSidePushSuccessFollowedByClientExceptionStillKeepsReservation() {
        String dropKey = String.format(RedisKeyConstant.DROP_LIST, 1);
        List<String> serverQueue = new ArrayList<>();
        when(listOps.rightPush(eq(dropKey), anyString())).thenAnswer(invocation -> {
            //模拟 Redis 已经完成 RPUSH，但响应返回客户端前连接断开。
            serverQueue.add(invocation.getArgument(1));
            throw new RuntimeException("response lost after server executed RPUSH");
        });

        ServiceException exception = invokeAndExpectFailure(
                "发药", dropKey, RedisKeyConstant.BELT_LIST, RedisKeyConstant.SEND_LIST);

        assertEquals(1, serverQueue.size());
        assertTrue(serverQueue.get(0).contains("5f31e89c-2da8-4b5b-a001-111111111111"));
        assertTrue(exception.getMessage().contains("禁止自动恢复"));
        assertReservationWasKept();
    }

    @Test
    void nullRightPushResultIsTreatedAsUnknownAndKeepsReservation() {
        when(listOps.rightPush(anyString(), anyString())).thenReturn(null);

        ServiceException exception = invokeAndExpectFailure(
                "发药", String.format(RedisKeyConstant.DROP_LIST, 1));

        assertTrue(exception.getMessage().contains("Redis入队结果不确定"));
        assertReservationWasKept();
    }

    @Test
    void serializationFailureHappensBeforeDatabaseReservation() {
        VacMachine machine = machine();
        RedisDrugListData data = new RedisDrugListData() {
            @Override
            public String getTaskId() {
                throw new IllegalStateException("serialization getter failed");
            }
        };
        data.setUuid(UUID.fromString("5f31e89c-2da8-4b5b-a001-111111111111"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> dispensingFunction.reserveAvailableInventoryAndEnqueue(
                        "发药", machine, data, RedisKeyConstant.SEND_LIST));

        assertTrue(exception.getMessage().contains("尚未预占可用库存"));
        verify(vacMachineMapper, never()).decrementVaccineUseNumById(any());
        verify(listOps, never()).rightPush(anyString(), anyString());
    }

    private ServiceException invokeAndExpectFailure(String flowName, String... queueKeys) {
        VacMachine machine = machine();
        RedisDrugListData data = queueData();

        ServiceException exception = assertThrows(ServiceException.class,
                () -> dispensingFunction.reserveAvailableInventoryAndEnqueue(
                        flowName, machine, data, queueKeys));

        assertEquals(4, machine.getVaccineUseNum());
        assertEquals(4, data.getVaccineUseNum());
        verify(vacMachineExceptionService).dropException(
                eq(SettingConstants.MachineException.SENDWARING.code), eq(data), anyString());
        return exception;
    }

    private void assertReservationWasKept() {
        verify(vacMachineMapper).decrementVaccineUseNumById(MACHINE_ID);
        verifyNoMoreInteractions(vacMachineMapper);
        verify(listOps, never()).remove(anyString(), eq(1L), any());
    }

    private VacMachine machine() {
        VacMachine machine = new VacMachine();
        machine.setId(MACHINE_ID);
        machine.setBoxNo("A0101");
        machine.setLineNum(1);
        machine.setProductNo("P-001");
        machine.setProductName("测试疫苗");
        machine.setVaccineNum(5);
        machine.setVaccineUseNum(5);
        return machine;
    }

    private RedisDrugListData queueData() {
        RedisDrugListData data = new RedisDrugListData();
        data.setMachineId(MACHINE_ID);
        data.setBoxNo("A0101");
        data.setProductNo("P-001");
        data.setUuid(UUID.fromString("5f31e89c-2da8-4b5b-a001-111111111111"));
        return data;
    }
}
