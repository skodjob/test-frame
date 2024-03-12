# TEST-FRAME
Library for easy testing of kubernetes deployments and operators using fabric8 api.

[![Build](https://github.com/skodjob/test-frame/actions/workflows/build.yaml/badge.svg?branch=main)](https://github.com/skodjob/test-frame/actions/workflows/build.yaml)
[![Publish-snapshot](https://github.com/skodjob/test-frame/actions/workflows/publish-snapshot.yaml/badge.svg?branch=main)](https://github.com/skodjob/test-frame/actions/workflows/publish-snapshot.yaml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

## Provided functionality
### Kubernetes resource manager
[KubeResoruceManager](test-frame-common/src/main/java/io/skodjob/testframe/resources/KubeResourceManager.java) provides management of resources created during test phases.
Every kubernetes resource created by `KubeResoruceManager` is automatically deleted in case of test end or test failed.
So kubernetes environment is clean before and after every test run and user do not need to handle it.
Working with kubernetes resources using `KubeResoruceManager` also provides proper waiting for resource readiness.

### Fabric8 kubernetes client and cmd client
Instance of `KubeResoruceManager` contains accessible fabric8 kubernetes client and kubernetes cmd client.
These clients are initialized and connected to the test cluster based on configuration provided by env file, env variables or kubeconfig.

### Test visual separation
For better reading test logs library provides ASCII vial separation of tests and test classes.

### Utils
`TestFrame` contains also tweaks and utils for better working with kubernetes cluster.



## Usage
1. Include dependency to your maven test project
```xml
<dependency>
    <groupId>io.skodjob</groupId>
    <artifactId>test-frame-common</artifactId>
</dependency>
```
2. Use annotations for working with `KubeResoruceManager` or other provided functionality
```java
//...
@ResourceManager
@TestVisualSeparator
class Test {
    //...
}
//...
```
3. Work with `KubeResoruceManager` and clients
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

## Examples
Examples are stored in [test-frame-test](test-frame-test/src/test/java/io/skodjob/testframe/test/integration) module.


## Config environment variables
* **ENV_FILE** - path to yaml file with env variables values
* **KUBE_USERNAME** - user of the cluster
* **KUBE_PASSWORD** - password of kube user
* **KUBE_TOKEN** - token of kube access (use instead of username/password)
* **KUBE_URL** - url of the cluster (api url)