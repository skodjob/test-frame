# KubeTest JUnit Extensions

A comprehensive JUnit 5 extension for Kubernetes testing that provides declarative test configuration, multi-namespace support, comprehensive log collection, and automatic resource management.

## Features

- **Multi-Namespace Support** - Create and test across multiple namespaces
- **Comprehensive Log Collection** - Automatic log collection with multiple strategies and exception handling
- **Dependency Injection** - Inject Kubernetes clients, resource managers, and resources
- **Automatic Resource Management** - Configurable cleanup strategies and lifecycle management
- **YAML Resource Loading** - Load and inject resources from YAML files
- **Exception-Based Log Collection** - Capture failures from ANY test lifecycle phase
- **Context Switching** - Test against different Kubernetes clusters
- **Visual Test Output** - Enhanced test logging with visual separators

## Quick Start

### 1. Add Dependency

Add the JUnit 5 extension dependency to your project:

```xml
<dependency>
    <groupId>io.skodjob</groupId>
    <artifactId>kubetest-junit</artifactId>
    <version>1.4.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```

### 2. Basic Test Example

```java
@KubernetesTest(namespaces = {"my-test"})
class MyKubernetesTest {

    @InjectKubeClient
    KubeClient client;

    @InjectResourceManager
    KubeResourceManager resourceManager;

    @Test
    void testPodCreation() {
        Pod pod = new PodBuilder()
            .withNewMetadata()
            .withName("test-pod")
            .withNamespace("my-test")  // Must specify namespace explicitly
            .endMetadata()
            .withNewSpec()
            .addNewContainer()
            .withName("test")
            .withImage("quay.io/prometheus/busybox")
            .withCommand("sleep", "300")
            .endContainer()
            .endSpec()
            .build();

        resourceManager.createResourceWithWait(pod);

        // Pod is automatically cleaned up after test
        assertNotNull(client.getClient().pods()
            .inNamespace("my-test")
            .withName("test-pod")
            .get());
    }
}
```

##️ Multi-Namespace Testing

Create and manage multiple namespaces for complex test scenarios. **Existing namespaces are automatically protected** - only namespaces created by the test will be deleted during cleanup:

```java
@KubernetesTest(
    namespaces = {"frontend", "backend", "monitoring"},
    createNamespaces = true,
    cleanup = CleanupStrategy.AUTOMATIC
)
class MultiNamespaceTest {

    @Test
    void testCrossNamespaceNetworking() {
        // Create service in backend namespace
        Service backendService = new ServiceBuilder()
            .withNewMetadata()
            .withName("api-service")
            .withNamespace("backend")  // Explicit namespace required
            .endMetadata()
            .withNewSpec()
            .withSelector(Map.of("app", "backend"))
            .addNewPort()
            .withPort(8080)
            .withTargetPort(new IntOrStringBuilder().withValue(8080).build())
            .endPort()
            .endSpec()
            .build();

        resourceManager.createResourceWithWait(backendService);

        // Test connectivity from frontend namespace
        // All three namespaces are automatically created and available
    }
}
```

## Comprehensive Log Collection

### Automatic Log Collection with Exception Handlers

The framework uses exception handlers to capture failures from **ANY** test lifecycle phase:

- `@BeforeAll` method failures
- `@BeforeEach` method failures
- Test execution failures
- `@AfterEach` method failures
- `@AfterAll` method failures

```java
@KubernetesTest(
    namespaces = {"log-test"},
    collectLogs = true,
    logCollectionStrategy = LogCollectionStrategy.ON_FAILURE,
    logCollectionPath = "target/test-logs",
    collectPreviousLogs = true,
    collectNamespacedResources = {"pods", "services", "configmaps", "secrets"},
    collectClusterWideResources = {"nodes"},
    collectEvents = true
)
class LogCollectionTest {

    @Test
    void testThatFails() {
        // If this test fails, logs will be automatically collected
        // from all configured namespaces and resources
        throw new AssertionError("Demonstrating automatic log collection");
    }
}
```

### Log Collection Strategies

- **`ON_FAILURE`** - Collect logs only when tests fail (default)
- **`AFTER_EACH`** - Collect logs after each test method (success or failure)
- **`NEVER`** - Disable automatic log collection

