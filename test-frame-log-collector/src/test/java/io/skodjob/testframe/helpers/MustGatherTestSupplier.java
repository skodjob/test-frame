/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.helpers;

import io.skodjob.testframe.interfaces.MustGatherSupplier;
import org.junit.jupiter.api.extension.ExtensionContext;

public class MustGatherTestSupplier implements MustGatherSupplier {
    @Override
    public void saveKubernetesState(ExtensionContext context) {
        ValueHolder.get().callbackCalled.set(true);
    }
}
