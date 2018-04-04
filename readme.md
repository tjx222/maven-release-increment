#maven增量版本发布插件
---
本插件基于maven-release-plugin 开发， 用于根据版本库提交日志来打包项目增量，支持git,svn等常用版本管理工具
###使用方法
pom 中增加plugin 配置
	
	...
	<plugin>
                <groupId>com.mainbo.plugin</groupId>
				<artifactId>increment-maven-plugin</artifactId>
				<version>0.0.1</version>
				<configuration>
					<username>tanjinxiang</username>
					<password>5yPxINDU</password>
				</configuration>
    </plugin>
	...

执行 mvn increment:increment 即可， 若没有未提交内容，将提示需打包增量的范围，可以是时间，也可以是版本号

默认插件生成增量包前会执行 clean package 以生成编译项目。 可以通过incrementGoals 参数指定需要执行的目标。