### Configurable Log Collection

```java
@KubernetesTest(
    namespaces = {"comprehensive-test"},

    // ===== Log Collection Configuration =====
    collectLogs = true,
    logCollectionStrategy = LogCollectionStrategy.AFTER_EACH,
    logCollectionPath = "target/test-logs/comprehensive",
    collectPreviousLogs = true,  // Include previous container logs

    // Specify which namespaced resources to collect
    collectNamespacedResources = {
        "pods", "services", "configmaps", "secrets",
        "deployments", "replicasets", "daemonsets"
    },

    // Specify cluster-wide resources to collect
    collectClusterWideResources = {"nodes", "persistentvolumes"},

    collectEvents = true  // Include Kubernetes events
)
```

## Annotations Reference

### @KubernetesTest

Main annotation to enable Kubernetes test features:

```java
@KubernetesTest(
    // Multi-namespace support
    namespaces = {"app", "monitoring"},     // Namespaces to create/use
    createNamespaces = true,                // Auto-create namespaces

    // Resource management
    cleanup = CleanupStrategy.AUTOMATIC,  // Cleanup strategy
    context = "staging",                    // Kubernetes context
    storeYaml = true,                      // Store resource YAMLs
    yamlStorePath = "target/yamls",        // YAML storage path

    // Namespace metadata
    namespaceLabels = {"env=test", "team=backend"},
    namespaceAnnotations = {"description=Test namespace"},

    // Visual output
    visualSeparatorChar = "#",             // Separator character
    visualSeparatorLength = 76,            // Separator length

    // ===== Log Collection =====
    collectLogs = true,
    logCollectionStrategy = LogCollectionStrategy.ON_FAILURE,
    logCollectionPath = "target/test-logs",
    collectPreviousLogs = true,
    collectNamespacedResources = {"pods", "services"},
    collectClusterWideResources = {"nodes"},
    collectEvents = true
)
```

### @InjectKubeClient

Inject a configured Kubernetes client:

```java
@InjectKubeClient
KubeClient client;

@Test
void testWithClient(@InjectKubeClient KubeClient client) {
    // Both field and parameter injection work
}
```

### @InjectCmdKubeClient

Inject a command-line kubectl client:

```java
@InjectCmdKubeClient
KubeCmdClient<?> cmdClient;

@Test
void testWithCmdClient() {
    cmdClient.exec("get", "pods", "-n", "my-namespace");
}
```

### @InjectResourceManager

Inject the resource manager for lifecycle management:

```java
@InjectResourceManager
KubeResourceManager resourceManager;

@Test
void testResourceLifecycle() {
    resourceManager.createResourceWithWait(myResource);
    // Automatic cleanup based on configured strategy
}
```

### @InjectNamespaces

Inject a Map of Namespace objects corresponding to the namespaces defined in @KubernetesTest (key=namespace name, value=Namespace object):

```java
@InjectNamespaces
Map<String, Namespace> namespaces;

@Test
void testNamespaces(@InjectNamespaces Map<String, Namespace> paramNamespaces) {
    // Both field and parameter injection work
    assertEquals(2, namespaces.size());
    assertTrue(namespaces.containsKey("app"));
    assertTrue(namespaces.containsKey("monitoring"));
    assertEquals("app", namespaces.get("app").getMetadata().getName());
    assertEquals("monitoring", namespaces.get("monitoring").getMetadata().getName());
}
```

### @InjectNamespace

Inject a specific Namespace object by name:

```java
@InjectNamespace(name = "app")
Namespace appNamespace;

@InjectNamespace(name = "monitoring")
Namespace monitoringNamespace;

@Test
void testSpecificNamespace(@InjectNamespace(name = "app") Namespace paramAppNamespace) {
    // Both field and parameter injection work
    assertEquals("app", appNamespace.getMetadata().getName());
    assertEquals("monitoring", monitoringNamespace.getMetadata().getName());
    assertEquals("app", paramAppNamespace.getMetadata().getName());
}
```

### @InjectResource

Load and inject resources from YAML files:

```java
@InjectResource("deployment.yaml")
Deployment deployment;

@InjectResource(value = "service.yaml", waitForReady = true)
Service service;
```

## Cleanup Strategies

Control when resources are cleaned up:

