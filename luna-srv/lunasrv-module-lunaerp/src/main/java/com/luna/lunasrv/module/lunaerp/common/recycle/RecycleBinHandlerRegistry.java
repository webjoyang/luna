package com.luna.lunasrv.module.lunaerp.common.recycle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_BIZ_TYPE_NOT_SUPPORTED;

/**
 * 回收站处理器注册表
 *
 * 启动时收集 Spring 容器中的所有 {@link RecycleBinHandler} 实现，按 bizType 路由。
 *
 * @author luna
 */
@Component
@Slf4j
public class RecycleBinHandlerRegistry {

    private final Map<String, RecycleBinHandler> handlerMap = new ConcurrentHashMap<>();

    public RecycleBinHandlerRegistry(List<RecycleBinHandler> handlers) {
        if (handlers != null) {
            for (RecycleBinHandler handler : handlers) {
                handlerMap.put(handler.getBizType(), handler);
                log.info("[RecycleBinHandlerRegistry] 注册回收站处理器: bizType={}", handler.getBizType());
            }
        }
    }

    /**
     * 获取指定 bizType 的处理器
     *
     * @param bizType 业务类型
     * @return 处理器
     */
    public RecycleBinHandler getHandler(String bizType) {
        RecycleBinHandler handler = handlerMap.get(bizType);
        if (handler == null) {
            throw exception(RECYCLE_BIN_BIZ_TYPE_NOT_SUPPORTED, bizType);
        }
        return handler;
    }

    /**
     * 获取所有已注册处理器
     *
     * @return 处理器列表
     */
    public List<RecycleBinHandler> getAllHandlers() {
        return new ArrayList<>(handlerMap.values());
    }

}
