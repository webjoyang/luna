Java 后端：`master` 分支为 JDK 8 + Spring Boot 2.7，`master-jdk17` 分支为 JDK 17/21 + Spring Boot 3.5，`master-jdk25` 分支为 JDK 25 + Spring Boot 4.x

- 管理后台的电脑端：Vue3 提供 `element-plus`、`vben(ant-design-vue)` 两个版本，Vue2 提供 `element-ui` 版本
- 管理后台的移动端：采用 `uni-app` 方案，一份代码多终端适配，同时支持 APP、小程序、H5！
- 后端采用 Spring Boot 多模块架构、MySQL + MyBatis Plus、Redis + Redisson
- 数据库使用PostgreSQL
- 消息队列可使用 Event、Redis、RabbitMQ、Kafka、RocketMQ 等
- 权限认证使用 Spring Security & Token & Redis，支持多终端、多种用户的认证系统，支持 SSO 单点登录
- 支持加载动态权限菜单，按钮级别权限控制，Redis 缓存提升性能
- 支持 SaaS 多租户，可自定义每个租户的权限，提供透明化的多租户底层封装
- 工作流使用 Flowable，支持动态表单、在线设计流程、会签 / 或签、多种任务分配方式
- 高效率开发，使用代码生成器可以一键生成 Java、Vue 前后端代码、SQL 脚本、接口文档，支持单表、树表、主子表
- 实时通信，采用 Spring WebSocket 实现，内置 Token 身份校验，支持 WebSocket 集群
- 集成微信小程序、微信公众号、企业微信、钉钉等三方登陆，集成支付宝、微信等支付与退款
- 集成阿里云、腾讯云等短信渠道，集成 MinIO、阿里云、腾讯云、七牛云等云存储服务
- 集成报表设计器、大屏设计器，通过拖拽即可生成酷炫的报表与大屏

### 系统功能

| <br /> | 功能    | 描述                              |
| ------ | ----- | ------------------------------- |
| <br /> | 用户管理  | 用户是系统操作者，该功能主要完成系统用户配置          |
| ⭐️     | 在线用户  | 当前系统中活跃用户状态监控，支持手动踢下线           |
| <br /> | 角色管理  | 角色菜单权限分配、设置角色按机构进行数据范围权限划分      |
| <br /> | 菜单管理  | 配置系统菜单、操作权限、按钮权限标识等，本地缓存提供性能    |
| <br /> | 部门管理  | 配置系统组织机构（公司、部门、小组），树结构展现支持数据权限  |
| <br /> | 岗位管理  | 配置系统用户所属担任职务                    |
| 🚀     | 租户管理  | 配置系统租户，支持 SaaS 场景下的多租户功能        |
| 🚀     | 租户套餐  | 配置租户套餐，自定每个租户的菜单、操作、按钮的权限       |
| <br /> | 字典管理  | 对系统中经常使用的一些较为固定的数据进行维护          |
| 🚀     | 短信管理  | 短信渠道、短息模板、短信日志，对接阿里云、腾讯云等主流短信平台 |
| 🚀     | 邮件管理  | 邮箱账号、邮件模版、邮件发送日志，支持所有邮件平台       |
| 🚀     | 站内信   | 系统内的消息通知，提供站内信模版、站内信消息          |
| 🚀     | 操作日志  | 系统正常操作日志记录和查询，集成 Swagger 生成日志内容 |
| ⭐️     | 登录日志  | 系统登录日志记录查询，包含登录异常               |
| 🚀     | 错误码管理 | 系统所有错误码的管理，可在线修改错误提示，无需重启服务     |
| <br /> | 通知公告  | 系统通知公告信息发布维护                    |
| 🚀     | 敏感词   | 配置系统敏感词，支持标签分组                  |
| 🚀     | 应用管理  | 管理 SSO 单点登录的应用，支持多种 OAuth2 授权方式 |
| 🚀     | 地区管理  | 展示省份、城市、区镇等城市信息，支持 IP 对应城市      |

## 🐨 技术栈

### 模块

