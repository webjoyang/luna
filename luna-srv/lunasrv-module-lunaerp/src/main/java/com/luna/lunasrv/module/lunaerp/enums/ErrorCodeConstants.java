package com.luna.lunasrv.module.lunaerp.enums;

import com.luna.lunasrv.framework.common.exception.ErrorCode;

/**
 * LunaERP 错误码枚举类
 *
 * lunaerp 系统，使用 1-030-001-xxx 段（产品）
 *                  与 1-030-002-xxx 段（胚布品种，见 GreigeErrorCodeConstants）
 *                  与 1-030-003-xxx 段（销售合同）
 *                  与 1-030-004-xxx 段（客户，customer 与 customer-contact 共用）
 *                  与 1-030-005-xxx 段（供应商，supplier 与 supplier-contact 共用）
 *                  与 1-030-006-xxx 段（胚布单，主表与颜色行共用）
 *                  与 1-030-007-xxx 段（染色单，主表与颜色明细共用）
 *                  与 1-030-008-xxx 段（纱线档案：材质+规格+配比BOM+库存台账共用）
 *                  与 1-030-009-xxx 段（纱线采购单：主表+明细共用）
 */
public interface ErrorCodeConstants {

    // ========== 产品模块 1-030-001-000 ==========
    ErrorCode PRODUCT_NOT_EXISTS = new ErrorCode(1_030_001_000, "产品不存在");
    ErrorCode PRODUCT_CODE_DUPLICATE = new ErrorCode(1_030_001_001, "编号已被占用，请重新获取编号");
    ErrorCode PRODUCT_TAG_NOT_EXISTS = new ErrorCode(1_030_001_002, "产品标签不存在");
    ErrorCode PRODUCT_TAG_NAME_DUPLICATE = new ErrorCode(1_030_001_003, "已经存在名为【{}】的产品标签");
    ErrorCode PRODUCT_TAG_COLOR_INVALID = new ErrorCode(1_030_001_004, "标签颜色只能从预设色里选");
    ErrorCode PRODUCT_TAG_NAME_TOO_LONG = new ErrorCode(1_030_001_005, "标签最多 8 个字，请再精简一下");
    ErrorCode PRODUCT_TAG_LIMIT_EXCEEDED = new ErrorCode(1_030_001_006, "一个产品最多 20 个标签，当前 {} 个");
    ErrorCode PRODUCT_TAG_NAME_EMPTY = new ErrorCode(1_030_001_007, "标签内容不能为空");

    // ========== 客户模块 1-030-004-000（customer 与 customer-contact 共用） ==========
    // 注：1-030-002 段已由胚布品种先行占用（GreigeErrorCodeConstants，随产品模块迁移上线），
    // 客户作为后开发模块改用 004 段，避免整段重号。
    ErrorCode CUSTOMER_NOT_EXISTS = new ErrorCode(1_030_004_000, "客户不存在");
    ErrorCode CUSTOMER_CONTACT_NOT_EXISTS = new ErrorCode(1_030_004_001, "客户联系人不存在");

    // ========== 销售合同模块 1-030-003-000 ==========
    ErrorCode SALES_CONTRACT_NOT_EXISTS = new ErrorCode(1_030_003_000, "销售合同不存在");
    ErrorCode SALES_CONTRACT_NO_DUPLICATE = new ErrorCode(1_030_003_001, "合同编号已被占用，请修改编号或重新获取");

    // ========== 供应商模块 1-030-005-000（supplier 与 supplier-contact 共用） ==========
    ErrorCode SUPPLIER_NOT_EXISTS = new ErrorCode(1_030_005_000, "供应商不存在");
    ErrorCode SUPPLIER_NO_DUPLICATE = new ErrorCode(1_030_005_001, "供应商编号已被占用，请修改编号或重新获取");
    ErrorCode SUPPLIER_CONTACT_NOT_EXISTS = new ErrorCode(1_030_005_002, "供应商联系人不存在");

    // ========== 胚布单模块 1-030-006-000（主表与颜色行共用） ==========
    ErrorCode GREIGE_ORDER_NOT_EXISTS = new ErrorCode(1_030_006_000, "胚布单不存在");
    ErrorCode GREIGE_ORDER_NO_DUPLICATE = new ErrorCode(1_030_006_001, "胚布单号【{}】已存在");
    ErrorCode GREIGE_ORDER_REFERENCE_NOT_FOUND = new ErrorCode(1_030_006_002, "关联的{}不存在或不可用");