- **`AUTOMATIC`** - Automatically clean up resources using KubeResourceManager (default)
  - Resources are cleaned up both after each test method and after all tests complete
  - Uses async deletion for better performance
  - Covers failure scenarios as well
- **`MANUAL`** - No automatic cleanup, manual resource management required

```java
@KubernetesTest(
    namespaces = {"cleanup-test"},
    cleanup = CleanupStrategy.MANUAL  // No automatic cleanup
)
```

## Resource Injection from YAML

Load and apply resources from YAML files automatically:

```yaml
# test-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: test-app
  namespace: my-test  # Namespace must be specified!
spec:
  replicas: 2
  selector:
    matchLabels:
      app: test-app
  template:
    metadata:
      labels:
        app: test-app
    spec:
      containers:
      - name: app
        image: nginx:latest
```

```java
@KubernetesTest(namespaces = {"my-test"})
class ResourceTest {

    @InjectResource("test-deployment.yaml")
    Deployment deployment; // Automatically loaded and applied

    @Test
    void testDeployment() {
        assertEquals(2, deployment.getSpec().getReplicas());
        // Deployment is already running in the cluster
    }
}
```

## Multi-Context Testing

Test against different Kubernetes clusters:

```java
@KubernetesTest(
    namespaces = {"staging-test"},
    context = "staging"
)
class StagingTest {
    // Tests run against staging cluster
}

@KubernetesTest(
    namespaces = {"prod-test"},
    context = "prod"
)
class ProductionTest {
    // Tests run against production cluster
}
```

Configure contexts using environment variables:
```bash
# Default context
KUBE_URL=https://api.default:6443
KUBE_TOKEN=default-token
# or
KUBECONFIG=/path/to/default.kubeconfig

# Staging context
KUBE_URL_STAGING=https://api.staging:6443
KUBE_TOKEN_STAGING=staging-token

# Production context
KUBE_URL_PROD=https://api.prod:6443
KUBE_TOKEN_PROD=prod-token
```

## Visual Test Output

The framework provides visual separators for enhanced readability:

```
############################################################################
TestClass io.example.MyKubernetesTest STARTED
Setting up Kubernetes test environment for class: MyKubernetesTest
Using namespaces: frontend, backend, monitoring
############################################################################
Test io.example.MyKubernetesTest.testMethod STARTED
...
Test io.example.MyKubernetesTest.testMethod SUCCEEDED
############################################################################
TestClass io.example.MyKubernetesTest FINISHED
############################################################################
```

Customize separators:
```java
@KubernetesTest(
    visualSeparatorChar = "=",
    visualSeparatorLength = 60
)
```

## Advanced Features

### Complex Namespace Configuration

```java
@KubernetesTest(
    namespaces = {"app-frontend", "app-backend", "monitoring"},
    createNamespaces = true,
    namespaceLabels = {
        "environment=test",
        "team=backend",
        "cost-center=engineering"
    },
    namespaceAnnotations = {
        "description=Integration tests for microservices",
        "contact=backend-team@company.com"
    }
)
```

### Per-Class vs Per-Method Lifecycle

```java
@TestInstance(TestInstance.Lifecycle.PER_CLASS)  // Important for @BeforeAll injection
@KubernetesTest(
    namespaces = {"shared-resources"},
    cleanup = CleanupStrategy.AUTOMATIC
)
class SharedResourceTest {

    @InjectResourceManager
    KubeResourceManager resourceManager;

    @BeforeAll
    void setupSharedResources() {
        // resourceManager is properly injected for PER_CLASS lifecycle
        ConfigMap sharedConfig = new ConfigMapBuilder()
            .withNewMetadata()
            .withName("shared-config")
            .withNamespace("shared-resources")
            .endMetadata()
            .addToData("shared.key", "shared.value")
            .build();

        resourceManager.createResourceWithWait(sharedConfig);
    }
}
```

### Namespace Injection Example