| 项目                      | 说明                    |
| ----------------------- | --------------------- |
| `lunasrv-dependencies`  | Maven 依赖版本管理          |
| `lunasrv-framework`     | Java 框架拓展             |
| `lunasrv-server`        | 管理后台 + 用户 APP 的服务端    |
| `lunasrv-module-system` | 系统功能的 Module 模块       |
| `lunasrv-module-member` | 会员中心的 Module 模块       |
| `lunasrv-module-infra`  | 基础设施的 Module 模块       |
| `lunasrv-module-bpm`    | 工作流程的 Module 模块       |
| `lunasrv-module-pay`    | 支付系统的 Module 模块       |
| `lunasrv-module-mall`   | 商城系统的 Module 模块       |
| `lunasrv-module-erp`    | ERP 系统的 Module 模块     |
| `lunasrv-module-crm`    | CRM 系统的 Module 模块     |
| `lunasrv-module-mes`    | MES 系统的 Module 模块     |
| `lunasrv-module-wms`    | WMS 系统的 Module 模块     |
| `lunasrv-module-hrm`    | HRM 人力资源管理的 Module 模块 |
| `lunasrv-module-fms`    | FMS 财务管理的 Module 模块   |
| `lunasrv-module-im`     | IM 即时通讯的 Module 模块    |
| `lunasrv-module-ai`     | AI 大模型的 Module 模块     |
| `lunasrv-module-iot`    | IoT 物联网的 Module 模块    |
| `lunasrv-module-mp`     | 微信公众号的 Module 模块      |
| `lunasrv-module-report` | 大屏报表 Module 模块        |

### 框架

