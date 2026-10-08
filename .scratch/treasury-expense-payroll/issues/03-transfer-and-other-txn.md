# 03: 账户间转账 + 其他收支（ZZ/QT 单）

**What to build:** 出纳可登记两类无业务单据的资金动作：账户间转账（银行提现、私户转公户）——选转出/转入账户填金额日期备注，提交即同事务写两条 TRANSFER 分录并双边条件更新余额，生成 ZZ 单；其他收支（利息/废品/手续费/工本费等）——选账户方向类别填金额，提交即写 IN_OTHER/OUT_OTHER 分录，生成 QT 单。两类单据登记即生效、不可改删（写账凭证，错账红冲二期）。

**Blocked by:** 02

**Status:** ready-for-agent

- [ ] `POST /transfer/create`：转出/转入账户不可相同 020_107、必须同币种 020_106；同事务两账户按 id 升序经统一通道各过一笔——from=CREDIT/TRANSFER_OUT、to=DEBIT/TRANSFER_IN（ref_type=TRANSFER+本单、ref_no=transfer_no、账户名称快照）；一边失败整体回滚（无半转）；transfer_no=`ZZ`+YYYYMMDD+3 位日流水
- [ ] `GET /transfer/page`、`GET /transfer/get?id=`：列表与详情可查；不提供 update/delete 端点
- [ ] `POST /other-txn/create`：类别字典 2287 与 direction 后端匹配校验 020_110（INTEREST_IN/WASTE_IN/OTHER_IN→DEBIT，FEE_OUT/COST_OUT/OTHER_OUT→CREDIT）；账户外币按登记日 SPOT 取价快照（取不到 020_200）、RMB rate=1；登记即写 DEBIT/IN_OTHER 或 CREDIT/OUT_OTHER（ref_type=OTHER_TXN）；txn_no=`QT`+YYYYMMDD+3 位
- [ ] `GET /other-txn/page`、`GET /other-txn/get?id=`；不提供 update/delete 端点
- [ ] 权限码 account:transfer / account:other-txn；登记 @LogRecord；金额 @Sensitive(AMOUNT)
- [ ] Mockito 单测：同账户 020_107/跨币种 020_106/双边两笔分录类别方向 ref 正确/一边失败整体回滚/类别方向不符 020_110/外币登记日取价快照/登记后不可改删（无端点即无入口）/纵向越权 403
