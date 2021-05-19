# Frebel--让你修改的JAVA代码实时生效  

![Frebel](https://img.shields.io/github/issues/hengyoush/Frebel) 
![Frebel](https://img.shields.io/github/forks/hengyoush/Frebel)
![Frebel](https://img.shields.io/github/stars/hengyoush/Frebel)
![Frebel](https://img.shields.io/github/license/hengyoush/Frebel)
          
Frebel是一个可以让你修改的JAVA代码实时生效的插件，它有如下特点：

- 轻量化：你只需要在启动命令中加入一行配置即可使用。
- 无侵入性：不需要修改你的任何代码，开箱即用。
- 功能丰富：支持方法新增删除、类新增删除、类继承关系的改变等原生JVM不支持的操作。

## 快速开始
1. 下载Jar包
2. 在启动命令中加入：
    ```shell script
    -javaagent:/path/to/frebel-core-1.0.0.jar
    ```
3. 修改你的代码并且享受实时生效的乐趣！

## 目前已支持的场景
1. 支持方法体内容修改实时生效
2. 支持方法参数类型、数目修改实时生效
3. 支持方法名称修改实时生效
4. 支持方法返回值类型修改实时生效
5. 支持类字段修改实时生效
6. 支持对象状态继承,即对类的方法等修改之后保持其字段值不变.
7. 支持instanceOf操作实时生效
8. 支持cast强转实时生效
9. 支持对类的super class的方法实现等以上各方面进行修改的效果实时提现在子类上.
10. 支持静态方法的修改实时生效

## 计划支持的场景
- 静态变量动态修改支持
- 类继承接口的修改支持
- 单元测试补充

