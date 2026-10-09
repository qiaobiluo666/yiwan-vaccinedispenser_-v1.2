package com.yiwan.vaccinedispenser.system.until;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BoxSpecIdsTest {

    @Test
    void parsesValidUniquePositiveIds() {
        assertThat(BoxSpecIds.parse(" 3,2,invalid,3,0,-1, 5 "))
                .containsExactly(3L, 2L, 5L);
    }

    @Test
    void returnsEmptyListForBlankValue() {
        assertThat(BoxSpecIds.parse("  ")).isEmpty();
        assertThat(BoxSpecIds.parse(null)).isEmpty();
    }
}