```java
@KubernetesTest(
    namespaces = {"app", "monitoring"},
    createNamespaces = true,
    namespaceLabels = {"environment=test", "team=backend"}
)
class NamespaceTest {

    @InjectNamespaces
    Map<String, Namespace> namespaces;

    @Test
    void testNamespaceAccess() {
        assertEquals(2, namespaces.size());

        // Access namespace metadata
        assertEquals("app", namespaces.get("app").getMetadata().getName());
        assertEquals("test", namespaces.get("app").getMetadata().getLabels().get("environment"));

        assertEquals("monitoring", namespaces.get("monitoring").getMetadata().getName());
        assertEquals("backend", namespaces.get("monitoring").getMetadata().getLabels().get("team"));
    }
}
```

### Parameter Injection

All injection annotations work with test method parameters:

```java
@Test
void testWithInjection(
    @InjectKubeClient KubeClient client,
    @InjectResourceManager KubeResourceManager resourceManager,
    @InjectCmdKubeClient KubeCmdClient<?> cmdClient,
    @InjectNamespaces Map<String, Namespace> namespaces,
    @InjectNamespace(name = "app") Namespace appNamespace,
    @InjectResource("deployment.yaml") Deployment deployment) {

    // All parameters are automatically injected
    assertNotNull(client);
    assertNotNull(resourceManager);
    assertNotNull(cmdClient);
    assertNotNull(namespaces);
    assertNotNull(appNamespace);
    assertNotNull(deployment);
}
```

## Examples

See comprehensive examples in the test directory:

- **[`BasicKubernetesTest`](src/test/java/io/skodjob/testframe/kubetest/examples/BasicKubernetesTest.java)** - Basic usage, injection, and resource creation
- **[`MultiNamespaceTest`](src/test/java/io/skodjob/testframe/kubetest/examples/MultiNamespaceTest.java)** - Multi-namespace testing patterns
- **[`NamespaceInjectionTest`](src/test/java/io/skodjob/testframe/kubetest/examples/NamespaceInjectionTest.java)** - Map-based namespace injection with @InjectNamespaces
- **[`NamespaceProtectionTest`](src/test/java/io/skodjob/testframe/kubetest/examples/NamespaceProtectionTest.java)** - Namespace protection and safe cleanup demonstration
- **[`AdvancedKubernetesTest`](src/test/java/io/skodjob/testframe/kubetest/examples/AdvancedKubernetesTest.java)** - Advanced features, PER_CLASS lifecycle, manual cleanup
- **[`ResourceInjectionTest`](src/test/java/io/skodjob/testframe/kubetest/examples/ResourceInjectionTest.java)** - YAML resource injection patterns
- **[`LogCollectionTest`](src/test/java/io/skodjob/testframe/kubetest/examples/LogCollectionTest.java)** - Log collection with AFTER_EACH strategy

## ⚠️ Important Notes

### Namespace Protection and Management

The framework **automatically protects existing namespaces** from deletion:

- ✅ **Existing namespaces**: If a namespace already exists, it will be used as-is and **never deleted**
- ✅ **Created namespaces**: Only namespaces created by the test will be deleted during cleanup
- ✅ **Mixed scenarios**: You can safely mix existing namespaces (like `default`, `kube-system`) with test-created ones

```java
@KubernetesTest(
    namespaces = {"default", "my-test-ns", "existing-ns"}, // Mix of existing and new
    createNamespaces = true
)
class SafeNamespaceTest {
    // 'default' and 'existing-ns' will be protected from deletion
    // Only 'my-test-ns' will be deleted if it was created by the test
}
```

### Namespace Specification Required

**All resources must explicitly specify their namespace** in the metadata. The framework does not inject or default namespaces:

```java
// ✅ CORRECT - Namespace explicitly specified
ConfigMap config = new ConfigMapBuilder()
    .withNewMetadata()
    .withName("config")
    .withNamespace("my-test")  // Required!
    .endMetadata()
    .build();

// ❌ INCORRECT - No namespace specified
ConfigMap config = new ConfigMapBuilder()
    .withNewMetadata()
    .withName("config")
    // Missing namespace - will fail or use unexpected namespace
    .endMetadata()
    .build();
```

### YAML Resources Must Include Namespace

```yaml
# ✅ CORRECT
apiVersion: v1
kind: ConfigMap
metadata:
  name: my-config
  namespace: my-test  # Required!
data:
  key: value

# ❌ INCORRECT - Missing namespace
apiVersion: v1
kind: ConfigMap
metadata:
  name: my-config
  # No namespace specified
data:
  key: value
```
