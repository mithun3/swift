package com.fx.common.handler;

import com.fx.common.handler.BusySpinWaitStrategy;
import com.fx.common.handler.PhasedBackOffWaitStrategy;
import com.fx.common.handler.WaitStrategy;
import com.fx.common.handler.YieldingWaitStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class WaitStrategyTest {

    @AfterEach
    void tearDown() {
        System.clearProperty("fx.waitstrategy");
    }

    @Test
    void testBusySpinFactory() {
        System.setProperty("fx.waitstrategy", "busyspin");
        WaitStrategy s1 = WaitStrategy.fromSystemProperty();
        WaitStrategy s2 = WaitStrategy.fromSystemProperty();
        
        assertInstanceOf(BusySpinWaitStrategy.class, s1);
        // BusySpin is a singleton
        assertSame(s1, s2);
    }

    @Test
    void testYieldingFactory() {
        System.setProperty("fx.waitstrategy", "yielding");
        WaitStrategy s1 = WaitStrategy.fromSystemProperty();
        WaitStrategy s2 = WaitStrategy.fromSystemProperty();
        
        assertInstanceOf(YieldingWaitStrategy.class, s1);
        // Yielding has state, so it must not be a singleton
        assertNotSame(s1, s2);
    }

    @Test
    void testPhasedFactory() {
        System.setProperty("fx.waitstrategy", "phased");
        WaitStrategy s1 = WaitStrategy.fromSystemProperty();
        WaitStrategy s2 = WaitStrategy.fromSystemProperty();
        
        assertInstanceOf(PhasedBackOffWaitStrategy.class, s1);
        // Phased has state, so it must not be a singleton
        assertNotSame(s1, s2);
    }

    @Test
    void testDefaultFactory() {
        System.clearProperty("fx.waitstrategy");
        WaitStrategy s1 = WaitStrategy.fromSystemProperty();
        assertInstanceOf(BusySpinWaitStrategy.class, s1);
    }

    @Test
    void testIdleAndResetAreSafe() {
        // Just verify they don't crash when called sequentially
        WaitStrategy s = new YieldingWaitStrategy();
        for (int i = 0; i < 2000; i++) {
            s.idle();
        }
        s.reset();
        
        WaitStrategy p = new PhasedBackOffWaitStrategy();
        for (int i = 0; i < 11000; i++) {
            p.idle();
        }
        p.reset();
    }
}
