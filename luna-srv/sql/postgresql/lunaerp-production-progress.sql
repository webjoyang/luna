-- =============================================================================
-- 模块：LunaERP - 生产管理 - 生产进度（合同锚点全链路只读聚合视图）
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（菜单先 DELETE 再 INSERT，可重复执行）
-- 设计依据：ADR-0034（生产进度只读聚合视图）
--
-- 【无表结构变更】进度不是表也不是单据，是合同下各生产单据与库存流水的实时投影：
--   不落库、不加合同进度字段、不给 lunaerp_stock_ledger 加合同列（join 反查取合同）。
--   因此本脚本只含菜单与权限，聚合口径全部在 ProductionProgressQueryService 代码层。
--
-- 【菜单号段说明】生产管理目录 = 10200（见 lunaerp-production-greige-order.sql）；
--   10400-10405 已被进度反馈占用，故生产进度选用 10410 段，sort=11 排在进度反馈之后。
--
-- 注意：本脚本仅做语法自查，禁止在共享远程库直接执行。
-- =============================================================================

-- =============================================================================
-- 1. 菜单与权限（system_menu，可重复执行）
--    生产进度是只读聚合视图，仅一个 query 权限，直接挂在菜单本身（仿进度反馈先例）。
--    权限码 lunaerp:production-progress:query 与 ProductionProgressController 的
--    @PreAuthorize 一致，page / get 两个接口共用。
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_menu WHERE id IN (10410) OR permission = 'lunaerp:production-progress:query';

-- 菜单：生产进度（挂生产管理目录 10200，组件路径 lunaerp-elmui/production/production-progress/index）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10410, '生产进度', 'lunaerp:production-progress:query', 2, 11, 10200, 'production-progress', 'ep:trend-charts', 'lunaerp-elmui/production/production-progress/index', 'LunaerpElmuiProductionProgress', 0, '1', '1', '1', 'admin', '2026-09-29 00:00:00', '1', '2026-09-29 00:00:00', '0');

COMMIT;
-- @formatter:on
