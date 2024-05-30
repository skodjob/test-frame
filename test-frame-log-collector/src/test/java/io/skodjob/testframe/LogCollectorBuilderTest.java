package io.skodjob.testframe;

import io.skodjob.testframe.annotations.TestVisualSeparator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestVisualSeparator
public class LogCollectorBuilderTest {

    @Test
    void testPassingPodAndPodsAsResourcesToLogCollectorBuilder() {
        LogCollectorBuilder logCollectorBuilder = new LogCollectorBuilder()
            .withResources("pod", "pods");

        assertEquals(Collections.emptyList(), logCollectorBuilder.getResources());
    }

    @Test
    void testRuntimeExceptionIsThrownIfRootFolderPathIsNotSpecified() {
        LogCollectorBuilder logCollectorBuilder = new LogCollectorBuilder();

        assertThrows(RuntimeException.class, logCollectorBuilder::build);
    }
}
