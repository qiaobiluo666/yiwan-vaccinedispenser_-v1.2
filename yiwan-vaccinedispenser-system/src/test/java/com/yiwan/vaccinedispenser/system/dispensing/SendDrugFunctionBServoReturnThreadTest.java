package com.yiwan.vaccinedispenser.system.dispensing;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

class SendDrugFunctionBServoReturnThreadTest {

    @Test
    void threadStartFailureReturnsRegisteredSessionTask() {
        SendDrugFunction function = spy(new SendDrugFunction());
        SendDrugThreadManager threadManager = mock(SendDrugThreadManager.class);
        Thread rejectedThread = mock(Thread.class);
        ReflectionTestUtils.setField(function, "sendDrugThreadManager", threadManager);
        doReturn(rejectedThread).when(function)
                .createBServoReturnThread(any(Runnable.class), anyString());
        doThrow(new IllegalStateException("thread start rejected"))
                .when(rejectedThread).start();

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                function, "startBServoReturnThread", "session-1", "trace-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("thread start rejected");

        InOrder callbacks = inOrder(threadManager);
        callbacks.verify(threadManager).onBServoReturnFlowStarted("session-1", "trace-1");
        callbacks.verify(threadManager).onBServoReturnFlowFinished("session-1", "trace-1");
        callbacks.verifyNoMoreInteractions();
    }
}
