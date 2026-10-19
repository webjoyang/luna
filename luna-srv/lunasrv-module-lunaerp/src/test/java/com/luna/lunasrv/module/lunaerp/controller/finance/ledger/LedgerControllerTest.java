package com.luna.lunasrv.module.lunaerp.controller.finance.ledger;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.ledger.vo.LedgerPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.ledger.vo.LedgerRespVO;
import com.luna.lunasrv.module.lunaerp.service.finance.ledger.LedgerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link LedgerController} 单元测试
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class LedgerControllerTest {

    @InjectMocks
    private LedgerController ledgerController;

    @Mock
    private LedgerService ledgerService;

    @Test
    @DisplayName("getLedgerPage: 委托 Service 分页查询资金流水")
    void testGetLedgerPage() {
        LedgerPageReqVO reqVO = new LedgerPageReqVO();
        reqVO.setAccountId(10L);

        LedgerRespVO item = new LedgerRespVO();
        item.setId(101L);
        PageResult<LedgerRespVO> pageResult = new PageResult<>(List.of(item), 1L);
        when(ledgerService.getLedgerPage(reqVO)).thenReturn(pageResult);

        CommonResult<PageResult<LedgerRespVO>> result = ledgerController.getLedgerPage(reqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(101L, result.getData().getList().get(0).getId());
        verify(ledgerService).getLedgerPage(reqVO);
    }

}