    // ========== 染色单模块 1-030-007-000（主表与颜色明细共用） ==========
    ErrorCode DYEING_ORDER_NOT_EXISTS = new ErrorCode(1_030_007_000, "染色单不存在");
    ErrorCode DYEING_ORDER_STATUS_NOT_ALLOWED = new ErrorCode(1_030_007_001, "当前状态不允许该操作");
    ErrorCode DYEING_ORDER_REFERENCE_NOT_FOUND = new ErrorCode(1_030_007_002, "关联的{}不存在或不可用");

    // ========== 纱线档案模块 1-030-008-000（材质+规格+配比BOM+库存台账共用） ==========
    ErrorCode YARN_TYPE_NOT_EXISTS = new ErrorCode(1_030_008_000, "纱线材质不存在");
    ErrorCode YARN_TYPE_NAME_DUPLICATE = new ErrorCode(1_030_008_001, "纱线材质名【{}】已存在");
    ErrorCode YARN_SPEC_NOT_EXISTS = new ErrorCode(1_030_008_002, "纱线规格不存在");
    ErrorCode YARN_SPEC_NAME_DUPLICATE = new ErrorCode(1_030_008_003, "纱线规格【{}】已存在");
    ErrorCode YARN_RECIPE_BOM_NOT_EXISTS = new ErrorCode(1_030_008_004, "纱线配比BOM不存在");
    ErrorCode YARN_RECIPE_BOM_ITEM_NOT_EXISTS = new ErrorCode(1_030_008_005, "纱线配比BOM明细不存在");
    ErrorCode YARN_RECIPE_BOM_PRODUCT_EXISTS = new ErrorCode(1_030_008_007, "该产品已存在纱线配比BOM");

    // ========== 纱线采购单模块 1-030-009-000（主表+明细共用） ==========
    ErrorCode YARN_ORDER_NOT_EXISTS = new ErrorCode(1_030_009_000, "纱线采购单不存在");
    ErrorCode YARN_ORDER_NO_DUPLICATE = new ErrorCode(1_030_009_001, "纱线采购单号【{}】已存在");
    ErrorCode YARN_ORDER_STATUS_NOT_ALLOWED = new ErrorCode(1_030_009_002, "当前状态不允许该操作");
    ErrorCode YARN_ORDER_REFERENCE_NOT_FOUND = new ErrorCode(1_030_009_003, "关联的{}不存在或不可用");
    ErrorCode YARN_ORDER_GREIGE_ORDERS_EMPTY = new ErrorCode(1_030_009_004, "请至少选择一张胚布指示单");

    // ========== 检验单模块 1-030-010-000（主表与明细共用） ==========
    ErrorCode INSPECTION_ORDER_NOT_EXISTS = new ErrorCode(1_030_010_000, "检验单不存在");
    ErrorCode INSPECTION_ORDER_NO_DUPLICATE = new ErrorCode(1_030_010_001, "检验单号【{}】已存在");
    ErrorCode INSPECTION_ORDER_STATUS_NOT_ALLOWED = new ErrorCode(1_030_010_002, "当前状态不允许该操作");
    ErrorCode INSPECTION_ORDER_REFERENCE_NOT_FOUND = new ErrorCode(1_030_010_003, "关联的{}不存在或不可用");
    ErrorCode INSPECTION_ORDER_ROLL_DUPLICATE = new ErrorCode(1_030_010_004, "单内卷号【{}】缸号【{}】已存在");
    ErrorCode INSPECTION_ORDER_ROLL_NO_REQUIRED = new ErrorCode(1_030_010_002, "卷装模式下卷号不能为空");
    ErrorCode INSPECTION_ORDER_PIECE_NO_FORBIDDEN = new ErrorCode(1_030_010_002, "卷装模式下禁止填写匹号");
    ErrorCode INSPECTION_ORDER_PIECE_NO_REQUIRED = new ErrorCode(1_030_010_002, "匹装模式下匹号不能为空");
    ErrorCode INSPECTION_ORDER_PIECE_DUPLICATE = new ErrorCode(1_030_010_004, "单内匹号【{}】缸号【{}】已存在");
    ErrorCode INSPECTION_ORDER_STATUS_NOT_COMPLETED = new ErrorCode(1_030_010_005, "仅录检完成（COMPLETED）状态允许生成入库单");
    ErrorCode INSPECTION_ORDER_NO_QUALIFIED_ITEMS = new ErrorCode(1_030_010_006, "检验单无未入库的合格明细，无法生成入库单");
    ErrorCode INSPECTION_ORDER_ITEM_NOT_EXISTS = new ErrorCode(1_030_010_100, "录检明细不存在");

