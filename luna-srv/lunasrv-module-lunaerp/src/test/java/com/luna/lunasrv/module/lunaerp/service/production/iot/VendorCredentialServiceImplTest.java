package com.luna.lunasrv.module.lunaerp.service.production.iot;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.credential.VendorCredentialPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.credential.VendorCredentialRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.credential.VendorCredentialSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.iot.VendorCredentialDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.iot.VendorCredentialMapper;
import com.luna.lunasrv.module.lunaerp.service.production.iot.credential.VendorCredentialServiceImpl;
import com.luna.lunasrv.module.lunaerp.service.production.iot.crypto.IotCryptoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;

import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.IOT_VENDOR_ACCOUNT_DUPLICATE;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.IOT_VENDOR_CREDENTIAL_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link VendorCredentialServiceImpl} 单元测试
 *
 * 覆盖关键业务行为（ADR-0028）：
 * 1. 密码加密存储：写入即加密，禁止明文入库
 * 2. 明文密码不回显：RespVO 不泄露明文
 * 3. 租户内厂商账号唯一性守卫（重复报 401）
 * 4. 修改时保留旧密码/覆盖新密码
 * 5. 采集端解密方法
 *
 * @author luna
 */
class VendorCredentialServiceImplTest {

    @Mock
    private VendorCredentialMapper vendorCredentialMapper;

    @InjectMocks
    private VendorCredentialServiceImpl vendorCredentialService;

