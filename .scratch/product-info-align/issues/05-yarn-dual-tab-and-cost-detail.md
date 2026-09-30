# 05: 纱线规格双 Tab 组件 + 两级下拉 + 成本结构明细

**What to build:** 新增 ProductYarnCostSection.vue 组件，实现纱线规格和成本结构明细两个完整分区。纱线规格分区：针织/梭织双 Tab 切换，梭织下有简易估算/经纬精算两种子模式切换。表格列随模式动态变化（针织/简易：纱线规格/单价/比例/损耗率/成本贡献；经纬精算：纱线规格/单价/密度/织缩率/损耗率/每米用量/每米金额，按经纱/纬纱分组）。坯布参数输入（克重/门幅 → 每米重量自动计算）。新增 YarnSelect.vue 两级下拉组件（左侧材质分组从 lunaerp_production_yarn_type 取数，右侧规格项从 lunaerp_production_yarn_spec 取数，选中存 yarn_spec_id + yarn_spec_name 冗余，自动带出 ref_price 作为单价）。成本结构明细分区：成本项表格（供应商远程搜索存 id+name 冗余/工序/单价/损耗率）、加工费设置（针织模式显示针织加工费元/KG，梭织模式显示织造费元/M + 其他加工费元/M）、成本汇总展示（总成本/等效成本/每公斤约/未设价提示）。集成 calculateProductYarnCost 纯函数实时计算。保存时序列化 yarnSpecs/costItems 为 JSON 字符串提交。模式切换时不丢失已录入数据（字段按超集存储）。

**Blocked by:** 02 (Backend DO/VO alignment), 03 (Yarn cost calc function and tests), 04 (Frontend form restructure)

**Status:** ready-for-agent

- [ ] ProductYarnCostSection.vue 组件实现针织/梭织双 Tab 切换
- [ ] 梭织 Tab 下有简易估算/经纬精算两种子模式切换按钮
- [ ] 针织/简易模式表格列：纱线规格/单价/比例%/损耗率%/成本贡献
- [ ] 经纬精算模式表格列：纱线规格/单价/密度/织缩率%/损耗率%/每米用量/每米金额，按经纱/纬纱分组
- [ ] YarnSelect.vue 两级下拉：左侧材质分组，右侧规格项，选中存 yarn_spec_id + yarn_spec_name
- [ ] 选中纱线规格后自动带出 ref_price 作为单价
- [ ] 坯布克重/门幅输入后自动计算每米重量（克重×门幅/100000）
- [ ] 成本项表格：供应商远程搜索（存 id+name）/工序/单价/损耗率
- [ ] 加工费设置随模式变化（针织：针织加工费/KG；梭织：织造费/M + 其他加工费/M）
- [ ] 成本汇总展示：总成本/等效成本/每公斤约/未设价纱线数提示
- [ ] 集成 calculateProductYarnCost 函数，输入变化时实时重算
- [ ] 保存时 yarnSpecs/costItems 序列化为 JSON 字符串提交后端
- [ ] 编辑已有产品时所有纱线行和成本项正确回显
- [ ] 针织→梭织、简易→经纬精算模式切换时不丢失已录入纱线行数据
