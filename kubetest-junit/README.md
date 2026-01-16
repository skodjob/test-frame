# KubeTest JUnit Extensions

This module provides declarative JUnit extensions for the Test Frame Kubernetes testing library, making it easier to write and manage Kubernetes tests.

## Features

- **Declarative test setup** with annotations
- **Automatic resource management** and cleanup
- **Dependency injection** for Kubernetes clients and resources
- **Namespace lifecycle management**
- **Resource injection from YAML files**
- **Multiple cleanup strategies**
- **Context switching support**

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

### 2. Write Your First Test

```java
@KubernetesTest(namespace = "my-test")
class MyKubernetesTest {

    @InjectKubeClient
    KubeClient client;

    @InjectResourceManager
    KubeResourceManager resourceManager;

    @InjectNamespace
    String namespaceName;

    @Test
    void testPodCreation() {
        Pod pod = new PodBuilder()
            .withNewMetadata()
            .withName("test-pod")
            .withNamespace(namespaceName)
            .endMetadata()
            .withNewSpec()
            .addNewContainer()
            .withName("test")
            .withImage("busybox:latest")
            .withCommand("sleep", "300")
            .endContainer()
            .endSpec()
            .build();

        resourceManager.createResourceWithWait(pod);

        // Pod is automatically cleaned up after test
        assertNotNull(client.getClient().pods()
            .inNamespace(namespaceName)
            .withName("test-pod")
            .get());
    }
}
```

## Annotations

### @KubernetesTest

Main annotation to enable Kubernetes test features:

```java
@KubernetesTest(
    namespace = "my-test-ns",           // Namespace to use (auto-generated if empty)
    createNamespace = true,             // Whether to create the namespace
    cleanup = CleanupStrategy.AFTER_EACH, // When to clean up resources
    context = "prod",                   // Kubernetes context to use
    storeYaml = true,                   // Store YAML files of created resources
    yamlStorePath = "target/test-yamls", // Where to store YAML files
    namespaceLabels = {"env=test"},     // Labels to apply to namespace
    namespaceAnnotations = {"description=Test namespace"}, // Annotations for namespace
    visualSeparatorChar = "#",          // Character for visual test separators
    visualSeparatorLength = 76          // Length of visual separator lines
)
```

### @InjectKubeClient

Inject a configured KubeClient:

```java
@InjectKubeClient
KubeClient client;

@Test
void testWithClient(@InjectKubeClient KubeClient client) {
    // Both field and parameter injection work
}
```

### @InjectResourceManager

Inject the KubeResourceManager:

```java
@InjectResourceManager
KubeResourceManager resourceManager;
```

### @InjectNamespace

Inject namespace information:

```java
@InjectNamespace
String namespaceName;    // Namespace name as String

@InjectNamespace
Namespace namespace;     // Full Namespace object
```

### @InjectResource

Inject resources from YAML files:

```java
@InjectResource("deployment.yaml")
Deployment deployment;

@InjectResource(value = "service.yaml", waitForReady = true)
Service service;
```

## Cleanup Strategies

Control when resources are cleaned up:

- `CleanupStrategy.AFTER_EACH` - Clean up after each test method (default)
- `CleanupStrategy.AFTER_ALL` - Clean up after all tests in the class
- `CleanupStrategy.ON_FAILURE` - Clean up only if tests fail
- `CleanupStrategy.MANUAL` - No automatic cleanup

## Resource Injection

Load and apply resources from YAML files automatically:

```yaml
# test-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: test-app
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
@KubernetesTest
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
@KubernetesTest(context = "staging")
class StagingTest {
    // Tests run against staging cluster
}

@KubernetesTest(context = "prod")
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
# or
KUBECONFIG_STAGING=/path/to/staging.kubeconfig

# Production context
KUBE_URL_PROD=https://api.prod:6443
KUBE_TOKEN_PROD=prod-token
```

## Visual Test Separators

The framework automatically provides visual separators to make test output more readable:

```
############################################################################
TestClass io.example.MyKubernetesTest STARTED
Setting up Kubernetes test environment for class: MyKubernetesTest
...
############################################################################
Test io.example.MyKubernetesTest.testMethod STARTED
...
Test io.example.MyKubernetesTest.testMethod SUCCEEDED
############################################################################
TestClass io.example.MyKubernetesTest FINISHED
############################################################################
```

### Custom Visual Separators

You can customize the separator character and length:

```java
@KubernetesTest(
    visualSeparatorChar = "=",
    visualSeparatorLength = 60
)
class MyTest {
    // Test output will use ============================================================
}
```

## Advanced Features

### Custom Namespace Configuration

```java
@KubernetesTest(
    createNamespace = true,
    namespaceLabels = {
        "environment=test",
        "team=backend",
        "cost-center=engineering"
    },
    namespaceAnnotations = {
        "description=Integration tests for user service",
        "contact=backend-team@company.com"
    }
)
```

### YAML Template Variables

```java
@InjectResource(
    value = "configmap-template.yaml",
    templateVariables = {"ENV=test", "VERSION=1.0.0"}
)
ConfigMap config;
```

### Parameter Injection

All injection annotations work with test method parameters:

```java
@Test
void testWithInjection(
    @InjectKubeClient KubeClient client,
    @InjectResourceManager KubeResourceManager resourceManager,
    @InjectNamespace String namespace,
    @InjectResource("deployment.yaml") Deployment deployment) {

    // All parameters are automatically injected
}
```

## Migration from Raw Framework

Converting from the raw framework is straightforward:

### Before (Raw Framework):
```java
class MyTest {
    private KubeResourceManager resourceManager;

    @BeforeEach
    void setup() {
        resourceManager = KubeResourceManager.get();
        resourceManager.setTestContext(/* ... */);
        // Create namespace manually
        // Configure clients manually
    }

    @AfterEach
    void cleanup() {
        resourceManager.deleteResources();
    }

    @Test
    void test() {
        KubeClient client = resourceManager.kubeClient();
        // ... test code
    }
}
```

### After (JUnit 5 Extensions):
```java
@KubernetesTest
class MyTest {
    @InjectKubeClient
    KubeClient client;

    @InjectResourceManager
    KubeResourceManager resourceManager;

    @Test
    void test() {
        // Everything is automatically injected and configured
        // Cleanup is automatic
    }
}
```

## Examples

See the `examples` package for comprehensive examples:

- [`BasicKubernetesTest`](src/test/java/io/skodjob/testframe/junit/examples/BasicKubernetesTest.java) - Basic usage and injection
- [`ResourceInjectionTest`](src/test/java/io/skodjob/testframe/junit/examples/ResourceInjectionTest.java) - YAML resource injection
- [`AdvancedKubernetesTest`](src/test/java/io/skodjob/testframe/junit/examples/AdvancedKubernetesTest.java) - Advanced features and manual cleanup

## Benefits

- **Reduced Boilerplate**: No need for manual setup/teardown
- **Declarative**: Test configuration is clear and visible
- **Type Safety**: Strong typing for all injected dependencies
- **Resource Management**: Automatic cleanup with configurable strategies
- **IDE Support**: Full IDE support with code completion and validation
- **Test Isolation**: Each test gets its own namespace and resources
- **Parallel Execution**: Tests can run in parallel with proper isolation