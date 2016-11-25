---
title: 2016-11-25环境监控 
tags: Java,SWT,RXTXcomm
grammar_cjkRuby: true
---
# Agricultural-Info
农业信息实时监控


### 环境配置
* 本项目需要带有SWT插件的Eclipse/MyEclipse
* 采用64位的swt.jar,不能运行在32位的JVM
* 采用RXTXcomm.jar开发，Comm.jar年代已久，不能兼容新系统;把 dirve文件夹中的64位的rxtxParallel.dll ， rxtxSerial.dll放在JDK/bin目录下
* 如果想在32位上JVM上运行，把64位的rxtxParallel.dll ， rxtxSerial.dll，swt.jar都替换成32位的


### 项目介绍
*  项目采用Java语言开发,读取端口发过来的数据，存储并通过动态图表实时显示，并通过输入框输入内容控制各个节点。
* 软件界面用SWT开发，百分比布局
* 节点（Zigbee,蓝牙）发过来的数据存储在数据库中，一张表是一个节点，每辨别一个节点动态生成一张表
* 动态图表采用JFreeChart.jar
* 考虑到部分开发者没有节点设备，端口数据用Thread生成数据进行模拟。真实中是解析利用端口捕获到数据。

![enter description here][1]


  [1]: ./images/%E5%86%9C%E4%B8%9A%E4%BF%A1%E6%81%AF%E5%AE%9E%E6%97%B6%E7%9B%91%E6%8E%A7.PNG "农业信息实时监控.PNG"