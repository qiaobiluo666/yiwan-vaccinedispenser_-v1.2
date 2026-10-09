package com.yiwan.vaccinedispenser.system.sys.service.vac.impl;

import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachine;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class VacMachineAutoLoadingCandidateTest {

    private final VacMachineServiceImpl service = new VacMachineServiceImpl();

    @Test
    void autoXNullIsFiltered() {
        VacMachine invalid = machine(1L, null, 20);
        VacMachine valid = machine(2L, 10, 20);

        List<VacMachine> result = filter(Arrays.asList(invalid, valid), Collections.emptySet());

        assertEquals(1, result.size());
        assertSame(valid, result.get(0));
    }

    @Test
    void autoZNullIsFiltered() {
        VacMachine invalid = machine(1L, 10, null);
        VacMachine valid = machine(2L, 10, 20);

        List<VacMachine> result = filter(Arrays.asList(invalid, valid), Collections.emptySet());

        assertEquals(1, result.size());
        assertSame(valid, result.get(0));
    }

    @Test
    void validMachineAfterInvalidMachineIsStillRetained() {
        VacMachine missingX = machine(1L, null, 20);
        VacMachine missingZ = machine(2L, 10, null);
        VacMachine valid = machine(3L, 30, 40);

        List<VacMachine> result = filter(
                Arrays.asList(missingX, missingZ, valid), Collections.emptySet());

        assertEquals(1, result.size());
        assertSame(valid, result.get(0));
    }

    @Test
    void excludedMachineIsStillFiltered() {
        VacMachine excluded = machine(1L, 10, 20);
        VacMachine retained = machine(2L, 30, 40);

        List<VacMachine> result = filter(
                Arrays.asList(excluded, retained), new HashSet<>(Collections.singletonList(1L)));

        assertEquals(1, result.size());
        assertSame(retained, result.get(0));
    }

    @Test
    void zeroCoordinatesAreValid() {
        VacMachine origin = machine(1L, 0, 0);

        List<VacMachine> result = filter(Collections.singletonList(origin), Collections.emptySet());

        assertEquals(1, result.size());
        assertSame(origin, result.get(0));
    }

    @Test
    void plcModeDoesNotRequireAutoCoordinates() {
        ReflectionTestUtils.setField(service, "plcEnabled", true);
        VacMachine machineWithoutJavaCoordinates = machine(1L, null, null);

        List<VacMachine> result = filter(
                Collections.singletonList(machineWithoutJavaCoordinates), Collections.emptySet());

        assertEquals(1, result.size());
        assertSame(machineWithoutJavaCoordinates, result.get(0));
    }

    @SuppressWarnings("unchecked")
    private List<VacMachine> filter(List<VacMachine> machines, java.util.Set<Long> excludedIds) {
        return (List<VacMachine>) ReflectionTestUtils.invokeMethod(
                service, "filterAutoLoadingCandidates", machines, excludedIds);
    }

    private VacMachine machine(long id, Integer autoX, Integer autoZ) {
        VacMachine machine = new VacMachine();
        machine.setId(id);
        machine.setBoxNo("A" + id);
        machine.setAutoX(autoX);
        machine.setAutoZ(autoZ);
        return machine;
    }
}