    // ========== 入库单模块 1-030-011-000（主表/明细/放置共用） ==========
    ErrorCode STOCK_IN_NOT_EXISTS = new ErrorCode(1_030_011_000, "入库单不存在");
    ErrorCode STOCK_IN_NO_DUPLICATE = new ErrorCode(1_030_011_001, "入库单号【{}】已存在");
    ErrorCode STOCK_IN_STATUS_NOT_ALLOWED = new ErrorCode(1_030_011_002, "当前状态不允许该操作");
    ErrorCode STOCK_IN_REFERENCE_NOT_FOUND = new ErrorCode(1_030_011_003, "关联的{}不存在或不可用");
    ErrorCode STOCK_IN_LOCATION_NOT_IN_WAREHOUSE = new ErrorCode(1_030_011_004, "库位【{}】不属于入库仓库【{}】");
    ErrorCode STOCK_IN_ITEM_EMPTY = new ErrorCode(1_030_011_005, "审核失败：入库单没有明细");
    ErrorCode STOCK_IN_ITEM_MUTEX_FAILED = new ErrorCode(1_030_011_006, "入库明细物品必须二选一且产品与纱线规格互斥");
    ErrorCode STOCK_IN_ITEM_YARN_UNIT_INVALID = new ErrorCode(1_030_011_007, "纱线规格入库单位必须为KG");
    ErrorCode STOCK_IN_ITEM_TYPE_INVALID = new ErrorCode(1_030_011_008, "入库明细物品类型【{}】非法");

    // ========== 仓库与库位模块 1-030-013-000 ==========
    ErrorCode WAREHOUSE_NOT_EXISTS = new ErrorCode(1_030_013_000, "仓库不存在");
    ErrorCode WAREHOUSE_NO_DUPLICATE = new ErrorCode(1_030_013_001, "仓库编号【{}】已存在");
    ErrorCode WAREHOUSE_HAS_LOCATION = new ErrorCode(1_030_013_002, "仓库下存在库位，不能删除，请停用");
    ErrorCode WAREHOUSE_REFERENCED_BY_STOCK_IN = new ErrorCode(1_030_013_003, "仓库已有入库单引用，不能删除，请停用");
    ErrorCode WAREHOUSE_LOCATION_NOT_EXISTS = new ErrorCode(1_030_013_100, "库位不存在");
    ErrorCode WAREHOUSE_LOCATION_CODE_DUPLICATE = new ErrorCode(1_030_013_101, "库位编码【{}】在同一仓库下已存在");
    ErrorCode WAREHOUSE_LOCATION_IN_USE = new ErrorCode(1_030_013_102, "库位已被入库放置记录引用，不能删除，请停用");

    // ========== 织造执行 - 织造单模块 1-030-015-000 ==========
    ErrorCode WEAVING_ORDER_NOT_EXISTS = new ErrorCode(1_030_015_000, "织造单不存在");
    ErrorCode WEAVING_ORDER_CODE_DUPLICATE = new ErrorCode(1_030_015_001, "织造单号【{}】已存在");
    ErrorCode WEAVING_ORDER_STATUS_NOT_ALLOW = new ErrorCode(1_030_015_002, "织造单状态不允许该操作");
    ErrorCode WEAVING_ORDER_GREIGE_ORDER_INVALID = new ErrorCode(1_030_015_003, "关联胚布单不存在或非自织状态");
    ErrorCode WEAVING_ORDER_PLANNED_QTY_INVALID = new ErrorCode(1_030_015_004, "计划匹数必须为正整数");
    ErrorCode WEAVING_ORDER_NO_COMPLETED_TICKETS = new ErrorCode(1_030_015_005, "织造单暂无完工布票，无法交接检验");

