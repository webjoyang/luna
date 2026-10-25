# 19-errorcode-018-dedup: 错误码 1-030-018 段发染记录/样品分析重号清理

Type: task
Status: resolved
Blocked by: 

## Question

01 号 ticket 盘点错误码段位时发现：`1-030-018-xxx` 段被两方实际占用——

- production 发染记录：`enums/ErrorCodeConstants.java` 中 `GREIGE_DISPATCH_*`（018_000~007，ADR-0033，先上线）；
- sample 样品分析：`enums/SampleErrorCodeConstants.java` 中 `SAMPLE_ANALYSIS_*`（018_000~006、018_100，ADR-0036，后上线误取）。

码值 018_000~006 整数重叠、文案不同。当前异常消息随 ServiceException 携带文案，暂无线上错码错乱，但是段位约定的硬违约（shared 总表已标注"历史重号，禁用新取"）。

执行清理（AFK task，可在任意 W3 session 间隙做）：

1. 样品分析改用未占用段位（候选 `1-030-030`，避开 finance 预留块 020–029；取段前先查 shared 总表）；
2. 改 `SampleErrorCodeConstants` 全部码值、全仓引用点（后端 service/controller 测试、前端若有按码判断处）；
3. 更新 `docs/contexts/shared/GLOSSARY.md` 总表（018 行恢复为 production 发染记录独占、删除重号注记、新增样品分析新段行）与 `docs/contexts/sample/GLOSSARY.md` 错误码节、ADR-0036 中的段位文字；
4. 历史避让记录保留一条迁段说明；金额/逻辑零改动，只换码。

产出：## Answer 记录新段位、改动文件清单与验证（编译 + 样品分析相关测试）。

## Answer

新段位：**`1-030-040`**。票内候选 `1-030-030` 已作废——02-approval-engine 先行取 030、031–039 亦被各 ticket 全部占满（取段前查 shared GLOSSARY 总表，040 为 039 之后首个空闲段）；018 段恢复为 production 发染记录（ADR-0033）独占。

改动文件清单（金额/逻辑零改动，只换码）：

**代码**：
- `luna-srv/lunasrv-module-lunaerp/.../enums/SampleErrorCodeConstants.java`：javadoc/注释段位文字 + 8 个码值（000~006、100）由 018→040
- `luna-srv/.../enums/ErrorCodeConstantsTest.java`：`testSampleAnalysisErrorCodes` 8 条 assertEquals 断言码值
- `luna-srv/.../service/sample/sampleanalysis/SampleAnalysisServiceImplTest.java`：17 处 @DisplayName/注释中的码值文本
- `luna-srv/.../service/sample/sampleanalysis/SampleAnalysisServiceImpl.java`：`validateCustomer` 注释 1 处
- `ErrorCodeConstants.java`（production 发染记录）：**未动**，保留 018 段

**文档**：
- `docs/contexts/shared/GLOSSARY.md`：018 行去掉"历史重号"注记恢复为发染记录独占、新增 040 行、历史避让记录追加迁段说明
- `docs/contexts/sample/GLOSSARY.md`：模块边界行与"错误码"节段位文字
- `docs/adr/0036-sample-analysis-non-document-record-hybrid-storage.md`：决策 1 段位文字
- `.scratch/sample-analysis-mvp/spec.md`：错误码段一行

**前端**：无数字码引用（前端按 message 文案判断），零改动。

**历史避让**：未改写 `.scratch/production-progress/*` 与 `.trae/documents/contract-progress-architecture-design.md` 中 018 段引用——那些属发染记录侧，018 对其仍正确；迁段说明统一落在 shared GLOSSARY 历史避让记录。

**验证**：
- `mvn -pl lunasrv-module-lunaerp test -Dtest=ErrorCodeConstantsTest,SampleAnalysisServiceImplTest -am` 编译通过
- ErrorCodeConstantsTest 2/2 通过（含 `testSampleAnalysisErrorCodes` 断言新 040 码值）
- SampleAnalysisServiceImplTest 58/58 通过

**解阻塞**：shared GLOSSARY 总表 018 行"历史重号，禁用新取"注记已清除，018 段恢复为发染记录独占。
