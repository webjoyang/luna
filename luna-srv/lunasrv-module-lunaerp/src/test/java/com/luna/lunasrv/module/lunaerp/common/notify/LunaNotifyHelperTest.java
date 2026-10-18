package com.luna.lunasrv.module.lunaerp.common.notify;

import com.luna.lunasrv.module.system.api.notify.NotifyMessageSendApi;
import com.luna.lunasrv.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link LunaNotifyHelper} 单元测试
 *
 * 覆盖：
 * 1. 站内消息基础发送与参数合并（templateParams 携带 bizType/bizId/bizNo）
 * 2. 审批到达待办消息（逐级与会签每人一条）
 * 3. 审批通过消息
 * 4. 审批驳回消息（内容携带驳回意见）
 * 5. 发送失败异常被吞并容忍（记录 error 日志，不抛出异常，不阻塞/回滚业务事务）
 * 6. 模板枚举与契约位验证
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("横切基础 - LunaNotifyHelper 站内信通知助手测试")
public class LunaNotifyHelperTest {

    @InjectMocks
    private LunaNotifyHelper lunaNotifyHelper;

    @Mock
    private NotifyMessageSendApi notifyMessageSendApi;

    @Test
    @DisplayName("基础发送：消息 templateParams 正确合并业务参数与 bizJump")
    public void testSend_Success() {
        // 准备参数
        Long userId = 1001L;
        String templateCode = "LUNA_APPROVAL_TODO";
        Map<String, Object> params = new HashMap<>();
        params.put("bizName", "销售合同");
        params.put("bizNo", "SC-2026-001");
        params.put("submitterName", "张三");

        BizJump bizJump = BizJump.builder()
                .bizType("APPROVAL")
                .bizId(5001L)
                .bizNo("SC-2026-001")
                .build();

        when(notifyMessageSendApi.sendSingleMessageToAdmin(any(NotifySendSingleToUserReqDTO.class)))
                .thenReturn(9999L);

        // 执行
        Long messageId = lunaNotifyHelper.send(userId, templateCode, params, bizJump);

        // 校验
        assertEquals(9999L, messageId);

        ArgumentCaptor<NotifySendSingleToUserReqDTO> captor = ArgumentCaptor.forClass(NotifySendSingleToUserReqDTO.class);
        verify(notifyMessageSendApi).sendSingleMessageToAdmin(captor.capture());

        NotifySendSingleToUserReqDTO reqDTO = captor.getValue();
        assertEquals(userId, reqDTO.getUserId());
        assertEquals(templateCode, reqDTO.getTemplateCode());
        assertNotNull(reqDTO.getTemplateParams());

        // 断言占位符参数存在
        assertEquals("销售合同", reqDTO.getTemplateParams().get("bizName"));
        assertEquals("SC-2026-001", reqDTO.getTemplateParams().get("bizNo"));
        assertEquals("张三", reqDTO.getTemplateParams().get("submitterName"));

        // 断言固定携带的 bizJump 参数
        assertEquals("APPROVAL", reqDTO.getTemplateParams().get("bizType"));
        assertEquals(5001L, reqDTO.getTemplateParams().get("bizId"));
        assertEquals("SC-2026-001", reqDTO.getTemplateParams().get("bizNo"));
    }

    @Test
    @DisplayName("使用枚举调用：与 String code 保持一致")
    public void testSend_WithEnum() {
        Long userId = 1002L;
        Map<String, Object> params = new HashMap<>();
        params.put("bizName", "付款申请");
        params.put("bizNo", "PAY-002");

        BizJump bizJump = BizJump.ofApproval(6001L, "PAY-002");

        when(notifyMessageSendApi.sendSingleMessageToAdmin(any())).thenReturn(8888L);

        Long messageId = lunaNotifyHelper.send(userId, LunaNotifyTemplateEnum.LUNA_APPROVAL_APPROVED, params, bizJump);

        assertEquals(8888L, messageId);
        ArgumentCaptor<NotifySendSingleToUserReqDTO> captor = ArgumentCaptor.forClass(NotifySendSingleToUserReqDTO.class);
        verify(notifyMessageSendApi).sendSingleMessageToAdmin(captor.capture());
        assertEquals("LUNA_APPROVAL_APPROVED", captor.getValue().getTemplateCode());
    }

