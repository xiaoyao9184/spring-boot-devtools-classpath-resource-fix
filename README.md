# spring-boot-devtools-classpath-resource-fix

[![](https://jitpack.io/v/xiaoyao9184/spring-boot-devtools-classpath-resource-fix.svg)](https://jitpack.io/#xiaoyao9184/spring-boot-devtools-classpath-resource-fix)


# When

If you using Idea for Windows with classpath jar enabled and running Spring Boot project.
You maybe encountered the resource jars loaded problem, such as using JSPs in jars and not finding jsp files.

The reason is as follows:
- [Devtools doesn't work with classpath file mode in Idea](https://github.com/spring-projects/spring-boot/issues/5127)
- [Resource jars not found when loaded via classpath in manifest](https://github.com/spring-projects/spring-boot/issues/9513)
- [All absolute URLs in Class-Path manifest attribute are ignored by DevTools](https://github.com/spring-projects/spring-boot/issues/10268)


# Use

Use this with `spring-boot-devtools` together like this:
```groovy
dependencies {
    runtime 'com.github.xiaoyao9184:spring-boot-devtools-classpath-resource-fix:master-SNAPSHOT'
    runtime 'org.springframework.boot:spring-boot-devtools:1.5.1.RELEASE'
}
```

