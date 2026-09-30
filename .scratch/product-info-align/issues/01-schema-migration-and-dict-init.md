# 01: 数据库 Schema 迁移 + 字典初始化

**What to build:** 产品表 `lunaerp_baseinfo_product` 删除 4 列（output/pattern_no/temp_code/conversion_rate）、新增 17 列（布种 2 列 + 类别标签 1 列 + 梭织参数 5 列 + 成本设置 7 列 + 包装费/税率 2 列）。在 `system_dict_type` 和 `system_dict_data` 表中创建 `lunaerp_product_category`（产品类别，默认值：成品/胚布/半成品）和 `lunaerp_fabric_type`（布种，默认值：针织/梭织/经编/纬编）两个字典类型及默认数据。迁移脚本可重复执行（ALTER TABLE ADD COLUMN IF NOT EXISTS / 先 DELETE 再 INSERT 字典数据）。

**Blocked by:** None (can start immediately)

**Status:** done

- [x] 产品表新增 17 列，类型和注释符合 ADR-0035 规范
- [x] 产品表删除 output/pattern_no/temp_code/conversion_rate 4 列
- [x] system_dict_type 新增 lunaerp_product_category 和 lunaerp_fabric_type 两条字典类型
- [x] system_dict_data 新增各字典类型下的默认值数据
- [x] 迁移 SQL 可重复执行不报错（IF NOT EXISTS / DELETE WHERE + INSERT）
- [x] 迁移后 ProductDO 能正确映射新列（DO 字段与列名一致）

## Comments

- 2026-09-30 实现完成，变更集中在 `luna-srv/sql/postgresql/lunaerp-baseinfo-product.sql`：
  - CREATE TABLE 同步为最终列集（新库直接到位），另加存量库升级段：`DROP COLUMN IF EXISTS` × 4 + `ADD COLUMN IF NOT EXISTS` × 17，可重复执行。
  - 列注释同步：删 4 列注释、增 17 列注释，`type` 改为“产品类别（字典 value）”，`supplier_code` 标签更正为“供应商样号”。
  - 新增第 7 节字典初始化：dict_type id 2249（lunaerp_product_category：成品 FINISHED / 胚布 GREIGE / 半成品 SEMI_FINISHED，dict_data 3702-3704）、2250（lunaerp_fabric_type：针织 KNIT / 梭织 WOVEN / 经编 WARP_KNIT / 纬编 WEFT_KNIT，dict_data 3705-3708），先 DELETE 再 INSERT，id 段经全库扫描确认未占用（现有最大 2248 / 3701）。
  - DO 映射：新列名均为标准 snake_case（fabric_type→fabricType 等），numeric→BigDecimal、varchar→String，与 ticket 02 的 ProductDO 字段一一对应；本 ticket 未改 Java 代码。
  - 验证方式：仓库约定“仅做语法自查，禁止在共享远程库直接执行”，本机无 psql/docker，按已执行过的同构脚本（lunaerp-stock.sql 字典段、PG IF [NOT] EXISTS DDL）逐句对照审查。
  - 注意：SQL 在库上执行后、ticket 02 落地前，旧 ProductDO 仍引用已删列（output 等），产品接口会报列不存在——两票需同批上线。
