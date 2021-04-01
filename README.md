# Frebel--一款免费的JAVA代码热部署插件  

![Frebel](https://img.shields.io/github/issues/hengyoush/Frebel) 
![Frebel](https://img.shields.io/github/forks/hengyoush/Frebel)
![Frebel](https://img.shields.io/github/stars/hengyoush/Frebel)
![Frebel](https://img.shields.io/github/license/hengyoush/Frebel)

Frebel是一个可以让你修改的JAVA代码实时生效的插件，它有如下特点：

- 轻量化：你只需要在启动命令中加入一行配置即可使用。
- 无侵入性：不需要修改你的任何代码，开箱即用。
- 功能丰富：支持方法新增删除、类新增删除、类继承关系的改变等原生JVM不支持的操作。
- 免费：虽然功能比不过，但我们是免费开源的！

## 快速开始
1. 下载Jar包
2. 在启动命令中加入：
    ```shell script
    -javaagent:/path/to/frebel-core-1.0-SNAPSHOT.jar
    ```
3. 修改你的代码并且享受它！

## 后续规划
- 静态变量和方法的动态修改支持
- 单元测试补充
- Spring框架支持
- Dubbo框架支持

## 一些话
虽然Frebel一开始只是我的兴趣项目，但是我希望它能变得更好。  
目前Frebel可能存在很多问题，因此我需要大家的使用并且积极反馈BUG，非常欢迎大家提出宝贵的意见；同时如果对此项目有兴趣的小伙伴我们可以共同参与Frebel的开发完善。
