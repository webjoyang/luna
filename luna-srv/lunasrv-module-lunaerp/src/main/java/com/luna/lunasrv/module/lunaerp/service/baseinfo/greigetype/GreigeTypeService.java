package com.luna.lunasrv.module.lunaerp.service.baseinfo.greigetype;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.greigetype.vo.GreigeTypePageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.greigetype.vo.GreigeTypeSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.greigetype.GreigeTypeDO;

import java.util.List;

/**
 * 胚布品种 Service 接口
 *
 * @author luna
 */
public interface GreigeTypeService {

    /**
     * 创建胚布品种
     *
     * @param createReqVO 创建信息
     * @return 胚布品种ID
     */
    Long createGreigeType(GreigeTypeSaveReqVO createReqVO);

    /**
     * 修改胚布品种
     *
     * @param updateReqVO 修改信息
     */
    void updateGreigeType(GreigeTypeSaveReqVO updateReqVO);

    /**
     * 删除胚布品种（逻辑删除）
     *
     * @param id 胚布品种ID
     */
    void deleteGreigeType(Long id);

    /**
     * 获得胚布品种
     *
     * @param id 胚布品种ID
     * @return 胚布品种
     */
    GreigeTypeDO getGreigeType(Long id);

    /**
     * 获得胚布品种分页
     *
     * @param pageReqVO 分页查询
     * @return 胚布品种分页
     */
    PageResult<GreigeTypeDO> getGreigeTypePage(GreigeTypePageReqVO pageReqVO);

    /**
     * 获得启用状态（ACTIVE）的胚布品种精简列表，供产品表单选择器使用
     *
     * @return 启用中的胚布品种列表
     */
    List<GreigeTypeDO> getActiveGreigeTypeList();

}
