# 07: 检验单卷/匹双模式改造（packageMode + pieceNo + 唯一性 + 明细血缘列）

**What to build:** 检验单主表加 `package_mode`（字典 id=2242，ROLL 缺省/PIECE）；录检明细表加 `piece_no`（可空）与 `weaving_ticket_id`（可空，本 issue 只建列，交接写入在 issue 11）。Service 校验按模式分化（ADR-0025）：ROLL 时 rollNo 必填、pieceNo 禁填，唯一 `(vatNo, rollNo)`；PIECE 时 pieceNo 必填、rollNo 可空可填且**允许重复**（一卷拆多匹），唯一 `(vatNo, pieceNo)`，pieceCount 恒 1。前端检验单工作台：基本信息加模式切换（ROLL↔PIECE，已有明细时切换需二次确认）；明细 Tab 按模式切换列与标签（卷号/匹号），PIECE 隐藏 pieceCount；列表/导出按模式显示对应列。检验单错误码复用 1-030-010 段（重复明细沿用 004 文案并按模式点名卷号/匹号）。**不含**交接检验与布票回写（P3，issue 11）。

**Status:** ready-for-human

- [x] 主表 package_mode + 明细 piece_no/weaving_ticket_id 列与字典
- [x] 双模式校验：必填/禁填、可重复 rollNo、唯一性（004）
- [x] PIECE 下 pieceCount 恒 1
- [x] 前端模式切换 + 明细/列表/导出按模式渲染
- [x] 单测：双模式唯一性、一卷拆多匹、切换守卫
