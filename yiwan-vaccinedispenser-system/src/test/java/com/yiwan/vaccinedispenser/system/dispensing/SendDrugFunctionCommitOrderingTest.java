package com.yiwan.vaccinedispenser.system.dispensing;

import com.yiwan.vaccinedispenser.system.dispensing.reservation.AutoDrugReservationManager;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachine;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugRecordRequest;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacDrugRecordService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SendDrugFunctionCommitOrderingTest {

    @Test
    void redisDiagnosticFailureCannotBlockInventoryCommitAfterPhysicalDrop() throws Exception {
        SendDrugFunction function = new SendDrugFunction();
        VacDrugRecordService recordService = mock(VacDrugRecordService.class);
        VacMachineService machineService = mock(VacMachineService.class);
        AutoDrugReservationManager reservationManager = mock(AutoDrugReservationManager.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        inject(function, recordService, machineService, reservationManager, valueOperations);

        DrugRecordRequest request = request();
        VacMachine committed = new VacMachine();
        committed.setVaccineNum(10);
        committed.setVaccineUseNum(10);
        when(machineService.getById(26L)).thenReturn(committed);
        when(reservationManager.getPendingCount(26L))
                .thenThrow(new IllegalStateException("Redis disconnected"));

        Object reservedBox = reservedBox(request);

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(
                function, "commitReservedDrug", reservedBox));

        InOrder order = inOrder(recordService, reservationManager);
        order.verify(recordService).addAutoDrugRecordAndInventory(request, 1, 10);
        order.verify(reservationManager).getPendingCount(26L);
    }

    @Test
    void databaseCommitFailureDoesNotRunRedisDiagnostics() throws Exception {
        SendDrugFunction function = new SendDrugFunction();
        VacDrugRecordService recordService = mock(VacDrugRecordService.class);
        VacMachineService machineService = mock(VacMachineService.class);
        AutoDrugReservationManager reservationManager = mock(AutoDrugReservationManager.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        inject(function, recordService, machineService, reservationManager, valueOperations);

        DrugRecordRequest request = request();
        RuntimeException databaseError = new RuntimeException("database unavailable");
        org.mockito.Mockito.doThrow(databaseError)
                .when(recordService)
                .addAutoDrugRecordAndInventory(request, 1, 10);

        Object reservedBox = reservedBox(request);

        assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(
                function, "commitReservedDrug", reservedBox));
        verify(machineService, never()).getById(26L);
        verify(reservationManager, never()).getPendingCount(26L);
        verify(valueOperations, never()).get(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void redisAutoDrugFlagDiagnosticFailureCannotBlockInventoryCommit() throws Exception {
        SendDrugFunction function = new SendDrugFunction();
        VacDrugRecordService recordService = mock(VacDrugRecordService.class);
        VacMachineService machineService = mock(VacMachineService.class);
        AutoDrugReservationManager reservationManager = mock(AutoDrugReservationManager.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        inject(function, recordService, machineService, reservationManager, valueOperations);

        DrugRecordRequest request = request();
        VacMachine committed = new VacMachine();
        committed.setVaccineNum(10);
        committed.setVaccineUseNum(10);
        when(machineService.getById(26L)).thenReturn(committed);
        when(reservationManager.getPendingCount(26L)).thenReturn(0L);
        when(valueOperations.get(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new IllegalStateException("Redis disconnected"));

        Object reservedBox = reservedBox(request);

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(
                function, "commitReservedDrug", reservedBox));
        verify(recordService).addAutoDrugRecordAndInventory(request, 1, 10);
    }

    @Test
    void physicalDropConfirmationHasNoRedisOrHomingBeforeDatabaseCommit() throws Exception {
        String source = Files.readString(sendDrugFunctionSource(), StandardCharsets.UTF_8);
        int confirmation = source.indexOf("// HAND_SENSOR=RESET 是真实库存入库的唯一物理依据。");
        int commit = source.indexOf("commitReservedDrug(reservedBox);", confirmation);
        int release = source.indexOf("releaseReservationAfterCommit(reservedBox);", commit);
        int clampHome = source.indexOf(
                "cabinetAStepInit(CabinetConstants.CabinetAStepMode.CLAMP);", release);
        int blockHome = source.indexOf(
                "cabinetAStepInit(CabinetConstants.CabinetAStepMode.BLOCK);", release);
        int servoHome = source.indexOf("moveHandServoInit(configData);", release);

        assertThat(confirmation).isGreaterThanOrEqualTo(0);
        assertThat(commit).isGreaterThan(confirmation);
        assertThat(release).isGreaterThan(commit);
        assertThat(clampHome).isGreaterThan(release);
        assertThat(blockHome).isGreaterThan(release);
        assertThat(servoHome).isGreaterThan(release);

        String beforeCommit = source.substring(confirmation, commit);
        assertThat(beforeCommit)
                .doesNotContain("valueOperations.", "autoDrugReservationManager.",
                        "cabinetAStepInit(", "waitCabinetAStepEnd(", "moveHandServoInit(");
    }

    private void inject(
            SendDrugFunction function,
            VacDrugRecordService recordService,
            VacMachineService machineService,
            AutoDrugReservationManager reservationManager,
            ValueOperations<String, String> valueOperations) {
        ReflectionTestUtils.setField(function, "vacDrugRecordService", recordService);
        ReflectionTestUtils.setField(function, "vacMachineService", machineService);
        ReflectionTestUtils.setField(function, "autoDrugReservationManager", reservationManager);
        ReflectionTestUtils.setField(function, "valueOperations", valueOperations);
    }

    private DrugRecordRequest request() {
        DrugRecordRequest request = new DrugRecordRequest();
        request.setMachineId(26L);
        request.setMachineNo("A01");
        request.setProductNo("P-001");
        request.setProductName("测试疫苗");
        request.setBatchNo("B-001");
        return request;
    }

    private Object reservedBox(DrugRecordRequest request) throws Exception {
        Class<?> type = Class.forName(SendDrugFunction.class.getName() + "$ReservedDrugBox");
        Constructor<?> constructor = type.getDeclaredConstructor(
                DrugRecordRequest.class, String.class, String.class,
                int.class, long.class, int.class);
        constructor.setAccessible(true);
        return constructor.newInstance(request, "trace-1", "session-1", 9, 1L, 10);
    }

    private Path sendDrugFunctionSource() {
        Path moduleSource = Path.of("src", "main", "java", "com", "yiwan",
                "vaccinedispenser", "system", "dispensing", "SendDrugFunction.java");
        if (Files.exists(moduleSource)) {
            return moduleSource;
        }
        return Path.of("yiwan-vaccinedispenser-system").resolve(moduleSource);
    }
}
