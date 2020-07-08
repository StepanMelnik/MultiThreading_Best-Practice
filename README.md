# MultiThreading Best Practice
The project contains a few suggestions with examples how to work effectively with multi-threading in java.

## Description

First of all check <a href="https://github.com/StepanMelnik/MultiThreading_Examples">MultiThreading Examples</a> project with a bunch of examples how java works with multi-threading.

When you think to resolve a task by multi-threading, pay attention on the following. 

### Do not use old API

#### Avoid Shared Mutable variables

Every time when a thread changes a variable value we have to consider whether we have to put the change back to the global memory or leave it in the local cache of the thread.
* synchronized keyword lacks granularity, do not support timeout, etc;
* volatile variable access is expensive
* volatile does not support the atomicity features. See <a href="https://github.com/StepanMelnik/MultiThreading_Examples/blob/master/src/test/java/com/sme/multithreading/sharedvariable/VolatileIncrementVariableTest.java">VolatileIncrementVariableTest</a> test.

#### Do not use old API of threads

Do not use old threads API as well:
* thread does not allow to restart;
* thread creating is expensive operation;
* wait() and notify() methods require synchronization;
* synchronized method does not have a solution to time out execution.

### Use new concurrent API

<a href="https://github.com/StepanMelnik/MultiThreading_BestPractice/blob/master/src/test/java/com/sme/multithreading/service/SlowServiceIntegrationTest.java">SlowServiceIntegrationTest</a> test calls a slow service 100 times.

 


### Immutable
TODO

### Example to calculate files on the file system
TODO

## Build

Clone and install <a href="https://github.com/StepanMelnik/Parent.git">Parent</a> project before building.

Clone current project.

### Maven

	> mvn clean test
