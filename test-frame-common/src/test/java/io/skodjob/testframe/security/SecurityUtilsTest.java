/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.security;

import io.skodjob.testframe.utils.SecurityUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SecurityUtilsTest {
    static final String ROOT_CA = "C=COM, L=Boston, O=Example, CN=ExampleRootCA";
    static final String INTERMEDIATE_CA = "C=COM, L=Boston, O=Example, CN=ExampleIntermediateCA";
    static final String END_SUBJECT = "C=COM, L=Boston, O=Example, CN=end-app.example.io";
    static final String APP_SUBJECT = "C=COM, L=Boston, O=Example, CN=app.example.io";

    CertAndKey ca;
    CertAndKey intermediateCa;
    CertAndKey appCert;
    CertAndKey endAppCert;

    @BeforeAll
    void setup() {
        ca = CertAndKeyBuilder.rootCaCertBuilder()
            .withIssuerDn(ROOT_CA)
            .withSubjectDn(ROOT_CA)
            .build();

        intermediateCa = CertAndKeyBuilder.intermediateCaCertBuilder(ca)
            .withIssuerDn(INTERMEDIATE_CA)
            .withSubjectDn(INTERMEDIATE_CA)
            .build();

        appCert = CertAndKeyBuilder.appCaCertBuilder(ca)
            .withSubjectDn(APP_SUBJECT)
            .build();

        endAppCert = CertAndKeyBuilder.endEntityCertBuilder(intermediateCa)
            .withSubjectDn(END_SUBJECT)
            .withSanDnsName("*.example.io")
            .build();
    }

    @Test
    void testExportCertsToPem() throws IOException {
        CertAndKeyFiles all = SecurityUtils.exportToPemFiles(ca, intermediateCa, appCert);

        String content = Files.readString(Paths.get(all.getCertPath()));
        assertNotEquals("", content);
    }

    @Test
    void testExportDataToCa() throws IOException {
        File caCert = SecurityUtils.exportCaDataToFile(ca.getPublicKey().toString(), "ca", ".crt");

        String content = Files.readString(caCert.toPath());
        assertNotEquals("", content);
    }
}
