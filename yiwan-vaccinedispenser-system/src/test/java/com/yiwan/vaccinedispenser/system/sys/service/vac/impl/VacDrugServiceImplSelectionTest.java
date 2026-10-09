package com.yiwan.vaccinedispenser.system.sys.service.vac.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacDrug;
import com.yiwan.vaccinedispenser.system.sys.dao.VacDrugMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VacDrugServiceImplSelectionTest {

    @Test
    void prefersCompleteDimensionsOverNewerIncompleteDuplicate() {
        VacDrug complete = drug(2556L, LocalDateTime.of(2026, 8, 20, 10, 0), 135, 34, 28);
        VacDrug newerIncomplete = drug(2631L, LocalDateTime.of(2026, 8, 21, 10, 0), null, null, null);

        VacDrug selected = VacDrugServiceImpl.selectPreferredDrug(List.of(newerIncomplete, complete));

        assertEquals(2556L, selected.getId());
    }

    @Test
    void productNumberGetFiltersOutDuplicateWithMissingLength() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "drug-selection-test"),
                VacDrug.class);
        VacDrug complete = drug(2556L, LocalDateTime.of(2026, 8, 20, 10, 0), 137, 36, 26);
        VacDrug newerMissingLength = drug(2631L, LocalDateTime.of(2026, 8, 21, 10, 0), null, null, null);
        VacDrugMapper mapper = mock(VacDrugMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(newerMissingLength, complete));
        VacDrugServiceImpl service = new VacDrugServiceImpl();
        ReflectionTestUtils.setField(service, "vacDrugMapper", mapper);

        VacDrug selected = service.vacDrugGetValidDimensionsByProductNo("05734001501");

        assertEquals(2556L, selected.getId());
        assertEquals(137, selected.getVaccineLong());
    }

    @Test
    void ordinaryProductLookupStillFindsDrugBeforeDimensionsAreEntered() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "drug-scan-test"),
                VacDrug.class);
        VacDrug incomplete = drug(2631L, LocalDateTime.of(2026, 8, 21, 10, 0), null, null, null);
        VacDrugMapper mapper = mock(VacDrugMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(incomplete));
        VacDrugServiceImpl service = new VacDrugServiceImpl();
        ReflectionTestUtils.setField(service, "vacDrugMapper", mapper);

        VacDrug selected = service.vacDrugGetByproductNo("05734001501");

        assertEquals(2631L, selected.getId());
    }

    @Test
    void choosesNewestRecordWhenMultipleRecordsHaveCompleteDimensions() {
        VacDrug older = drug(2556L, LocalDateTime.of(2026, 8, 20, 10, 0), 135, 34, 28);
        VacDrug newer = drug(2700L, LocalDateTime.of(2026, 8, 22, 10, 0), 136, 35, 29);

        VacDrug selected = VacDrugServiceImpl.selectPreferredDrug(List.of(older, newer));

        assertEquals(2700L, selected.getId());
    }

    @Test
    void rejectsAllRecordsWhenVaccineLengthIsMissing() {
        VacDrug older = drug(2557L, LocalDateTime.of(2026, 8, 20, 10, 0), null, null, null);
        VacDrug newer = drug(2632L, LocalDateTime.of(2026, 8, 21, 10, 0), null, null, null);

        VacDrug selected = VacDrugServiceImpl.selectPreferredDrug(List.of(older, newer));

        assertNull(selected);
    }

    @Test
    void rejectsPositiveLengthRecordWhenWidthOrHeightIsMissing() {
        VacDrug validLength = drug(2557L, LocalDateTime.of(2026, 8, 20, 10, 0), 135, null, null);
        VacDrug newerMissingLength = drug(2632L, LocalDateTime.of(2026, 8, 21, 10, 0), null, null, null);

        VacDrug selected = VacDrugServiceImpl.selectPreferredDrug(List.of(newerMissingLength, validLength));

        assertNull(selected);
    }

    @Test
    void returnsNullForEmptyCandidateList() {
        assertNull(VacDrugServiceImpl.selectPreferredDrug(List.of()));
    }

    private VacDrug drug(Long id, LocalDateTime updateTime, Integer length, Integer width, Integer height) {
        VacDrug drug = new VacDrug();
        drug.setId(id);
        drug.setUpdateTime(updateTime);
        drug.setVaccineLong(length);
        drug.setVaccineWide(width);
        drug.setVaccineHigh(height);
        return drug;
    }
}
