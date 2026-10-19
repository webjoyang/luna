# 02: 统一过账服务 FinancePostingService + 账户域扩展接口 + ADR-0043

**What to build:** 域公共唯一资金写入口就位：任何模块要动钱只能注入 `FinancePostingService.post(...)`——校验账户存在/启用/币种一致 → 条件更新 current_balance（流出超额或停用即失败）→ 同事务 INSERT 一条分录（含本 spec 扩列 party_name/ref_no）；全域无手工改余额端点，可用余额=current_balance 实时展示。账户管理获得完整生命周期：编辑（账号/性质/备注/停启用）、期初仅无分录可改、无分录可删进回收站，停用账户不再出现在各业务下拉。余额口径决策钉入 ADR-0043。

**Blocked by:** 01（SQL 基建）

**Status:** closed

- [x] `FinancePostingService.post(PostingReq{ accountId, direction, category, partyType, partyId, partyName, currency, originalAmount, exchangeRate, bizDate, refType, refId, refItemId?, refNo, remark })`：账户不存在 020_100、status≠ENABLE 020_101、分录币种≠账户币种 020_106；条件更新（10 号定稿 SQL 形态）——流入 `SET current_balance = current_balance + #{amt} WHERE id=#{id} AND status='ENABLE'`、流出 `… - #{amt} … AND current_balance >= #{amt}`，updateCount=0 先查状态区分 020_101/023_004；INSERT ledger_entry 字段以 10 号定稿 + party_name/ref_no 扩列为准，base_amount=round(original×rate,2)、RMB rate=1；全程同事务，调用方异常整体回滚
- [x] 兼容 11 号契约：payment 层 catch 到的余额不足就是 023_004、停用就是 020_101，11 号既有 mock seam 单测不变；10 号收款 handler 收口到本服务的改造点以代码注释挂账（10 号落地时收口，语义零变化）；13 号往来分录只读 append 接口（account_id 为空不碰余额）预留签名
- [x] `PUT /account/update`：备注/状态（停用↔启用）/account_no/account_nature 可改；opening_balance 仅无分录可改否则 020_104；币种/类型建后不可改；account_no 按类型条件必填 020_105（BANK/WECHAT/ALIPAY 必填、CASH 可空）；名称租户内唯一含已删除行 020_102
- [x] `DELETE /account/delete?id=`：无分录才可删（020_103 友好提示"已有分录请停用"），进回收站 FINANCE_ACCOUNT（ADR-0038，恢复回原状态）；`GET /account/simple-list` 过滤停用账户（收付款/报销/工资/转账/其他收支下拉共用）；停用后历史流水与统计不受影响
- [x] account/ledger DO 扩列（若 10 号 DO/Mapper 未落地，按 10 号定稿补齐含新列的最小骨架供本域与后续票使用）；分录方向值 DEBIT/CREDIT 随 10 号
- [x] 权限码 account: update/delete；账户增改/停启用/期初修改/删除 @LogRecord；account_no @Sensitive(CONTACT)、金额 @Sensitive(AMOUNT)
- [x] **新建 ADR-0043**：一张经营分录账 + current_balance 受控物化缓存（只经统一过账通道变更、无手工端点、红冲追加反向行、期初为账户主数据），`docs/adr/` 全局连续编号
- [x] Mockito 单测：流出超额 023_004/停用 020_101/币种不符 020_106/base_amount 折算（RMB=1、外币四舍五入）/分录字段（category/direction/party_name/ref_no）正确/期初有分录禁改 020_104 与无分录可改/账号条件必填 020_105/名称唯一 020_102（含软删行）/有分录禁删 020_103/停用后新过账被拒且历史统计不变/横向越权报不存在
