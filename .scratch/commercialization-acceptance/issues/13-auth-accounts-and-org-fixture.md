# 13: 验收 auth 造数包（11 角色测试账号 + 4 档组织 fixture + 菜单基线核对）

**What to build:** 验收人员执行一个造数入口即得到整套权限走查环境：11 个标准角色测试账号可登录；§6.2 的部门 D1/D1.1/D2 与用户 m/a/c/d 及 DEPT_ONLY 角色用户、5 表归属数据齐备；用每个角色登录，菜单树与 03 号 seed 基线逐角色一致（多/缺即失败）。21 号票 07 号工单的 fixture 直接承接复用，本票补全报价表与账号清单。

**Blocked by:** 03, 04（角色 seed 与可重建环境就位）

**Status:** ready-for-agent

- [ ] `luna-srv/script/acceptance/auth/` 造数入口：11 角色测试账号（账号/密码清单写入 acceptance-runbook 的脱敏抽查登录账号表）
- [ ] §6.2 组织：部门 D1（子部门 D1.1）、D2 平级；用户 m=D1 主管（sales_manager）、a=D1 业务员（salesperson）、c=D1.1 业务员、d=D2 业务员；另设 DEPT_ONLY 角色用户于 D1；5 表（客户、客户联系人、报价单、销售合同、发货单）按 owner_user_id/dept_id 造各人归属数据（承接 21 号票 07 fixture，补报价表）
- [ ] 角色菜单基线核对脚本：逐角色登录拉菜单树与 03 号 seed 基线比对，多一项/缺一项即失败并输出差异；零 PC 菜单角色（workshop_worker/workshop_inspector）PC 菜单为空
- [ ] 造数幂等可重跑（验收库每轮重建后执行）；造数入口与 runbook 命令一致
