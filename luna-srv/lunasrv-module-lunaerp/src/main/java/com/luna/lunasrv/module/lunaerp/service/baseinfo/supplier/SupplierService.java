package com.luna.lunasrv.module.lunaerp.service.baseinfo.supplier;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.supplier.vo.SupplierPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.supplier.vo.SupplierRespVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.supplier.vo.SupplierSaveReqVO;

import java.util.List;

/**
 * 基本信息-供应商信息 Service 接口
 *
 * @author luna
 */
public interface SupplierService {

    /**
     * 创建供应商：supplierNo 为空时自动生成（SUP+年份+4位序号），手填查重含已删除行；
     * shortName 为空时用 companyName 回填，status 缺省 ACTIVE。
     *
     * @param createReqVO 供应商
     * @return 供应商ID
     */
    Long createSupplier(SupplierSaveReqVO createReqVO);

    /**
     * 获得供应商分页（支持 search 智能词 + supplierType + status 过滤）
     *
     * @param reqVO 分页条件
     * @return 供应商分页
     */
    PageResult<SupplierRespVO> getSupplierPage(SupplierPageReqVO reqVO);

    /**
     * 获得供应商详情
     *
     * @param id 供应商ID
     * @return 供应商
     */
    SupplierRespVO getSupplier(Long id);

    /**
     * 修改供应商：编号变更时全量查重（排除自身，含已删除行）；
     * shortName 为空时用 companyName 回填（与创建一致）。
     *
     * @param updateReqVO 供应商
     */
    void updateSupplier(SupplierSaveReqVO updateReqVO);

    /**
     * 删除供应商：先置 status='INACTIVE'，再逻辑删除（deleted=1，与客户模块模式一致）。
     *
     * @param id 供应商ID
     */
    void deleteSupplier(Long id);

    /**
     * 批量删除供应商：先置 status='INACTIVE'，再逻辑删除。
     *
     * @param ids 供应商ID列表
     */
    void deleteSupplierList(List<Long> ids);

    /**
     * 预览当前租户当年下一个供应商编号（只读、不占号）：
     * 复用自动取号的扫描结果（含已删除行，ADR-0005），不做撞号查重、不写库；
     * 无新建时多次调用返回相同值。
     *
     * @return 下一个供应商编号，如 SUP20260001
     */
    String getNextSupplierNo();

    /**
     * 获得供应商导入模板数据
     *
     * @return 模板数据
     */
    List<com.luna.lunasrv.module.lunaerp.controller.baseinfo.supplier.vo.SupplierImportVO> getImportTemplate();

    /**
     * 导入供应商
     *
     * @param file Excel 文件
     * @return 导入结果
     * @throws java.io.IOException 读取异常
     */
    com.luna.lunasrv.module.lunaerp.common.dataimport.ExcelImportResult importSupplier(org.springframework.web.multipart.MultipartFile file) throws java.io.IOException;

}
