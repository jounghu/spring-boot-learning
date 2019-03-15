## 10分钟搞定Spring Boot + Dubbo + Dubbo admin UI一整套分布式解决方案


### 本机环境

`JDK`: 1.8.0_161

`Spring Boot`: 2.1.3.RELEASE

`Zookeeper`: 3.4.11

`org.apache.dubbo`: 2.7.0

`Node`: v8.4.0

`Npm`: 5.3.0


### 开始搭建

### Dubbo-Api

提供给外部使用

新建一个Dubbo-api 供consumer 和 provider使用

仅仅包含一个HelloService

```java
public interface HelloService {
    String sayHello(String name);
}
````


### Dubbo-Provider

pom依赖

```pom
 <properties>
        <java.version>1.8</java.version>
        <spring-boot.version>2.1.3.RELEASE</spring-boot.version>
        <dubbo.version>2.7.0</dubbo.version>
 </properties>
    
 <dependencyManagement>
        <dependencies>
            <!-- Spring Boot -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- Apache Dubbo  -->
            <dependency>
                <groupId>org.apache.dubbo</groupId>
                <artifactId>dubbo-dependencies-bom</artifactId>
                <version>${dubbo.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <dependency>
                <groupId>org.apache.dubbo</groupId>
                <artifactId>dubbo</artifactId>
                <version>${dubbo.version}</version>
                <exclusions>
                    <exclusion>
                        <groupId>org.springframework</groupId>
                        <artifactId>spring</artifactId>
                    </exclusion>
                    <exclusion>
                        <groupId>javax.servlet</groupId>
                        <artifactId>servlet-api</artifactId>
                    </exclusion>
                    <exclusion>
                        <groupId>log4j</groupId>
                        <artifactId>log4j</artifactId>
                    </exclusion>
                </exclusions>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        
        <!-- Dubbo Spring Boot Starter -->
        <dependency>
            <groupId>org.apache.dubbo</groupId>
            <artifactId>dubbo-spring-boot-starter</artifactId>
            <version>2.7.0</version>
        </dependency>

        <dependency>
            <groupId>org.apache.dubbo</groupId>
            <artifactId>dubbo</artifactId>
        </dependency>

        <dependency>
            <groupId>org.apache.curator</groupId>
            <artifactId>curator-framework</artifactId>
        </dependency>

        <dependency>
            <groupId>org.apache.curator</groupId>
            <artifactId>curator-recipes</artifactId>
        </dependency>

        <dependency>
            <groupId>org.apache.zookeeper</groupId>
            <artifactId>zookeeper</artifactId>
            <exclusions>
                <exclusion>
                    <artifactId>log4j</artifactId>
                    <groupId>log4j</groupId>
                </exclusion>
                <exclusion>
                    <artifactId>slf4j-log4j12</artifactId>
                    <groupId>org.slf4j</groupId>
                </exclusion>
            </exclusions>
        </dependency>

        <dependency>
            <groupId>com.101tec</groupId>
            <artifactId>zkclient</artifactId>
        </dependency>

    </dependencies>
```

这些依赖都缺一不可,接下来就实现dubbo-api提供的Service方法即可

```java
import com.skrein.dubbo.api.service.HelloService;
import org.apache.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// 指定dubbo-provider的版本
@Service(version = "${demo.service.version}")
@Component
public class HelloServiceImpl implements HelloService {

    @Value("${server.port}")
    String port;

    @Value("${spring.application.name}")
    String appName;

    @Override
    public String sayHello(String name) {
        return appName + ":" + port + "\n hello " + name;
    }
}
```

`@Service`: 这个注解是org.apache.dubbo提供的

然后`BootApplication`设置@EnableDubbo 即可完成一个最简单的Provider

```java
@SpringBootApplication
@EnableDubbo
public class BootApplication {

    public static void main(String[] args) {
        SpringApplication.run(BootApplication.class, args);
    }

}
```

配置文件配置通过`Zookeeper`作为我们的注册中心:

`application.yml`

```yaml
dubbo:
  registry:
    address: zookeeper://127.0.0.1:2181 # 注册中心地址
  protocol:
    port: -1 # 随机端口号，我们等会会通过不同端口起2台Provider,所以使用随机端口
    name: dubbo
  metadata-report:
    address: zookeeper://127.0.0.1:2181 # dubbo-admin需要这个配置上报meta信息
demo:
  service:
    version: 0.0.1

spring:
  application:
    name: dubbo-provider
  main:
    allow-bean-definition-overriding: true # 不设置会与Spring Boot冲突
```

然后我们通过命令行启动

```bash
$ java -jar target/dubbo-provider-0.0.1.jar --server.port=9000
$ java -jar target/dubbo-provider-0.0.1.jar --server.port=9000
```

### Dubbo-Consumer

pom配置同Dubbo-provider, `Dubbo-Consumer`这里作为一个Web服务

```java
@RestController
public class HelloController {

    @Reference(version = "${demo.service.version}")
    private HelloService helloService;

    @GetMapping("/hello/{name}")
    public String hello(@PathVariable(name = "name") String name) {
        return helloService.sayHello(name);
    }
}
```

启动类加上`@EnableDubbo`即可

`application.yml`:

```yaml
dubbo:
  registry:
    address: zookeeper://127.0.0.1:2181 # Zookeeper注册中心地址
  protocol:
    port: -1 # 随机端口
    name: dubbo # dubbo协议名字


spring:
  main:
    allow-bean-definition-overriding: true # 不设置会与Spring Boot冲突
  application:
    name: dubbo-consumer
demo:
  service:
    version: 0.0.1 # 消费者版本与生产者版本一致
```

`spring.main.allow-bean-definition-overriding=true`: [Spring Boot 2.7.0 Environment Beans conflict in Spring Boot](https://github.com/apache/incubator-dubbo-spring-boot-project/issues/459)

命令行启动:

```bash
$ java -jar target/dubbo-consumer-0.0.1.jar
```

启动之后如下:

![dubbo启动图](http://ww1.sinaimg.cn/large/005RZJcZgy1g12ibefba9j30fs05l3yp.jpg)


### 配置Dubbo-admin Dubbo-admin-ui

#### dubbo-admin-server

官网地址 [Dubb-admin-UI](https://github.com/apache/incubator-dubbo-admin)

把项目下载下来，使用IDEA打开

![Dubbo-admin-ui项目结构](http://ww1.sinaimg.cn/large/005RZJcZgy1g12ie8a8poj30ho0j1wg7.jpg)

首先启动`dubbo-admin-server`,注意这里的端口号，可能会与`dubbo-consumer`冲突，我们这里修改

`dubbo-admin-server`: 把端口号修改为`8888`

**--server.port=8888**

然后启动就行了。

#### dubbo-admin-ui

前端使用Vue,所以需要Node环境

推荐一个我常用的node管理： `NVM`

NVM-windows: https://github.com/coreybutler/nvm-windows

安装完毕

```bash
$ nvm install 8.4.0
$ nvm use 8.4.0
```

nvm 安装node如下:

![](http://ww1.sinaimg.cn/large/005RZJcZgy1g137rc2k1cj30j005r0t2.jpg)


然后切换到dubbo-admin-ui这个目录下:

![dubbo-admin-ui directory](http://ww1.sinaimg.cn/large/005RZJcZgy1g137wcnm09j30tk024wel.jpg)

执行 `npm install`

```bash
$ npm install
```

我本机已经执行过，没有执行过的等待安装:

![cnpm install](http://ww1.sinaimg.cn/large/005RZJcZgy1g137xap4rfj30nr034q39.jpg)

你们还记得，`admin-server`的端口号为 `8888` ,因此我们需要把`dubbo-admin-ui`连接server的端口号也要修改(默认是`8080`)

修改 `config/index.js`

![modify port](http://ww1.sinaimg.cn/large/005RZJcZgy1g1381hez50j30lq08zq3c.jpg)


然后启动`dubbo-admin-ui`:

```bash
$ cnpm run dev
```

![run success](http://ww1.sinaimg.cn/large/005RZJcZgy1g1383f7zfgj30gk03974a.jpg)

访问 `http://localhost:8081`

![dubbo-admin-ui index](http://ww1.sinaimg.cn/large/005RZJcZgy1g1384qb430j31hb0drdhk.jpg)

可以看到我们的服务已经被admin监控了。   

点击详情,也能看到我们的2个provider也是在监控当中的：

![](http://ww1.sinaimg.cn/large/005RZJcZgy1g1385xtem9j31ha0pkmzv.jpg)




---

代码已经上传至我的[github](https://github.com/jounghu/spring-boot-learning),欢迎star,后续会持续更新，为你带来分布式，高并发等等详细例子。