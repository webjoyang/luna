# 02: 下达生产编排后端（from-contract + release）

**What to build:** 合同详情页调预填接口可看到每行的剩余可下达量与该合同已有打色单；调下达接口在同一事务内按同合同同产品分组创建胚布单（ACTIVE）、染色单（ACTIVE）、胚布检验单（GREIGE/DRAFT），并累加合同行已下达量。超下达与并发重复提交被拦截回滚；打色单支持选已有/新建/不关联三态。

**Blocked by:** 01 (SQL 基建 + DO 字段变更)

**Status:** resolved

- [x] 后端包 `com.luna.lunasrv.module.lunaerp.{controller,service,dal.*}.production.releaseproduction`：Controller、Service、`ReleaseFromContractRespVO`、`ReleaseProductionReqVO`
- [x] `GET /lunaerp/production/release-production/from-contract?contractId=`：返回合同行（contractQty、releasedQty、remainingQty=差量）+ 该合同已有打色单列表；合同不存在报 `1_030_035_000`
- [x] `POST /lunaerp/production/release-production/release`：
  - 合同存在且非 CANCELLED（CANCELLED 报 `1_030_035_001`）；group 内 contractLineId 必须属该合同
  - 逐行条件更新 `released_qty`（`WHERE released_qty + #{delta} <= quantity`），影响行数 0 报 `1_030_035_002`
  - 打色三态：EXISTING 须 CONFIRMED/IN_PRODUCTION 否则报 `1_030_035_003`、胚布/染色填引用；NEW 建 PENDING 打色单（主灯源必填，否则报 `1_030_035_004`）、胚布/染色不引用（零回写）；NONE 不处理
  - 创建胚布单 ACTIVE（颜色行含 customerColor、finishedQty、lossRate、contractLineId/contractLineNo）
  - 创建染色单 ACTIVE（颜色行含 customerColorNo、quantity、contractQty、contractLineId/contractLineNo）
  - 创建胚布检验单 type=GREIGE/status=DRAFT（targetWeight=组内下达量合计）
  - 同合同同产品多行合并到一张胚布单/染色单（多颜色行）
  - 全部在同一事务，任一异常回滚
- [x] 错误码常量类注册 `1_030_035_000`~`005`
- [x] `@LogRecord` 记录下达（合同号 + 生成单据号）
- [x] `ReleaseProductionServiceImplTest` 纯 Mockito 覆盖：正常一组一行、同产品多行合并、超下达拦截回滚、并发条件更新返回 0、EXISTING/NEW/NONE 三态、PENDING 打色引用被拒、主灯源缺失、合同 CANCELLED、from-contract 剩余量
- [x] 租户隔离：查询/条件更新带 tenant_id，跨租户合同访问被拦

## Comments
- Implemented in `9791e2d4`
- Merged into `feat/release-production` in `322686a6`
