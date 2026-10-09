package com.yiwan.vaccinedispenser.system.sys.service.vac.impl;

import com.yiwan.vaccinedispenser.system.domain.model.vac.VacDrug;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacBoxSpec;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MarginServiceImplTest {

    @Test
    void usesAllCompatibleSpecsWhenDrugHasNoSpecifiedSpecs() {
        VacDrug drug = new VacDrug();
        drug.setVaccineWide(35);
        VacBoxSpec small = boxSpec(1L, 30, 5);
        VacBoxSpec compatible1 = boxSpec(2L, 40, 10);
        VacBoxSpec compatible2 = boxSpec(3L, 50, 20);
        List<VacBoxSpec> allSpecs = List.of(small, compatible1, compatible2);

        assertThat(MarginServiceImpl.resolveBoxSpecs(
                drug,
                allSpecs,
                Map.of(1L, small, 2L, compatible1, 3L, compatible2)))
                .containsExactly(compatible1, compatible2);
    }

    @Test
    void specifiedSpecsTakePriorityOverWidthCompatibility() {
        VacDrug drug = new VacDrug();
        drug.setVaccineWide(35);
        drug.setBoxSpecIds("1");
        VacBoxSpec specified = boxSpec(1L, 30, 5);
        VacBoxSpec compatible = boxSpec(2L, 40, 10);

        assertThat(MarginServiceImpl.resolveBoxSpecs(
                drug,
                List.of(specified, compatible),
                Map.of(1L, specified, 2L, compatible)))
                .containsExactly(specified);
    }

    @Test
    void calculatesCapacityUsingAutoDrugRuleAndConfiguredLimit() {
        VacDrug drug = new VacDrug();
        drug.setVaccineLong(30);
        drug.setMaxNum(8);

        assertThat(MarginServiceImpl.calcBoxMaxNum(300, drug)).isEqualTo(8);
    }

    @Test
    void returnsZeroForInvalidDimensions() {
        VacDrug drug = new VacDrug();
        drug.setVaccineLong(0);

        assertThat(MarginServiceImpl.calcBoxMaxNum(300, drug)).isZero();
        assertThat(MarginServiceImpl.calcBoxMaxNum(null, drug)).isZero();
    }

    @Test
    void normalizesSelectedProductNames() {
        assertThat(MarginServiceImpl.normalizeProductNames(
                List.of(" 疫苗A ", "疫苗B", "疫苗A", " ")))
                .containsExactly("疫苗A", "疫苗B");
    }

    private VacBoxSpec boxSpec(long id, int length, int ranges) {
        VacBoxSpec spec = new VacBoxSpec();
        spec.setId(id);
        spec.setLength(length);
        spec.setRanges(ranges);
        return spec;
    }
}
