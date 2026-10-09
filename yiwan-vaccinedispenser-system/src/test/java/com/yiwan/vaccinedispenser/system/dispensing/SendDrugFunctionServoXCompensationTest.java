package com.yiwan.vaccinedispenser.system.dispensing;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SendDrugFunctionServoXCompensationTest {

    @Test
    void firstCompensationStartsAfterEightSeconds() {
        assertThat(ReflectionTestUtils.getField(
                SendDrugFunction.class, "HAND_SERVO_X_COMPENSATION_START_WAIT_TIME"))
                .isEqualTo(8000);
    }

    @Test
    void positiveDirectionMovesOneMillimeterAtATimeUpToFiveMillimeters() {
        assertThat(SendDrugFunction.buildHandServoXMoveTargets(1000, 1))
                .containsExactly(1010, 1020, 1030, 1040, 1050);
    }

    @Test
    void negativeDirectionMovesOneMillimeterAtATimeUpToFiveMillimeters() {
        assertThat(SendDrugFunction.buildHandServoXMoveTargets(1000, -1))
                .containsExactly(990, 980, 970, 960, 950);
    }

    @Test
    void invalidDirectionIsRejected() {
        assertThatThrownBy(() -> SendDrugFunction.buildHandServoXMoveTargets(1000, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