    private IotCryptoHelper iotCryptoHelper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), VendorCredentialDO.class);
        iotCryptoHelper = new IotCryptoHelper("XDV71a+xqStEA3WH");
        ReflectionTestUtils.setField(vendorCredentialService, "iotCryptoHelper", iotCryptoHelper);

        when(vendorCredentialMapper.insert(any(VendorCredentialDO.class))).thenAnswer(invocation -> {
            VendorCredentialDO dobj = invocation.getArgument(0);
            dobj.setId(100L);
            return 1;
        });
    }

    private VendorCredentialSaveReqVO buildCreateReqVO() {
        VendorCredentialSaveReqVO req = new VendorCredentialSaveReqVO();
        req.setVendorAccount("vendor_test_001");
        req.setVendorPassword("SecretPassword999");
        req.setScope("车间一区织机");
        req.setStatus("ACTIVE");
        return req;
    }

    @Test
    void createVendorCredential_encryptsPassword_andStoresCiphertext() {
        VendorCredentialSaveReqVO req = buildCreateReqVO();
        when(vendorCredentialMapper.selectByVendorAccountIncludingDeleted("vendor_test_001")).thenReturn(null);

        Long id = vendorCredentialService.createVendorCredential(req);

        assertEquals(100L, id);
        ArgumentCaptor<VendorCredentialDO> captor = ArgumentCaptor.forClass(VendorCredentialDO.class);
        verify(vendorCredentialMapper).insert(captor.capture());
        VendorCredentialDO inserted = captor.getValue();

        assertEquals("vendor_test_001", inserted.getVendorAccount());
        assertNotEquals("SecretPassword999", inserted.getVendorPasswordEnc(), "密码必须加密存储，不能存明文");
        assertTrue(inserted.getVendorPasswordEnc().length() > 10);
        assertEquals("SecretPassword999", iotCryptoHelper.decrypt(inserted.getVendorPasswordEnc()), "解密后应等于原明文");
        assertEquals("ACTIVE", inserted.getStatus());
    }

    @Test
    void createVendorCredential_duplicateAccount_throws401() {
        VendorCredentialSaveReqVO req = buildCreateReqVO();
        VendorCredentialDO existing = new VendorCredentialDO();
        existing.setId(99L);
        existing.setVendorAccount("vendor_test_001");
        when(vendorCredentialMapper.selectByVendorAccountIncludingDeleted("vendor_test_001")).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                vendorCredentialService.createVendorCredential(req));

        assertEquals(IOT_VENDOR_ACCOUNT_DUPLICATE.getCode(), ex.getCode());
        verify(vendorCredentialMapper, never()).insert(any(VendorCredentialDO.class));
    }

    @Test
    void getVendorCredential_doesNotExposePlaintextPassword() {
        VendorCredentialDO existing = new VendorCredentialDO();
        existing.setId(100L);
        existing.setVendorAccount("vendor_test_001");
        existing.setVendorPasswordEnc(iotCryptoHelper.encrypt("SecretPassword999"));
        existing.setScope("车间一区织机");
        existing.setStatus("ACTIVE");
        existing.setLastSyncAt(LocalDateTime.now());
        when(vendorCredentialMapper.selectById(100L)).thenReturn(existing);

        VendorCredentialRespVO respVO = vendorCredentialService.getVendorCredential(100L);

        assertNotNull(respVO);
        assertEquals(100L, respVO.getId());
        assertEquals("vendor_test_001", respVO.getVendorAccount());
        assertEquals("车间一区织机", respVO.getScope());
        assertTrue(respVO.getHasPassword(), "应标识已设置密码");

        // 验证采集端解密方法
        String decrypted = vendorCredentialService.getDecryptedPassword(100L);
        assertEquals("SecretPassword999", decrypted);
    }

    @Test
    void updateVendorCredential_withoutNewPassword_preservesOldPassword() {
        VendorCredentialDO existing = new VendorCredentialDO();
        existing.setId(100L);
        existing.setVendorAccount("vendor_test_001");
        String oldEncrypted = iotCryptoHelper.encrypt("OriginalPass123");
        existing.setVendorPasswordEnc(oldEncrypted);
        existing.setScope("旧范围");
        existing.setStatus("ACTIVE");
        when(vendorCredentialMapper.selectById(100L)).thenReturn(existing);
        when(vendorCredentialMapper.selectByVendorAccountIncludingDeleted("vendor_test_001")).thenReturn(existing);

        VendorCredentialSaveReqVO updateReq = new VendorCredentialSaveReqVO();
        updateReq.setId(100L);
        updateReq.setVendorAccount("vendor_test_001");
        updateReq.setVendorPassword(null); // 不修改密码
        updateReq.setScope("新范围");
        updateReq.setStatus("INACTIVE");

        vendorCredentialService.updateVendorCredential(updateReq);

        verify(vendorCredentialMapper).updateById(existing);
        assertEquals("新范围", existing.getScope());
        assertEquals("INACTIVE", existing.getStatus());
        assertEquals(oldEncrypted, existing.getVendorPasswordEnc(), "留空密码时原加密密码应保持不变");
    }

    @Test
    void updateVendorCredential_withNewPassword_encryptsAndUpdates() {
        VendorCredentialDO existing = new VendorCredentialDO();
        existing.setId(100L);
        existing.setVendorAccount("vendor_test_001");
        existing.setVendorPasswordEnc(iotCryptoHelper.encrypt("OldPass"));
        when(vendorCredentialMapper.selectById(100L)).thenReturn(existing);
        when(vendorCredentialMapper.selectByVendorAccountIncludingDeleted("vendor_test_001")).thenReturn(existing);

        VendorCredentialSaveReqVO updateReq = new VendorCredentialSaveReqVO();
        updateReq.setId(100L);
        updateReq.setVendorAccount("vendor_test_001");
        updateReq.setVendorPassword("NewBrandPass456");
        updateReq.setScope("车间更新");
        updateReq.setStatus("ACTIVE");

        vendorCredentialService.updateVendorCredential(updateReq);

        verify(vendorCredentialMapper).updateById(existing);
        assertEquals("NewBrandPass456", iotCryptoHelper.decrypt(existing.getVendorPasswordEnc()));
    }

    @Test
    void deleteVendorCredential_success() {
        VendorCredentialDO existing = new VendorCredentialDO();
        existing.setId(100L);
        when(vendorCredentialMapper.selectById(100L)).thenReturn(existing);

        vendorCredentialService.deleteVendorCredential(100L);

        verify(vendorCredentialMapper).deleteById(100L);
    }

    @Test
    void deleteVendorCredential_notExists_throws() {
        when(vendorCredentialMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                vendorCredentialService.deleteVendorCredential(999L));
        assertEquals(IOT_VENDOR_CREDENTIAL_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void vendorCredential_inheritsTenantBaseDO_ensuresTenantIsolation() {
        VendorCredentialDO credential = new VendorCredentialDO();
        credential.setTenantId(1L);
        credential.setVendorAccount("tenant1_admin");

        assertTrue(credential instanceof com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO,
                "VendorCredentialDO 必须继承 TenantBaseDO 保证多租户物理隔离（ADR-0028）");
        assertEquals(1L, credential.getTenantId());
    }

}