    // ========== 织造执行 - 机台任务模块 1-030-015-100 ==========
    ErrorCode MACHINE_TASK_NOT_EXISTS = new ErrorCode(1_030_015_100, "机台任务不存在");
    ErrorCode MACHINE_TASK_CODE_DUPLICATE = new ErrorCode(1_030_015_101, "机台任务编号【{}】已存在");
    ErrorCode MACHINE_TASK_STATUS_NOT_ALLOW = new ErrorCode(1_030_015_102, "机台任务状态不允许该操作");
    ErrorCode MACHINE_TASK_PLANNED_QTY_INVALID = new ErrorCode(1_030_015_103, "计划匹数必须为正整数");
    ErrorCode MACHINE_TASK_DATE_RANGE_INVALID = new ErrorCode(1_030_015_104, "计划结束日期不能早于计划开始日期");
    ErrorCode MACHINE_TASK_HALT_REASON_EMPTY = new ErrorCode(1_030_015_105, "停机原因不能为空");
    ErrorCode MACHINE_TASK_COMPLETED_CANNOT_HALT = new ErrorCode(1_030_015_106, "已完工的机台任务不能标停机");

    // ========== 织造执行 - 织机模块 1-030-015-200 ==========
    ErrorCode LOOM_NOT_EXISTS = new ErrorCode(1_030_015_200, "织机不存在");
    ErrorCode LOOM_NO_DUPLICATE = new ErrorCode(1_030_015_201, "机台号【{}】已存在");
    ErrorCode LOOM_REFERENCED_BY_MACHINE_TASK = new ErrorCode(1_030_015_202, "织机已被机台任务引用，不能删除，请停用");

    // ========== 织造执行 - 布票模块 1-030-015-110 ==========
    ErrorCode WEAVING_TICKET_NOT_EXISTS = new ErrorCode(1_030_015_110, "布票不存在");
    ErrorCode WEAVING_TICKET_STATUS_NOT_ALLOW = new ErrorCode(1_030_015_111, "布票当前状态不允许该操作");
    ErrorCode WEAVING_TICKET_ALREADY_VOIDED = new ErrorCode(1_030_015_112, "布票已作废");
    ErrorCode WEAVING_TICKET_EXCEPTION_TYPE_INVALID = new ErrorCode(1_030_015_113, "异常类型非法");
    ErrorCode WEAVING_TICKET_EXCEED_PLANNED_QTY = new ErrorCode(1_030_015_114, "生成布票数量超过机台任务计划余量");
    ErrorCode WEAVING_TICKET_VOID_REASON_EMPTY = new ErrorCode(1_030_015_115, "作废原因不能为空");
    ErrorCode WEAVING_TICKET_CODE_DUPLICATE = new ErrorCode(1_030_015_116, "布票编号【{}】已存在");

    // ========== 织造执行 - 领纱单与明细模块 1-030-015-300 ==========
    ErrorCode YARN_REQUISITION_NOT_EXISTS = new ErrorCode(1_030_015_300, "领纱单不存在");
    ErrorCode YARN_REQUISITION_CODE_DUPLICATE = new ErrorCode(1_030_015_301, "领纱单号【{}】已存在");
    ErrorCode YARN_REQUISITION_ITEM_NOT_EXISTS = new ErrorCode(1_030_015_310, "领纱明细不存在");
    ErrorCode YARN_REQUISITION_STOCK_NOT_ENOUGH = new ErrorCode(1_030_015_311, "纱线库存不足：需求【{}】，可用【{}】");
    ErrorCode YARN_REQUISITION_STATUS_NOT_ALLOW = new ErrorCode(1_030_015_312, "领纱单状态不允许该操作");
    ErrorCode YARN_REQUISITION_ITEM_QTY_INVALID = new ErrorCode(1_030_015_313, "领纱明细需求数量必须大于0");

    // ========== 织造执行 - IoT 接入模块 1-030-015-400 ==========
    ErrorCode IOT_VENDOR_CREDENTIAL_NOT_EXISTS = new ErrorCode(1_030_015_400, "厂商凭证不存在");
    ErrorCode IOT_VENDOR_ACCOUNT_DUPLICATE = new ErrorCode(1_030_015_401, "厂商账号【{}】已存在");
    ErrorCode IOT_DEVICE_BINDING_NOT_EXISTS = new ErrorCode(1_030_015_410, "设备绑定不存在");
    ErrorCode IOT_DEVICE_ALREADY_BOUND = new ErrorCode(1_030_015_411, "设备【{}】已绑定，不可重复绑定");
    ErrorCode IOT_LOOM_ALREADY_BOUND = new ErrorCode(1_030_015_412, "织机已被设备【{}】绑定，不可重复绑定");
    ErrorCode IOT_MACHINE_REALTIME_NOT_EXISTS = new ErrorCode(1_030_015_420, "设备实时快照不存在");

}

