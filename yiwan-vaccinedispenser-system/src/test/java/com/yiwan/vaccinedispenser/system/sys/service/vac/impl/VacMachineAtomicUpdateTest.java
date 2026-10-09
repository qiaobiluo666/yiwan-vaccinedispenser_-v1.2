package com.yiwan.vaccinedispenser.system.sys.service.vac.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.yiwan.vaccinedispenser.system.sys.dao.VacMachineMapper;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugRecordRequest;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.MappedStatement;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VacMachineAtomicUpdateTest {

    @Test
    void incrementInventoryReturnsMapperAffectedRows() {
        VacMachineMapper mapper = mock(VacMachineMapper.class);
        VacMachineServiceImpl service = serviceWith(mapper);
        DrugRecordRequest request = request(26L);
        when(mapper.incrementInventoryIfBelowMax(request, 1, 10)).thenReturn(1);

        int affectedRows = service.incrementInventoryIfBelowMax(request, 1, 10);

        assertEquals(1, affectedRows);
        verify(mapper).incrementInventoryIfBelowMax(request, 1, 10);
    }

    @Test
    void incrementInventoryReturnsZeroWhenCapacityConditionRejectsUpdate() {
        VacMachineMapper mapper = mock(VacMachineMapper.class);
        VacMachineServiceImpl service = serviceWith(mapper);
        DrugRecordRequest request = request(26L);
        when(mapper.incrementInventoryIfBelowMax(request, 1, 10)).thenReturn(0);

        assertEquals(0, service.incrementInventoryIfBelowMax(request, 1, 10));
    }

    @Test
    void decrementAvailableInventoryReturnsMapperAffectedRows() {
        VacMachineMapper mapper = mock(VacMachineMapper.class);
        VacMachineServiceImpl service = serviceWith(mapper);
        when(mapper.decrementVaccineUseNumById(26L)).thenReturn(1);

        assertEquals(1, service.decrementVaccineUseNumById(26L));
        verify(mapper).decrementVaccineUseNumById(26L);
    }

    @Test
    void invalidAtomicUpdateArgumentsAreRejectedBeforeSqlExecution() {
        VacMachineMapper mapper = mock(VacMachineMapper.class);
        VacMachineServiceImpl service = serviceWith(mapper);
        DrugRecordRequest missingMachine = request(null);

        assertThrows(IllegalArgumentException.class,
                () -> service.incrementInventoryIfBelowMax(missingMachine, 1, 10));
        assertThrows(IllegalArgumentException.class,
                () -> service.incrementInventoryIfBelowMax(request(26L), 1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> service.decrementVaccineUseNumById(null));

        verify(mapper, never()).incrementInventoryIfBelowMax(missingMachine, 1, 10);
        verify(mapper, never()).decrementVaccineUseNumById(null);
    }

    @Test
    void mapperSqlUsesAtomicCapacityGuardAndDoesNotOverwriteRealInventoryOnDispense() throws Exception {
        MybatisConfiguration configuration = new MybatisConfiguration();
        String resource = "mapper/vac/VacMachineMapper.xml";
        try (InputStream input = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }

        DrugRecordRequest request = request(26L);
        Map<String, Object> incrementParams = new HashMap<>();
        incrementParams.put("request", request);
        incrementParams.put("status", 1);
        incrementParams.put("maxCapacity", 10);
        String incrementSql = sql(configuration, "incrementInventoryIfBelowMax", incrementParams);

        assertTrue(incrementSql.contains("vaccine_num = coalesce(vaccine_num, 0) + 1"));
        assertTrue(incrementSql.contains("vaccine_use_num = coalesce(vaccine_use_num, 0) + 1"));
        assertTrue(incrementSql.contains("where id = ? and deleted = 0"));
        assertTrue(incrementSql.contains("coalesce(vaccine_num, 0) < ?"));

        Map<String, Object> decrementParams = new HashMap<>();
        decrementParams.put("machineId", 26L);
        String decrementSql = sql(configuration, "decrementVaccineUseNumById", decrementParams);

        assertTrue(decrementSql.contains("set vaccine_use_num = vaccine_use_num - 1"));
        assertTrue(decrementSql.contains("and vaccine_use_num > 0"));
        assertFalse(decrementSql.contains("vaccine_num"));
        assertFalse(decrementSql.contains("update_time"));

    }

    private VacMachineServiceImpl serviceWith(VacMachineMapper mapper) {
        VacMachineServiceImpl service = new VacMachineServiceImpl();
        ReflectionTestUtils.setField(service, "vacMachineMapper", mapper);
        return service;
    }

    private DrugRecordRequest request(Long machineId) {
        DrugRecordRequest request = new DrugRecordRequest();
        request.setMachineId(machineId);
        request.setVaccineId(100L);
        request.setProductNo("P-001");
        request.setProductName("测试疫苗");
        request.setBatchNo("B-001");
        return request;
    }

    private String sql(MybatisConfiguration configuration, String statementName, Object parameters) {
        String statementId = VacMachineMapper.class.getName() + "." + statementName;
        MappedStatement statement = configuration.getMappedStatement(statementId);
        return statement.getBoundSql(parameters).getSql()
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}