    @Test
    @DisplayName("审批待办通知：单人与会签批量（每人各发一条）")
    public void testSendApprovalTodo_AndBatch() {
        when(notifyMessageSendApi.sendSingleMessageToAdmin(any())).thenReturn(101L, 102L, 103L);

        // 单人审批待办
        Long id1 = lunaNotifyHelper.sendApprovalTodo(11L, "APPROVAL", 2001L, "SC-001", "销售合同", "李四");
        assertEquals(101L, id1);

        // 会签节点多位审批人：每人各一条
        List<Long> approvers = List.of(12L, 13L);
        List<Long> msgIds = lunaNotifyHelper.sendApprovalTodoBatch(approvers, "APPROVAL", 2001L, "SC-001", "销售合同", "李四");
        assertEquals(2, msgIds.size());
        assertEquals(List.of(102L, 103L), msgIds);

        verify(notifyMessageSendApi, times(3)).sendSingleMessageToAdmin(any());
    }

    @Test
    @DisplayName("审批通过通知：正确包含单号与实例跳转参数")
    public void testSendApprovalApproved() {
        when(notifyMessageSendApi.sendSingleMessageToAdmin(any())).thenReturn(301L);

        Long msgId = lunaNotifyHelper.sendApprovalApproved(88L, "APPROVAL", 2001L, "SC-001", "销售合同");
        assertEquals(301L, msgId);

        ArgumentCaptor<NotifySendSingleToUserReqDTO> captor = ArgumentCaptor.forClass(NotifySendSingleToUserReqDTO.class);
        verify(notifyMessageSendApi).sendSingleMessageToAdmin(captor.capture());

        NotifySendSingleToUserReqDTO req = captor.getValue();
        assertEquals(88L, req.getUserId());
        assertEquals(LunaNotifyTemplateEnum.LUNA_APPROVAL_APPROVED.getCode(), req.getTemplateCode());
        assertEquals("销售合同", req.getTemplateParams().get("bizName"));
        assertEquals("SC-001", req.getTemplateParams().get("bizNo"));
        assertEquals(2001L, req.getTemplateParams().get("bizId"));
    }

    @Test
    @DisplayName("审批驳回通知：正确包含驳回意见与单号")
    public void testSendApprovalRejected() {
        when(notifyMessageSendApi.sendSingleMessageToAdmin(any())).thenReturn(401L);

        Long msgId = lunaNotifyHelper.sendApprovalRejected(88L, "APPROVAL", 2001L, "SC-001", "销售合同", "价格不符合标准，需打折重提");
        assertEquals(401L, msgId);

        ArgumentCaptor<NotifySendSingleToUserReqDTO> captor = ArgumentCaptor.forClass(NotifySendSingleToUserReqDTO.class);
        verify(notifyMessageSendApi).sendSingleMessageToAdmin(captor.capture());

        NotifySendSingleToUserReqDTO req = captor.getValue();
        assertEquals(88L, req.getUserId());
        assertEquals(LunaNotifyTemplateEnum.LUNA_APPROVAL_REJECTED.getCode(), req.getTemplateCode());
        assertEquals("销售合同", req.getTemplateParams().get("bizName"));
        assertEquals("SC-001", req.getTemplateParams().get("bizNo"));
        assertEquals("价格不符合标准，需打折重提", req.getTemplateParams().get("reason"));
        assertEquals(2001L, req.getTemplateParams().get("bizId"));
    }

    @Test
    @DisplayName("容错守卫：消息底层异常仅记录日志，不向外抛出异常，不破坏业务事务")
    public void testSend_FailureTolerance() {
        // 模拟底层接口报错（如网络抖动、模板缺失、租户不存在等）
        when(notifyMessageSendApi.sendSingleMessageToAdmin(any()))
                .thenThrow(new RuntimeException("模拟底座站内信服务异常"));

        // 调用应安全返回 null，不抛出任何异常
        assertDoesNotThrow(() -> {
            Long result = lunaNotifyHelper.send(1001L, "LUNA_APPROVAL_TODO", Collections.emptyMap(), null);
            assertNull(result);
        });

        // 验证批量调用同样安全
        assertDoesNotThrow(() -> {
            List<Long> results = lunaNotifyHelper.sendApprovalTodoBatch(
                    List.of(1001L, 1002L), "APPROVAL", 1L, "NO-1", "测试", "发起人");
            assertNotNull(results);
            assertTrue(results.isEmpty());
        });
    }

