package com.luna.lunasrv.module.lunaerp.service.production.iot;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.realtime.MachineRealtimeRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.realtime.MachineRealtimeSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.iot.DeviceBindingDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.iot.MachineRealtimeDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.iot.DeviceBindingMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.iot.MachineRealtimeMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.loom.LoomMapper;
import com.luna.lunasrv.module.lunaerp.service.production.iot.collector.LoomDataCollector;
import com.luna.lunasrv.module.lunaerp.service.production.iot.realtime.MachineRealtimeServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link MachineRealtimeServiceImpl} 单元测试
 *
 * 覆盖关键业务行为（ADR-0028）：
 * 1. 手工录入快照：首次新增入库
 * 2. 手工录入快照：已存在时更新覆盖（只保留最新值）
 * 3. 严格信息源隔离：快照录入/更新【绝不回写】织机状态 loom.status
 * 4. 模拟采集：批量生成快照并更新绑定最后同步时间
 * 5. 织机ID权威路由最新快照查询
 *
 * @author luna
 */
class MachineRealtimeServiceImplTest {

    @Mock
    private MachineRealtimeMapper machineRealtimeMapper;

    @Mock
    private DeviceBindingMapper deviceBindingMapper;

    @Mock
    private LoomMapper loomMapper;

    @Mock
    private LoomDataCollector loomDataCollector;

    @InjectMocks
    private MachineRealtimeServiceImpl machineRealtimeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), MachineRealtimeDO.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), DeviceBindingDO.class);

        when(machineRealtimeMapper.insert(any(MachineRealtimeDO.class))).thenAnswer(invocation -> {
            MachineRealtimeDO dobj = invocation.getArgument(0);
            dobj.setId(300L);
            return 1;
        });
    }

    private MachineRealtimeSaveReqVO buildSaveReqVO() {
        MachineRealtimeSaveReqVO req = new MachineRealtimeSaveReqVO();
        req.setVendorDeviceId("DEV-YJ-001");
        req.setDeviceSn("SN-998822");
        req.setStatus("running");
        req.setRunEfficiencyPct(new BigDecimal("94.50"));
        req.setMinuteCircleSpeed(25);
        req.setShiftCircles(12000);
        req.setLimitSpeed(28);
        req.setRunMinutes(440);
        req.setShiftMinutes(480);
        req.setStaffOnShift("张工");
        return req;
    }

    @Test
    void recordRealtimeData_newDevice_insertsSnapshot_doesNotTouchLoomStatus() {
        MachineRealtimeSaveReqVO req = buildSaveReqVO();
        when(machineRealtimeMapper.selectByVendorDeviceId("DEV-YJ-001")).thenReturn(null);

        Long id = machineRealtimeService.recordRealtimeData(req);

        assertEquals(300L, id);
        ArgumentCaptor<MachineRealtimeDO> captor = ArgumentCaptor.forClass(MachineRealtimeDO.class);
        verify(machineRealtimeMapper).insert(captor.capture());
        MachineRealtimeDO inserted = captor.getValue();
        assertEquals("DEV-YJ-001", inserted.getVendorDeviceId());
        assertEquals("running", inserted.getStatus());
        assertEquals(new BigDecimal("94.50"), inserted.getRunEfficiencyPct());
        assertEquals(25, inserted.getMinuteCircleSpeed());

        // 核心断言：ADR-0028 规范：IoT 快照绝不回写 loom.status
        verifyNoInteractions(loomMapper);
    }

    @Test
    void recordRealtimeData_existingDevice_overwritesSnapshot_doesNotTouchLoomStatus() {
        MachineRealtimeSaveReqVO req = buildSaveReqVO();
        req.setStatus("idle");
        req.setRunEfficiencyPct(BigDecimal.ZERO);

        MachineRealtimeDO existing = new MachineRealtimeDO();
        existing.setId(300L);
        existing.setVendorDeviceId("DEV-YJ-001");
        existing.setStatus("running");
        existing.setRunEfficiencyPct(new BigDecimal("94.50"));
        when(machineRealtimeMapper.selectByVendorDeviceId("DEV-YJ-001")).thenReturn(existing);

        Long id = machineRealtimeService.recordRealtimeData(req);

        assertEquals(300L, id);
        verify(machineRealtimeMapper).updateById(existing);
        assertEquals("idle", existing.getStatus(), "最新快照应被覆盖为当前值");
        assertEquals(BigDecimal.ZERO, existing.getRunEfficiencyPct());

        // 核心断言：ADR-0028 规范：IoT 快照绝不回写 loom.status
        verifyNoInteractions(loomMapper);
    }

    @Test
    void triggerSimulatedCollection_updatesSnapshotsAndSyncTime() {
        MachineRealtimeDO snapshot = new MachineRealtimeDO();
        snapshot.setVendorDeviceId("DEV-YJ-001");
        snapshot.setStatus("running");
        snapshot.setRunEfficiencyPct(new BigDecimal("92.00"));
        snapshot.setMinuteCircleSpeed(24);
        snapshot.setFetchedAt(LocalDateTime.now());

        when(loomDataCollector.collectAll()).thenReturn(Collections.singletonList(snapshot));
        when(machineRealtimeMapper.selectByVendorDeviceId("DEV-YJ-001")).thenReturn(null);

        DeviceBindingDO binding = new DeviceBindingDO();
        binding.setId(200L);
        binding.setVendorDeviceId("DEV-YJ-001");
        when(deviceBindingMapper.selectByVendorDeviceId("DEV-YJ-001")).thenReturn(binding);

        int count = machineRealtimeService.triggerSimulatedCollection();

        assertEquals(1, count);
        verify(machineRealtimeMapper).insert(snapshot);
        verify(deviceBindingMapper).updateById(binding);
        assertNotNull(binding.getLastSyncAt(), "模拟采集应更新绑定的最后同步时间");
    }

    @Test
    void getLatestByLoomId_routesViaBinding() {
        DeviceBindingDO binding = new DeviceBindingDO();
        binding.setId(200L);
        binding.setVendorDeviceId("DEV-YJ-001");
        binding.setLoomId(10L);
        when(deviceBindingMapper.selectByLoomId(10L)).thenReturn(binding);

        MachineRealtimeDO realtime = new MachineRealtimeDO();
        realtime.setId(300L);
        realtime.setVendorDeviceId("DEV-YJ-001");
        realtime.setStatus("running");
        realtime.setRunEfficiencyPct(new BigDecimal("96.00"));
        when(machineRealtimeMapper.selectByVendorDeviceId("DEV-YJ-001")).thenReturn(realtime);

        MachineRealtimeRespVO resp = machineRealtimeService.getLatestByLoomId(10L);

        assertNotNull(resp);
        assertEquals("DEV-YJ-001", resp.getVendorDeviceId());
        assertEquals("running", resp.getStatus());
        assertEquals(new BigDecimal("96.00"), resp.getRunEfficiencyPct());
    }

}
