---
date: 2017-01-07T08:03:33-05:00
title: Running Light-4J Application as Windows Service
---

Light-4J is packaged as a fatjar and normally will be running in docker container
on production; however, it can be executed on Windows or Linux host machine or VM
with the following command.

```
java -jar filename.jar
```

For production deployment, you want to make sure that when you host server/VM
is restarted, the application will be restarted automatically. 


The following describes step-by-step how you can create a Windows service for your 
Light-4J application or any Java application that can be packaged in a fatjar.

There are two different options but the first option is more generic and popular.

## Windows Service Wrapper

Due to difficulties with the GPL license of the Java Service Wrapper 
(the second option below) in combination with e.g. the MIT license of Jenkins, 
the Windows Service Wrapper project, also known as [winsw](https://github.com/kohsuke/winsw), 
is the best choice.

Winsw provides programmatic means to install/uninstall/start/stop a service. In 
addition, it may be used to run any kind of executable as a service under Windows, 
whereas Java Service Wrapper, as implied by its name, only supports Java applications.

First, you download the binaries [here](http://repo.jenkins-ci.org/releases/com/sun/winsw/winsw/).

Next, the configuration file that defines our Windows service, MyApp.xml, should 
look like this:

```
<service>
    <id>MyApp</id>
    <name>MyApp</name>
    <description>This runs Spring Boot as a Service.</description>
    <env name="MYAPP_HOME" value="%BASE%"/>
    <executable>java</executable>
    <arguments>-Xmx256m -jar "%BASE%\MyApp.jar"</arguments>
    <logmode>rotate</logmode>
</service>

```
Finally, you have to rename the winsw.exe to MyApp.exe so that its name matches 
with the MyApp.xml configuration file. Thereafter you can install the service 
like so:

```
$ MyApp.exe install
```
Similarly, you may use uninstall, start, stop, etc.

## Java Service Wrapper

In case you don’t mind the GPL licensing of the [Java Service Wrapper](http://wrapper.tanukisoftware.com/doc/english/index.html) 
project, this alternative may address your needs to configure your JAR file as 
a Windows service equally well. Basically, the Java Service Wrapper also requires 
you to specify in a configuration file which specifies how to run your process 
as a service under Windows.

This [article](http://edn.embarcadero.com/article/32068) explains in a very 
detailed way how to set up such an execution of a JAR file as a service under 
Windows.

