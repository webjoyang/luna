package com.luna.lunasrv.module.lunaerp.service.production.iot;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.binding.DeviceBindingRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.binding.DeviceBindingSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.iot.DeviceBindingDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.loom.LoomDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.iot.DeviceBindingMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.loom.LoomMapper;
import com.luna.lunasrv.module.lunaerp.service.production.iot.binding.DeviceBindingServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link DeviceBindingServiceImpl} 单元测试
 *
 * 覆盖关键业务行为（ADR-0028）：
 * 1. 织机存在性校验（不存在报 LOOM_NOT_EXISTS）
 * 2. 同设备重复绑定守卫（报 411 IOT_DEVICE_ALREADY_BOUND）
 * 3. 同织机重复绑定守卫（报 411 IOT_LOOM_ALREADY_BOUND）
 * 4. 正常绑定创建与修改
 * 5. 织机信息级联回填（loomNo、loomModel）
 *
 * @author luna
 */
class DeviceBindingServiceImplTest {

    @Mock
    private DeviceBindingMapper deviceBindingMapper;

    @Mock
    private LoomMapper loomMapper;

    @InjectMocks
    private DeviceBindingServiceImpl deviceBindingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), DeviceBindingDO.class);
        when(deviceBindingMapper.insert(any(DeviceBindingDO.class))).thenAnswer(invocation -> {
            DeviceBindingDO dobj = invocation.getArgument(0);
            dobj.setId(200L);
            return 1;
        });
    }

    private DeviceBindingSaveReqVO buildCreateReqVO() {
        DeviceBindingSaveReqVO req = new DeviceBindingSaveReqVO();
        req.setVendorDeviceId("DEV-YJ-001");
        req.setLoomId(10L);
        req.setStatus("ACTIVE");
        return req;
    }

    @Test
    void createDeviceBinding_loomNotExists_throwsLoomNotExists() {
        DeviceBindingSaveReqVO req = buildCreateReqVO();
        when(loomMapper.selectById(10L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                deviceBindingService.createDeviceBinding(req));

        assertEquals(LOOM_NOT_EXISTS.getCode(), ex.getCode());
        verify(deviceBindingMapper, never()).insert(any(DeviceBindingDO.class));
    }

    @Test
    void createDeviceBinding_duplicateDevice_throws411() {
        DeviceBindingSaveReqVO req = buildCreateReqVO();
        LoomDO loom = new LoomDO();
        loom.setId(10L);
        loom.setLoomNo("LM20260001");
        when(loomMapper.selectById(10L)).thenReturn(loom);

        DeviceBindingDO existingDev = new DeviceBindingDO();
        existingDev.setId(99L);
        existingDev.setVendorDeviceId("DEV-YJ-001");
        existingDev.setLoomId(5L);
        when(deviceBindingMapper.selectByVendorDeviceId("DEV-YJ-001")).thenReturn(existingDev);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                deviceBindingService.createDeviceBinding(req));

        assertEquals(IOT_DEVICE_ALREADY_BOUND.getCode(), ex.getCode());
        verify(deviceBindingMapper, never()).insert(any(DeviceBindingDO.class));
    }

    @Test
    void createDeviceBinding_duplicateLoom_throws411() {
        DeviceBindingSaveReqVO req = buildCreateReqVO();
        LoomDO loom = new LoomDO();
        loom.setId(10L);
        loom.setLoomNo("LM20260001");
        when(loomMapper.selectById(10L)).thenReturn(loom);
        when(deviceBindingMapper.selectByVendorDeviceId("DEV-YJ-001")).thenReturn(null);

        DeviceBindingDO existingLoom = new DeviceBindingDO();
        existingLoom.setId(98L);
        existingLoom.setVendorDeviceId("DEV-YJ-099");
        existingLoom.setLoomId(10L);
        when(deviceBindingMapper.selectByLoomId(10L)).thenReturn(existingLoom);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                deviceBindingService.createDeviceBinding(req));

        assertEquals(IOT_LOOM_ALREADY_BOUND.getCode(), ex.getCode());
        verify(deviceBindingMapper, never()).insert(any(DeviceBindingDO.class));
    }

    @Test
    void createDeviceBinding_success() {
        DeviceBindingSaveReqVO req = buildCreateReqVO();
        LoomDO loom = new LoomDO();
        loom.setId(10L);
        loom.setLoomNo("LM20260001");
        when(loomMapper.selectById(10L)).thenReturn(loom);
        when(deviceBindingMapper.selectByVendorDeviceId("DEV-YJ-001")).thenReturn(null);
        when(deviceBindingMapper.selectByLoomId(10L)).thenReturn(null);

        Long id = deviceBindingService.createDeviceBinding(req);

        assertEquals(200L, id);
        ArgumentCaptor<DeviceBindingDO> captor = ArgumentCaptor.forClass(DeviceBindingDO.class);
        verify(deviceBindingMapper).insert(captor.capture());
        DeviceBindingDO inserted = captor.getValue();
        assertEquals("DEV-YJ-001", inserted.getVendorDeviceId());
        assertEquals(10L, inserted.getLoomId());
        assertEquals("ACTIVE", inserted.getStatus());
    }

    @Test
    void updateDeviceBinding_duplicateDeviceOnOther_throws411() {
        DeviceBindingSaveReqVO req = buildCreateReqVO();
        req.setId(200L);

        DeviceBindingDO existingSelf = new DeviceBindingDO();
        existingSelf.setId(200L);
        existingSelf.setVendorDeviceId("DEV-OLD");
        existingSelf.setLoomId(10L);
        when(deviceBindingMapper.selectById(200L)).thenReturn(existingSelf);

        LoomDO loom = new LoomDO();
        loom.setId(10L);
        when(loomMapper.selectById(10L)).thenReturn(loom);

        DeviceBindingDO existingOther = new DeviceBindingDO();
        existingOther.setId(300L);
        existingOther.setVendorDeviceId("DEV-YJ-001");
        when(deviceBindingMapper.selectByVendorDeviceId("DEV-YJ-001")).thenReturn(existingOther);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                deviceBindingService.updateDeviceBinding(req));

        assertEquals(IOT_DEVICE_ALREADY_BOUND.getCode(), ex.getCode());
    }

    @Test
    void getByLoomId_success_enrichesLoomInfo() {
        DeviceBindingDO binding = new DeviceBindingDO();
        binding.setId(200L);
        binding.setVendorDeviceId("DEV-YJ-001");
        binding.setLoomId(10L);
        binding.setStatus("ACTIVE");
        when(deviceBindingMapper.selectByLoomId(10L)).thenReturn(binding);

        LoomDO loom = new LoomDO();
        loom.setId(10L);
        loom.setLoomNo("LM20260001");
        loom.setModel("34″·28G");
        when(loomMapper.selectBatchIds(Collections.singleton(10L))).thenReturn(Collections.singletonList(loom));

        DeviceBindingRespVO resp = deviceBindingService.getByLoomId(10L);

        assertNotNull(resp);
        assertEquals("DEV-YJ-001", resp.getVendorDeviceId());
        assertEquals("LM20260001", resp.getLoomNo());
        assertEquals("34″·28G", resp.getLoomModel());
    }

    @Test
    void deviceBinding_inheritsTenantBaseDO_andEnsuresAuthoritativeRouting() {
        DeviceBindingDO binding = new DeviceBindingDO();
        binding.setTenantId(1L);
        binding.setVendorDeviceId("DEV-TENANT-1");
        binding.setLoomId(10L);

        assertTrue(binding instanceof com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO,
                "DeviceBindingDO 必须继承 TenantBaseDO 保证多租户物理隔离与权威路由（ADR-0028）");
        assertEquals(1L, binding.getTenantId());
    }

}