    @Test
    @DisplayName("事务安全：业务方法调用消息助手，即便消息发送异常，业务后续逻辑照常执行不中断")
    public void testSend_CallerBusinessTransactionNotAborted() {
        when(notifyMessageSendApi.sendSingleMessageToAdmin(any()))
                .thenThrow(new RuntimeException("模拟站内信数据库异常"));

        // 模拟业务流程
        boolean businessOperationCompleted = false;
        lunaNotifyHelper.sendApprovalTodo(101L, "APPROVAL", 1001L, "SC-001", "销售合同", "发起人");
        businessOperationCompleted = true;

        assertTrue(businessOperationCompleted, "业务流程应不受消息失败影响正常完成");
    }

    @Test
    @DisplayName("参数防御：非法参数直接安全返回 null")
    public void testSend_InvalidArgs() {
        assertNull(lunaNotifyHelper.send(null, "LUNA_APPROVAL_TODO", null, null));
        assertNull(lunaNotifyHelper.send(1001L, (String) null, null, null));
        assertNull(lunaNotifyHelper.send(1001L, "", null, null));
        verify(notifyMessageSendApi, never()).sendSingleMessageToAdmin(any());
    }

    @Test
    @DisplayName("对账单驳回通知：参数与 bizJump 正确传递")
    public void testSendStatementRejected() {
        Long creatorUserId = 2048L;
        Long statementId = 100L;
        String statementNo = "CS20260001";
        String customerName = "测试客户有限公司";
        String reason = "明细金额有误";

        when(notifyMessageSendApi.sendSingleMessageToAdmin(any())).thenReturn(7777L);

        Long msgId = lunaNotifyHelper.sendStatementRejected(creatorUserId, statementId, statementNo, customerName, reason);
        assertEquals(7777L, msgId);

        ArgumentCaptor<NotifySendSingleToUserReqDTO> captor = ArgumentCaptor.forClass(NotifySendSingleToUserReqDTO.class);
        verify(notifyMessageSendApi).sendSingleMessageToAdmin(captor.capture());
        NotifySendSingleToUserReqDTO req = captor.getValue();
        assertEquals(creatorUserId, req.getUserId());
        assertEquals("LUNA_STATEMENT_REJECTED", req.getTemplateCode());
        assertEquals(statementNo, req.getTemplateParams().get("bizNo"));
        assertEquals(customerName, req.getTemplateParams().get("customerName"));
        assertEquals(reason, req.getTemplateParams().get("reason"));
        assertEquals("STATEMENT", req.getTemplateParams().get("bizType"));
        assertEquals(statementId, req.getTemplateParams().get("bizId"));
    }

    @Test
    @DisplayName("模板枚举与冻结契约位校验")
    public void testTemplateEnumContracts() {
        // 审批模板
        assertEquals("LUNA_APPROVAL_TODO", LunaNotifyTemplateEnum.LUNA_APPROVAL_TODO.getCode());
        assertEquals("LUNA_APPROVAL_APPROVED", LunaNotifyTemplateEnum.LUNA_APPROVAL_APPROVED.getCode());
        assertEquals("LUNA_APPROVAL_REJECTED", LunaNotifyTemplateEnum.LUNA_APPROVAL_REJECTED.getCode());

        // 对账单驳回模板
        assertEquals("LUNA_STATEMENT_REJECTED", LunaNotifyTemplateEnum.LUNA_STATEMENT_REJECTED.getCode());

        // 冻结契约位（二期接线）
        assertEquals("LUNA_SALES_CONTRACT_ACTIVE", LunaNotifyTemplateEnum.LUNA_SALES_CONTRACT_ACTIVE.getCode());
        assertEquals("LUNA_SHIPMENT_SIGNED", LunaNotifyTemplateEnum.LUNA_SHIPMENT_SIGNED.getCode());
        assertEquals("LUNA_AR_PERIOD_WARNING", LunaNotifyTemplateEnum.LUNA_AR_PERIOD_WARNING.getCode());

        // 校验全部模板均具备非空名称与占位内容
        for (LunaNotifyTemplateEnum item : LunaNotifyTemplateEnum.values()) {
            assertNotNull(item.getCode());
            assertNotNull(item.getName());
            assertNotNull(item.getContent());
            assertTrue(item.getContent().contains("{bizNo}") || item.getContent().contains("{"),
                    "模板内容应包含占位符: " + item.getCode());
            assertEquals(item, LunaNotifyTemplateEnum.fromCode(item.getCode()));
        }
    }
}

