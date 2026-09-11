package com.luna.lunasrv.module.system.service.logger;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.system.api.logger.dto.LoginLogCreateReqDTO;
import com.luna.lunasrv.module.system.controller.admin.logger.vo.loginlog.LoginLogPageReqVO;
import com.luna.lunasrv.module.system.dal.dataobject.logger.LoginLogDO;

import jakarta.validation.Valid;

/**
 * 登录日志 Service 接口
 */
public interface LoginLogService {

    /**
     * 获得登录日志
     *
     * @param id 编号
     * @return 登录日志
     */
    LoginLogDO getLoginLog(Long id);

    /**
     * 获得登录日志分页
     *
     * @param pageReqVO 分页条件
     * @return 登录日志分页
     */
    PageResult<LoginLogDO> getLoginLogPage(LoginLogPageReqVO pageReqVO);

    /**
     * 创建登录日志
     *
     * @param reqDTO 日志信息
     */
    void createLoginLog(@Valid LoginLogCreateReqDTO reqDTO);

}
