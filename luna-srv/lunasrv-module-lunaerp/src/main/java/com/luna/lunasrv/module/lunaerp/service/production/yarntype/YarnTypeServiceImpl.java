package com.luna.lunasrv.module.lunaerp.service.production.yarntype;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.module.lunaerp.controller.production.yarntype.vo.YarnTypePageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarntype.vo.YarnTypeRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarntype.vo.YarnTypeSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarntype.YarnTypeDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarntype.YarnTypeMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.YARN_TYPE_NAME_DUPLICATE;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.YARN_TYPE_NOT_EXISTS;

/**
 * 生产管理-纱线材质 Service 实现类
 *
 * @author luna
 */
@Service
public class YarnTypeServiceImpl implements YarnTypeService {

    private static final String DEFAULT_STATUS = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";

    @Resource
    private YarnTypeMapper yarnTypeMapper;

    @Override
    public Long createYarnType(YarnTypeSaveReqVO createReqVO) {
        // 校验材质名唯一（含软删行，1_030_008_001）
        validateNameUnique(null, createReqVO.getName());

        YarnTypeDO yarnType = BeanUtils.toBean(createReqVO, YarnTypeDO.class);
        if (yarnType.getSort() == null) {
            yarnType.setSort(0);
        }
        if (!StringUtils.hasText(yarnType.getStatus())) {
            yarnType.setStatus(DEFAULT_STATUS);
        }
        yarnTypeMapper.insert(yarnType);
        return yarnType.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateYarnType(YarnTypeSaveReqVO updateReqVO) {
        // 校验存在（1_030_008_000）
        validateYarnTypeExists(updateReqVO.getId());
        // 校验材质名唯一（1_030_008_001）
        validateNameUnique(updateReqVO.getId(), updateReqVO.getName());

        YarnTypeDO updateObj = BeanUtils.toBean(updateReqVO, YarnTypeDO.class);
        yarnTypeMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteYarnType(Long id) {
        // 校验存在
        validateYarnTypeExists(id);
        // 先置 status='INACTIVE'
        YarnTypeDO updateObj = new YarnTypeDO();
        updateObj.setId(id);
        updateObj.setStatus(STATUS_INACTIVE);
        yarnTypeMapper.updateById(updateObj);
        // 逻辑删除
        yarnTypeMapper.deleteById(id);
    }

    @Override
    public YarnTypeRespVO getYarnType(Long id) {
        YarnTypeDO yarnType = yarnTypeMapper.selectById(id);
        if (yarnType == null) {
            throw exception(YARN_TYPE_NOT_EXISTS);
        }
        return BeanUtils.toBean(yarnType, YarnTypeRespVO.class);
    }

    @Override
    public PageResult<YarnTypeRespVO> getYarnTypePage(YarnTypePageReqVO pageReqVO) {
        PageResult<YarnTypeDO> pageResult = yarnTypeMapper.selectPage(pageReqVO);
        return BeanUtils.toBean(pageResult, YarnTypeRespVO.class);
    }

    // ==================== 私有方法 ====================

    private void validateYarnTypeExists(Long id) {
        if (id == null || yarnTypeMapper.selectById(id) == null) {
            throw exception(YARN_TYPE_NOT_EXISTS);
        }
    }

    private void validateNameUnique(Long id, String name) {
        if (!StringUtils.hasText(name)) {
            return;
        }
        YarnTypeDO existing = yarnTypeMapper.selectByNameIncludingDeleted(name);
        if (existing == null) {
            return;
        }
        // 如果存在且不是当前被更新的记录，则报错
        if (id == null || !existing.getId().equals(id)) {
            throw exception(YARN_TYPE_NAME_DUPLICATE, name);
        }
    }

}
