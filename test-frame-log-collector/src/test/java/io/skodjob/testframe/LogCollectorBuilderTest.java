package io.skodjob.testframe;

import io.skodjob.testframe.annotations.TestVisualSeparator;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestVisualSeparator
public class LogCollectorBuilderTest {

    @Test
    void testPassingPodAndPodsAsResourcesToLogCollectorBuilder() {
        LogCollectorBuilder logCollectorBuilder = new LogCollectorBuilder()
            .withNamespacedResources("pod", "pods");

        assertEquals(Collections.emptyList(), logCollectorBuilder.getNamespacedResources());
    }

    @Test
    void testRuntimeExceptionIsThrownIfRootFolderPathIsNotSpecified() {
        LogCollectorBuilder logCollectorBuilder = new LogCollectorBuilder();

        assertThrows(RuntimeException.class, logCollectorBuilder::build);
    }
}
