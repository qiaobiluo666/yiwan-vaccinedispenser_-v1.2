package com.yiwan.vaccinedispenser.system.dispensing;

import com.yiwan.vaccinedispenser.system.dispensing.reservation.AutoDrugReleaseOutcome;
import com.yiwan.vaccinedispenser.system.dispensing.reservation.AutoDrugReleaseResult;
import com.yiwan.vaccinedispenser.system.dispensing.reservation.AutoDrugReservation;
import com.yiwan.vaccinedispenser.system.dispensing.reservation.AutoDrugReservationManager;
import com.yiwan.vaccinedispenser.system.dispensing.reservation.AutoDrugReservationOutcome;
import com.yiwan.vaccinedispenser.system.dispensing.reservation.AutoDrugReservationResult;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachine;
import com.yiwan.vaccinedispenser.system.sys.data.DistanceServoData;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugRecordRequest;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SendDrugFunctionReservationRaceTest {

    /**
     * Regression for this interleaving:
     * B reads DB=9 -> A commits DB=10 and releases its pending -> B reserves with stale DB=9.
     * B must re-read DB after its Redis reservation and release before any physical movement.
     */
    @Test
    void postReservationRecheckRejectsStaleNineWhenDatabaseHasBecomeTen() {
        SendDrugFunction function = spy(new SendDrugFunction());
        AutoDrugReservationManager reservationManager =
                mock(AutoDrugReservationManager.class);
        VacMachineService machineService = mock(VacMachineService.class);
        ReflectionTestUtils.setField(
                function, "autoDrugReservationManager", reservationManager);
        ReflectionTestUtils.setField(function, "vacMachineService", machineService);

        DrugRecordRequest source = drug("CODE-001");
        DrugRecordRequest candidate = drug("CODE-001");
        candidate.setMachineId(26L);
        candidate.setMachineNo("A01");
        candidate.setAutoX(100);
        candidate.setAutoZ(200);
        DistanceServoData distance = new DistanceServoData();
        distance.setVaccineLong(100);

        doReturn(10).when(function).getDrugNum(anyInt(), any(DrugRecordRequest.class));
        doReturn(candidate, null).when(function).findBoxExcludeMachineIds(
                eq(distance), eq(source), anySet());

        VacMachine staleNine = machine(9);
        VacMachine latestTen = machine(10);
        when(machineService.getById(26L)).thenReturn(staleNine, latestTen);
        when(reservationManager.reserve(any())).thenReturn(
                AutoDrugReservationResult.builder()
                        .outcome(AutoDrugReservationOutcome.RESERVED)
                        .reservation(AutoDrugReservation.builder()
                                .reservationId("trace-1")
                                .machineId(26L)
                                .build())
                        .pendingCount(1)
                        .message("仓位预留成功")
                        .build());
        when(reservationManager.release("trace-1")).thenReturn(
                AutoDrugReleaseResult.builder()
                        .outcome(AutoDrugReleaseOutcome.RELEASED)
                        .reservationId("trace-1")
                        .machineId(26L)
                        .pendingCount(0)
                        .build());

        Object result = ReflectionTestUtils.invokeMethod(
                function, "findAndReserveBox",
                distance, source, "session-1", "trace-1");

        assertThat(result).as("旧库存9不能让第11盒进入机械流程").isNull();
        verify(reservationManager).release("trace-1");
        verify(function, atLeastOnce()).findBoxExcludeMachineIds(
                eq(distance), eq(source), anySet());
    }

    private DrugRecordRequest drug(String supervisedCode) {
        DrugRecordRequest request = new DrugRecordRequest();
        request.setVaccineId(100L);
        request.setProductNo("P-001");
        request.setProductName("测试疫苗");
        request.setBatchNo("B-001");
        request.setExpiredAt(new Date(1_800_000_000_000L));
        request.setSupervisedCode(supervisedCode);
        return request;
    }

    private VacMachine machine(int vaccineNum) {
        VacMachine machine = new VacMachine();
        machine.setId(26L);
        machine.setBoxNo("A01");
        machine.setDeleted(0);
        machine.setVaccineNum(vaccineNum);
        machine.setVaccineUseNum(vaccineNum);
        machine.setVaccineId(100L);
        machine.setProductNo("P-001");
        machine.setProductName("测试疫苗");
        machine.setBatchNo("B-001");
        machine.setExpiredAt(new Date(1_800_000_000_000L));
        machine.setAutoX(100);
        machine.setAutoZ(200);
        return machine;
    }
}
