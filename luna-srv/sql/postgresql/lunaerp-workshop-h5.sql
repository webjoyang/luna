-- =============================================================================
-- 模块：LunaERP - 生产管理 - 车间移动 H5（权限容器、角色与配置种子）
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro
-- 可重复执行：DELETE 先清后插 / ON CONFLICT
-- =============================================================================
BEGIN;

-- [2026-10-21] 02-infra-config-dict-frontend-deps: 车间移动H5发纱默认仓库配置项种子
DELETE FROM infra_config WHERE config_key = 'luna.workshop.default-warehouse-id';
INSERT INTO infra_config (
    id, category, type, name, config_key, value, visible, remark,
    creator, create_time, updater, update_time, deleted
) VALUES (
    103, 'biz', 2, '车间移动H5发纱默认仓库', 'luna.workshop.default-warehouse-id', '', '1',
    '车间移动H5发纱默认仓库ID，系统管理参数设置维护，未配置时前端强制手动选择',
    'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
);

-- [2026-10-21] 03-weaving-ticket-exception-photos-column: 布票加异常照片列
ALTER TABLE lunaerp_production_weaving_ticket
    ADD COLUMN IF NOT EXISTS exception_photos jsonb;
COMMENT ON COLUMN lunaerp_production_weaving_ticket.exception_photos IS '异常现场照片 URL JSON 数组（车间 H5 异常登记上传，≤6 张，infra 文件服务）';

-- [2026-10-21] 04-inspection-item-photos-column: 录检明细加疵点照片列
ALTER TABLE lunaerp_production_inspection_order_item
    ADD COLUMN IF NOT EXISTS photos jsonb;
COMMENT ON COLUMN lunaerp_production_inspection_order_item.photos IS '疵点照片 URL JSON 数组（车间 H5 录检上传，≤6 张，infra 文件服务）';

COMMIT;
