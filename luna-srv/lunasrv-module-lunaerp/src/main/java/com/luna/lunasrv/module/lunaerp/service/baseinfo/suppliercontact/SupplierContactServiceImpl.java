package com.luna.lunasrv.module.lunaerp.service.baseinfo.suppliercontact;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.suppliercontact.vo.SupplierContactPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.suppliercontact.vo.SupplierContactRespVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.suppliercontact.vo.SupplierContactSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.suppliercontact.SupplierContactDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.suppliercontact.SupplierContactMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.luna.lunasrv.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.SUPPLIER_CONTACT_NOT_EXISTS;

/**
 * 基本信息-供应商联系人 Service 实现类
 *
 * supplierId 与 name 必填由 Controller 层 @Valid 校验 VO 注解；
 * isPrimary 无单点约束，业务允许同供应商多条主联系人。
 *
 * @author luna
 */
@Service
public class SupplierContactServiceImpl implements SupplierContactService {

    @Resource
    private SupplierContactMapper supplierContactMapper;

    @Override
    public Long createSupplierContact(SupplierContactSaveReqVO createReqVO) {
        validateSupplierContactNotMasked(createReqVO);
        SupplierContactDO contact = BeanUtils.toBean(createReqVO, SupplierContactDO.class);
        supplierContactMapper.insert(contact);
        return contact.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSupplierContact(SupplierContactSaveReqVO updateReqVO) {
        // 校验存在
        validateSupplierContactExists(updateReqVO.getId());
        validateSupplierContactNotMasked(updateReqVO);
        // 更新
        SupplierContactDO updateObj = BeanUtils.toBean(updateReqVO, SupplierContactDO.class);
        supplierContactMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSupplierContact(Long id) {
        // 校验存在
        validateSupplierContactExists(id);
        // 逻辑删除（deleted=1）
        supplierContactMapper.deleteById(id);
    }

    @Override
    public SupplierContactRespVO getSupplierContact(Long id) {
        SupplierContactDO contact = supplierContactMapper.selectById(id);
        if (contact == null) {
            throw exception(SUPPLIER_CONTACT_NOT_EXISTS);
        }
        return BeanUtils.toBean(contact, SupplierContactRespVO.class);
    }

    @Override
    public PageResult<SupplierContactRespVO> getSupplierContactPage(SupplierContactPageReqVO reqVO) {
        PageResult<SupplierContactDO> pageResult = supplierContactMapper.selectPage(reqVO);
        return BeanUtils.toBean(pageResult, SupplierContactRespVO.class);
    }

    // ==================== 私有方法 ====================

    /**
     * 校验联系人存在，不存在抛 1_030_005_002
     */
    private void validateSupplierContactExists(Long id) {
        if (id == null || supplierContactMapper.selectById(id) == null) {
            throw exception(SUPPLIER_CONTACT_NOT_EXISTS);
        }
    }

    /**
     * 校验联系人字段不含脱敏占位符，写接口不接受脱敏值入库
     */
    private void validateSupplierContactNotMasked(SupplierContactSaveReqVO reqVO) {
        if (reqVO == null) {
            return;
        }
        if (reqVO.getName() != null && reqVO.getName().contains("*")) {
            throw exception(BAD_REQUEST, "联系人姓名不能包含脱敏字符");
        }
        if (reqVO.getPhone() != null && reqVO.getPhone().contains("*")) {
            throw exception(BAD_REQUEST, "联系电话不能包含脱敏字符");
        }
        if (reqVO.getEmail() != null && reqVO.getEmail().contains("*")) {
            throw exception(BAD_REQUEST, "邮箱不能包含脱敏字符");
        }
    }

}
