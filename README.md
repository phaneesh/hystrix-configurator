# Hystrix Configurator [![Travis build status](https://travis-ci.org/phaneesh/hystrix-configurator.svg?branch=master)](https://travis-ci.org/phaneesh/hystrix-configurator)

This is a simple helper library to configure [Hystrix](https://github.com/Netflix/Hystrix) 
with ease without having to write plumbing code.
This library compiles only on Java 8.
 
## Dependencies
* Hystrix 1.5.3  

## Usage
Configuring Hystrix environment requires one to write a lot of plumbing code. Using this helper library it is very easy to
feed in configuration into Hystrix runtime
### Build instructions
  - Clone the source:

        git clone github.com/phaneesh/hystrix-configurator

  - Build

        mvn install

### Maven Dependency
Use the following repository:
```xml
<repository>
    <id>clojars</id>
    <name>Clojars repository</name>
    <url>https://clojars.org/repo</url>
</repository>
```
Use the following maven dependency:
```xml
<dependency>
    <groupId>com.hystrix</groupId>
    <artifactId>hystrix-configurator</artifactId>
    <version>0.0.6</version>
</dependency>
```

### Using Hystrix Configurator (With setter caching)
```java

       HystrixConfigurationFactory.init(
        HystrixConfig.builder()
                .defaultConfig(HystrixDefaultConfig.builder().build())
                .command(HystrixCommandConfig.builder().name("test").build())
                .build());
       
       SimpleTestCommand command = new SimpleTestCommand();
       String result = command.queue().get();
       public class SimpleTestCommand extends BaseCommand<String> {
   
           public SimpleTestCommand() {
               super("test");
           }
   
           @Override
           protected String run() throws Exception {
               return "Simple Test";
           }
       }
```

### Using Hystrix Configurator (With ConfigurationManager)
```java

       HystrixConfigurationFactory.init(
        HystrixConfig.builder()
                .defaultConfig(HystrixDefaultConfig.builder().build())
                .command(HystrixCommandConfig.builder().name("test").build())
                .build());
       
       SimpleTestCommand command = new SimpleTestCommand();
       String result = command.queue().get();
       public class SimpleTestCommand extends HystrixCommand<String> {
   
           public SimpleTestCommand() {
               super(HystrixCommandGroupKey.Factory.asKey("test"));
           }
   
           @Override
           protected String run() throws Exception {
               return "Simple Test";
           }
       }
```

LICENSE
-------

Copyright 2016 Phaneesh Nagaraja <phaneesh.n@gmail.com>.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.