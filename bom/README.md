# light-4j-bom

# Goals
A BOM file is useful for forcing transitive dependencies and dependencies without a version to
adhere to the versions defined in the BOM. It is a useful tool for projects that depend on
light-4j because it prevents them from accidentally mixing different versions of the light-4j
modules. There is no guarantee that the light-4j modules will be compatible if they are of
different versions.

# Usage

## Maven
It's recommended that the dependency management section from our BOM files are imported, but they 
can also be used as parent POM files.

```xml
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.networknt</groupId>
    <artifactId>some-project</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
	
    <properties>
        <version.light>2.0.8-SNAPSHOT</version.light>
	</properties>
	
	<dependencyManagement>
        <dependencies>
            <!-- This is how to override dependency versions imported from the BOM. -->
            <dependency>
                <groupId>com.networknt</groupId>
                <artifactId>audit</artifactId>
                <version>1.6.0-SNAPSHOT</version>
			</dependency>
			<!-- Import dependency management from Light 4j BOM. -->
            <dependency>
                <groupId>com.networknt</groupId>
                <artifactId>light-4j-bom</artifactId>
                <version>${version.light}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
	
	<!-- Versions should be omitted so that the dependency management section handles them. -->
	<dependencies>
        <dependency>
            <groupId>com.networknt</groupId>
            <artifactId>audit</artifactId>
        </dependency>
        <dependency>
            <groupId>com.networknt</groupId>
            <artifactId>config</artifactId>
        </dependency>
	</dependencies>
</project>
```

## Gradle
Gradle partially supports importing BOM files now, but at the time of investigation (around the 
release of Gradle 5.0) behavior was different than that of Maven BOM imports. It is recommended 
to utilize the io.spring.dependency-management plugin for importing the BOM because it was 
designed to follow the same behavior as Maven BOM imports.

```groovy
buildscript {
  repositories {
    jcenter()
  }
  dependencies {
    classpath "io.spring.gradle:dependency-management-plugin:0.5.1.RELEASE"
  }
}

apply plugin: "io.spring.dependency-management"

dependencyManagement {
  // This is how to override dependency versions imported from the BOM.
  dependencies {
    dependency 'com.networknt:audit:1.6.0-SNAPSHOT'
  }
	
  imports {
    mavenBom 'com.networknt:light-4j-bom:2.0.8-SNAPSHOT'
  }
}

dependencies {
    // Versions should be omitted so that they handled by the resolution 
	// strategies setup by the dependency management plugin.
    compile 'com.networknt:audit'
    compile 'com.networknt:config'
}
```