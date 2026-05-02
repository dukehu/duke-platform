# Backend 服务开发规范

## 模块职责
- duke-parent：Maven BOM，版本统一在这里管理
- duke-framework：公共基础设施，禁止在各服务重复实现
- duke-gateway：唯一入口，路由/CORS/认证校验都在这里

## Nacos 配置
- duke-common.yml：Redis、JWT、内部密钥，禁止本地覆盖
- duke-gateway.yml：路由规则，新增路由改这里
- duke-{service}.yml：各服务私有配置
- 禁止把密钥硬编码进代码

## 编码规范
- 统一返回 Result<T>，禁止裸返回
- 分页用 PageHelper，禁止手写 LIMIT
- 金额用 Long（单位：分），禁用 Double/Float
- 日志用 @Slf4j，禁止 System.out.println
- 异常用 BusinessException(ResultCode.xxx, "msg")
- 新建 service 必须继承 duke-framework 的 BaseService
- DTO 和 Entity 严格分离，禁止把 Entity 直接返回给前端
- 接口幂等性：写操作加 @Idempotent 注解（duke-framework 提供）

## 数据库
- 禁止在 Mapper 写超过 3 个 JOIN 的 SQL，复杂查询拆到 Repository
- 禁止在 Mapper 写业务判断逻辑

## 关键约定
- 异常由 duke-framework GlobalExceptionHandler 统一处理，服务无需配置
- CORS 集中在 Gateway CorsConfig.java，服务禁止配置
- 服务通信：OpenFeign + X-Gateway-Secret，不走网关，不传 JWTr