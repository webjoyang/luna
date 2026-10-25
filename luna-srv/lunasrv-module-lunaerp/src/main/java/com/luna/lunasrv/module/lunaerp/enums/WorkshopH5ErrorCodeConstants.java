package com.luna.lunasrv.module.lunaerp.enums;

import com.luna.lunasrv.framework.common.exception.ErrorCode;

/**
 * 车间移动 H5 错误码枚举
 *
 * 使用 1-030-039-xxx 段（车间移动 H5 接入层，见 ADR-0045 与 shared GLOSSARY）
 * 业务守卫复用生产域 015（布票/领纱）、010（检验）既有错误码
 *
 * @author luna
 */
public interface WorkshopH5ErrorCodeConstants {

    // ========== 生产管理 - 车间移动 H5 1-030-039-001 ~ 009 ==========
    /**
     * 039-001 布票不存在或已作废（扫码查无此票，含租户隔离落空，统一文案防枚举）
     */
    ErrorCode H5_WEAVING_TICKET_NOT_EXISTS = new ErrorCode(1_030_039_001, "布票不存在或已作废");

    /**
     * 039-002 该机台任务已派给其他挡车工，无权操作（请联系领班在 PC 改派；含自助认领并发落败）
     */
    ErrorCode H5_MACHINE_TASK_ASSIGNED_TO_OTHER = new ErrorCode(1_030_039_002, "该机台任务已派给其他挡车工，无权操作");

    /**
     * 039-003 布票尚未完工，不能扫码质检（请先由挡车工报工完工）
     */
    ErrorCode H5_TICKET_NOT_COMPLETED = new ErrorCode(1_030_039_003, "布票尚未完工，不能扫码质检");

    /**
     * 039-004 布票当前状态不可录检
     */
    ErrorCode H5_TICKET_STATUS_NOT_ALLOW_INSPECTION = new ErrorCode(1_030_039_004, "布票当前状态不可录检");

    /**
     * 039-005 该缸号+卷号已存在录检记录（返回已存在行，前端定位改录，非致命）
     */
    ErrorCode H5_INSPECTION_ITEM_DUPLICATE = new ErrorCode(1_030_039_005, "该缸号+卷号已存在录检记录");

    /**
     * 039-006 领纱单不属于你的机台任务，无权操作
     */
    ErrorCode H5_YARN_REQUISITION_NOT_OWNED = new ErrorCode(1_030_039_006, "领纱单不属于你的机台任务，无权操作");

    /**
     * 039-007 未配置车间默认发纱仓库（保留段，前端通常在选择器侧引导，不阻断）
     */
    ErrorCode H5_DEFAULT_WAREHOUSE_NOT_CONFIGURED = new ErrorCode(1_030_039_007, "未配置车间默认发纱仓库");

    /**
     * 039-008 照片数量超过 6 张或含非图片文件
     */
    ErrorCode H5_PHOTO_COUNT_EXCEED_OR_INVALID = new ErrorCode(1_030_039_008, "照片数量超过 6 张或含非图片文件");

    /**
     * 039-009 起预留
     */
    ErrorCode H5_RESERVED_009 = new ErrorCode(1_030_039_009, "预留");

}
