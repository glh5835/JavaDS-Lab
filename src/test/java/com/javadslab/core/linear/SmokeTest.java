package com.javadslab.core.linear;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 工具链冒烟测试：验证 Maven + JUnit 5 + JDK 17 端到端可用。 */
class SmokeTest {
    @Test
    void jdk17FeaturesWork() {
        var list = new java.util.ArrayList<Integer>();
        list.add(42);
        assertEquals(42, list.get(0));
        assertEquals(17, Runtime.version().feature());
    }
}
