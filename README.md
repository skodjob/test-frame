# TEST-FRAME
Library for easy testing of Kubernetes deployments and operators using Fabric8 API.

[![Build](https://github.com/skodjob/test-frame/actions/workflows/build.yaml/badge.svg?branch=main)](https://github.com/skodjob/test-frame/actions/workflows/build.yaml)
[![Publish-snapshot](https://github.com/skodjob/test-frame/actions/workflows/publish-snapshot.yaml/badge.svg?branch=main)](https://github.com/skodjob/test-frame/actions/workflows/publish-snapshot.yaml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![GitHub Release](https://img.shields.io/github/v/release/skodjob/test-frame)](https://github.com/skodjob/test-frame/releases)
[![Maven Central Version](https://img.shields.io/maven-central/v/io.skodjob/test-frame)](https://central.sonatype.com/search?q=io.skodjob.test-frame)

## Provided functionality
### Kubernetes resource manager
[KubeResourceManager](test-frame-common/src/main/java/io/skodjob/testframe/resources/KubeResourceManager.java) provides management of resources created during test phases.
Every Kubernetes resource created by `KubeResourceManager` is automatically deleted at the end of the test, whether the test passed or failed.
So the Kubernetes environment is clean before and after every test run and user do not need to handle it.
Working with Kubernetes resources using `KubeResourceManager` also provides proper wait for resource readiness.

### Fabric8 Kubernetes client and CMD client
Instance of `KubeResourceManager` contains accessible fabric8 kubernetes client and kubernetes cmd client.
These clients are initialized and connected to the test cluster based on the configuration provided by the env file, env variables, or kubeconfig.

### Test visual separation
For better clarity regarding the test logs, `TestFrame` library provides ASCII vial separation of tests and test classes.

### Metrics Collector
The [`MetricsCollector`](test-frame-metrics-collector/MetricsCollector.md) is designed to facilitate the collection of metrics from Kubernetes pods. 
It integrates seamlessly with Kubernetes environments to gather and process metrics data efficiently. 
For more detailed documentation, see the MetricsCollector [README](test-frame-metrics-collector/README.md).

### Utils
`TestFrame` contains also tweaks and utils for better working with kubernetes cluster.

## Usage
### Include dependency to your maven test project
```xml
<dependency>
    <groupId>io.skodjob</groupId>
    <artifactId>test-frame-common</artifactId>
</dependency>
```
### Use annotations for working with `KubeResourceManager` or other provided functionality
```java
//...
@ResourceManager
@TestVisualSeparator
class Test {
    //...
}
//...
```
### To disable `KubeResourceManager` clean resources functionality
```java
//...
@ResourceManager(cleanResources = false)
@TestVisualSeparator
class Test {
    //...
}
//...
```
### Work with `KubeResourceManager` and clients
```java
//...
@ResourceManager
class Test {
    @Test
    void testMethod() {
        KubeResourceManager.getInstance().createResourceWithWait(
                new NamespaceBuilder().withNewMetadata().withName("test").endMetadata().build());
        assertNotNull(KubeResourceManager.getKubeCmdClient().get("namespace", "test"));
    }
}
//...
```
### Register `ResourceType` or `NamespacedResourceType` classes into `KubeResourceManager`
```java
KubeResourceManager.getInstance().setResourceTypes(
        new NamespaceType(),
        new JobType(),
        new NetworkPolicyType()
);
```

## Examples
Examples are stored in [test-frame-test](test-frame-test/src/test/java/io/skodjob/testframe/test/integration) module.

## Config environment variables
* **ENV_FILE** - path to YAML file with env variables values
* **KUBE_TOKEN** - token of Kube access (use instead of username/password)
* **KUBE_URL** - URL of the cluster (API URL)
* **CLIENT_TYPE** - Switch between `kubectl` or `oc` cmd client

## Adopters
* [opendatahub.io](https://github.com/opendatahub-io/opendatahub-operator) operator test suite - [odh-e2e](https://github.com/skodjob/odh-e2e)

## Maintainers
* [David Kornel](https://github.com/kornys) <kornys@outlook.com>
* [Lukas Kral](https://github.com/im-konge) <lukywill16@gmail.com>
* [Jakub Stejskal](https://github.com/Frawless) <xstejs24@gmail.com>