| 框架                                                                                          | 说明               | 版本             | 学习指南                                                             |
| ------------------------------------------------------------------------------------------- | ---------------- | -------------- | ---------------------------------------------------------------- |
| [Spring Boot](https://spring.io/projects/spring-boot)                                       | 应用开发框架           | 3.5.15         | [文档](https://github.com/YunaiV/SpringBoot-Labs)                  |
| [MySQL](https://www.mysql.com/cn/)                                                          | 数据库服务器           | 5.7 / 8.0+     | <br />                                                           |
| [Druid](https://github.com/alibaba/druid)                                                   | JDBC 连接池、监控组件    | 1.2.28         | [文档](http://www.iocoder.cn/Spring-Boot/datasource-pool/?lunasrv) |
| [MyBatis Plus](https://mp.baomidou.com/)                                                    | MyBatis 增强工具包    | 3.5.16         | [文档](http://www.iocoder.cn/Spring-Boot/MyBatis/?lunasrv)         |
| [Dynamic Datasource](https://dynamic-datasource.com/)                                       | 动态数据源            | 4.5.0          | [文档](http://www.iocoder.cn/Spring-Boot/datasource-pool/?lunasrv) |
| [Redis](https://redis.io/)                                                                  | key-value 数据库    | 5.0 / 6.0 /7.0 | <br />                                                           |
| [Redisson](https://github.com/redisson/redisson)                                            | Redis 客户端        | 4.6.1          | [文档](http://www.iocoder.cn/Spring-Boot/Redis/?lunasrv)           |
| [Spring MVC](https://github.com/spring-projects/spring-framework/tree/master/spring-webmvc) | MVC 框架           | 6.2.19         | [文档](http://www.iocoder.cn/SpringMVC/MVC/?lunasrv)               |
| [Spring Security](https://github.com/spring-projects/spring-security)                       | Spring 安全框架      | 6.5.11         | [文档](http://www.iocoder.cn/Spring-Boot/Spring-Security/?lunasrv) |
| [Hibernate Validator](https://github.com/hibernate/hibernate-validator)                     | 参数校验组件           | 8.0.3          | [文档](http://www.iocoder.cn/Spring-Boot/Validation/?lunasrv)      |
| [Flowable](https://github.com/flowable/flowable-engine)                                     | 工作流引擎            | 8.0.0          | [文档](https://doc.iocoder.cn/bpm/)                                |
| [Quartz](https://github.com/quartz-scheduler)                                               | 任务调度组件           | 2.5.0          | [文档](http://www.iocoder.cn/Spring-Boot/Job/?lunasrv)             |
| [Springdoc](https://springdoc.org/)                                                         | Swagger 文档       | 2.8.17         | [文档](http://www.iocoder.cn/Spring-Boot/Swagger/?lunasrv)         |
| [SkyWalking](https://skywalking.apache.org/)                                                | 分布式应用追踪系统        | 9.6.0          | [文档](http://www.iocoder.cn/Spring-Boot/SkyWalking/?lunasrv)      |
| [Spring Boot Admin](https://github.com/codecentric/spring-boot-admin)                       | Spring Boot 监控平台 | 3.5.9          | [文档](http://www.iocoder.cn/Spring-Boot/Admin/?lunasrv)           |
| [Jackson](https://github.com/FasterXML/jackson)                                             | JSON 工具库         | 2.21.4         | <br />                                                           |
| [MapStruct](https://mapstruct.org/)                                                         | Java Bean 转换     | 1.6.3          | [文档](http://www.iocoder.cn/Spring-Boot/MapStruct/?lunasrv)       |
| [Lombok](https://projectlombok.org/)                                                        | 消除冗长的 Java 代码    | 1.18.46        | [文档](http://www.iocoder.cn/Spring-Boot/Lombok/?lunasrv)          |
| [JUnit](https://junit.org/junit5/)                                                          | Java 单元测试框架      | 5.12.2         | -                                                                |
| [Mockito](https://github.com/mockito/mockito)                                               | Java Mock 框架     | 5.17.0         | -                                                                |

## 🚀 快速启动（本地开发）

### 环境要求

| 组件                | 版本要求     |
|-------------------|----------|
| JDK               | 17+（支持 17 / 21） |
| Maven             | 3.8+     |
| PostgreSQL / MySQL | 数据源，默认使用 PostgreSQL |
| Redis             | 5.0+     |

> `lunasrv-server` 的 `local` 环境默认连接远程 PostgreSQL 与 Redis，配置见
> `lunasrv-server/src/main/resources/application-local.yaml`，本地无需额外启动中间件；
> 如需改用本地实例，修改该文件中的数据源与 Redis 地址、账号密码即可。

### 启动步骤

1. 在 `luna-srv` 根目录构建全部模块（首次启动必须先执行，用于将各模块安装到本地 Maven 仓库）：

   ```bash
   mvn clean install -DskipTests
   ```

2. 启动 `lunasrv-server` 模块：

   ```bash
   # 进入 server 模块目录启动
   cd lunasrv-server
   mvn spring-boot:run -am

   或在 IDEA 中直接运行启动类 `com.luna.lunasrv.server.LunasrvServerApplication`。

### 启动信息

| 项目              | 值                                                      |
|-----------------|--------------------------------------------------------|
| 激活配置            | `application.yaml` → `spring.profiles.active: local`   |
| 服务端口            | `48080`                                                |
| 健康检查            | http://localhost:48080/actuator/health                |
| 接口文档（Knife4j）   | http://localhost:48080/doc.html                       |
| 接口文档（Swagger）  | http://localhost:48080/swagger-ui                     |
| 日志文件            | `${user.home}/logs/lunasrv-server.log`                |
| 前端地址（需单独启动）     | http://localhost（见 `luna-ui/README.md`）               |

### 停止服务

- IDEA 中：直接点击运行窗口的停止按钮。
- 命令行前台运行时：在终端按 `Ctrl + C`。

若服务在后台运行，可按端口号结束进程（`48080` 为服务端口）：

```powershell
# Windows PowerShell
Get-NetTCPConnection -LocalPort 48080 -State Listen | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
```

```bash
# Windows CMD
netstat -ano | findstr :48080
taskkill /F /PID <PID>

# Linux / macOS
lsof -ti:48080 | xargs kill -9
```

> 结束后可访问 http://localhost:48080/actuator/health 确认服务已停止（请求失败即为已停止）。