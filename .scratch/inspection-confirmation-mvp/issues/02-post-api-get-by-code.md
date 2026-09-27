# 02: 【前置改造】PostApi 增加按岗位编码查询（system 模块纯增量）

**What to build:** system 模块的跨模块 API 可以按岗位编码查到岗位——lunaerp 质检确认需要用固定编码 LUNA_WORKER 解析"挡车工"岗位（不能固定岗位 id）。改造后 `PostApi.getPostByCode(code)` 返回岗位（不存在返回 null），供后续质检确认服务解析挡车工候选。

**Blocked by:** None (can start immediately，可与 01 并行)

**Status:** ready-for-human

- [x] `PostApi` 新增 `PostRespDTO getPostByCode(String code)`，不改既有 getPostList/validPostList 等任何方法签名
- [x] `PostApiImpl` 经 `PostService` 新增同名透传方法实现；PostService 底层直接调用已存在的 `PostMapper.selectByCode`，不新增 SQL/Mapper 方法
- [x] 岗位不存在时返回 null，不抛异常
- [x] 单元测试覆盖：命中返回岗位、编码不存在返回 null
- [x] lunaerp 模块对 system API 层的 maven 依赖可直接注入 PostApi（若 lunaerp 尚未依赖该 API 包，本票补齐依赖；不允许直接访问 system 的 Service/Mapper）
