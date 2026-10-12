<template>
  <ContentWrap>
    <el-tabs v-model="activeTab" class="payment-tabs" @tab-change="handleTabChange">
      <!-- ====================== TAB 1: 客户收款 ====================== -->
      <el-tab-pane label="客户收款" name="receipt">
        <!-- 搜索区 -->
        <el-form
          class="-mb-15px mt-2"
          :model="receiptQuery"
          ref="receiptQueryFormRef"
          :inline="true"
          label-width="80px"
        >
          <el-form-item label="关键词" prop="keyword">
            <el-input
              v-model="receiptQuery.keyword"
              placeholder="收款单号 / 银行水单号"
              clearable
              @keyup.enter="handleReceiptQuery"
              class="!w-200px"
            />
          </el-form-item>

          <el-form-item label="客户" prop="customerId">
            <el-select
              v-model="receiptQuery.customerId"
              placeholder="全部客户"
              clearable
              filterable
              remote
              reserve-keyword
              :remote-method="searchCustomers"
              :loading="customerLoading"
              class="!w-200px"
            >
              <el-option
                v-for="item in customerOptions"
                :key="item.id"
                :label="item.companyName"
                :value="item.id!"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="收款类型" prop="receiptType">
            <el-select v-model="receiptQuery.receiptType" placeholder="全部类型" clearable class="!w-130px">
              <el-option
                v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_FINANCE_RECEIPT_TYPE)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="资金账户" prop="accountId">
            <el-select v-model="receiptQuery.accountId" placeholder="全部账户" clearable class="!w-180px">
              <el-option
                v-for="acc in accountSimpleOptions"
                :key="acc.id"
                :label="acc.accountName"
                :value="acc.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="审批状态" prop="approvalStatus">
            <el-select
              v-model="receiptQuery.approvalStatus"
              placeholder="全部状态"
              clearable
              class="!w-130px"
            >
              <el-option
                v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_APPROVAL_INSTANCE_STATUS)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="收款日期">
            <el-date-picker
              v-model="receiptDateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              class="!w-240px"
            />
          </el-form-item>

          <el-form-item>
            <el-button @click="handleReceiptQuery">
              <Icon icon="ep:search" class="mr-2px" />搜索
            </el-button>
            <el-button @click="resetReceiptQuery">
              <Icon icon="ep:refresh" class="mr-2px" />重置
            </el-button>
            <el-button
              type="primary"
              plain
              @click="openCreateReceipt()"
              v-hasPermi="['lunaerp:finance:payment:create']"
            >
              <Icon icon="ep:plus" class="mr-2px" />新增收款单
            </el-button>
          </el-form-item>
        </el-form>

        <!-- 收款单列表表格 -->
        <el-table v-loading="receiptLoading" :data="receiptList" border stripe class="mt-4">
          <el-table-column label="收款单号" prop="paymentNo" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">
              <el-link type="primary" :underline="false" @click="openReceiptDetail(row.id)">
                <span class="cell-code">{{ row.paymentNo }}</span>
              </el-link>
            </template>
          </el-table-column>

          <el-table-column label="收款类型" prop="receiptType" width="110" align="center">
            <template #default="{ row }">
              <dict-tag :type="DICT_TYPE.LUNAERP_FINANCE_RECEIPT_TYPE" :value="row.receiptType" />
            </template>
          </el-table-column>

          <el-table-column label="客户名称" prop="customerName" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">{{ row.customerName || '—' }}</template>
          </el-table-column>

          <el-table-column label="币种" prop="currency" width="75" align="center">
            <template #default="{ row }">
              <el-tag size="small">{{ row.currency || 'CNY' }}</el-tag>
            </template>
          </el-table-column>

          <el-table-column label="收款金额" prop="payAmount" width="130" align="right">
            <template #default="{ row }">
              <span class="font-bold text-blue-600">￥{{ formatAmount(row.payAmount) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="资金账户" prop="accountName" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.accountName || getAccountName(row.accountId) }}
            </template>
          </el-table-column>

          <el-table-column label="收款日期" prop="receiptDate" width="110" align="center">
            <template #default="{ row }">{{ row.receiptDate || '—' }}</template>
          </el-table-column>

          <el-table-column label="经手人" prop="handlerName" width="100" align="center" show-overflow-tooltip>
            <template #default="{ row }">{{ row.handlerName || '—' }}</template>
          </el-table-column>

          <el-table-column label="审批状态" prop="approvalStatus" width="110" align="center">
            <template #default="{ row }">
              <dict-tag :type="DICT_TYPE.LUNAERP_APPROVAL_INSTANCE_STATUS" :value="row.approvalStatus" />
            </template>
          </el-table-column>

          <el-table-column label="过账状态" prop="status" width="95" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.status === 'COMPLETED'" type="success" size="small">已过账</el-tag>
              <el-tag v-else-if="row.status === 'REVERSED'" type="danger" size="small">已红冲</el-tag>
              <el-tag v-else type="info" size="small">未过账</el-tag>
            </template>
          </el-table-column>

          <el-table-column
            label="创建时间"
            prop="createTime"
            width="160"
            align="center"
            :formatter="dateFormatter"
          />

          <!-- 操作列 -->
          <el-table-column label="操作" width="220" align="center" fixed="right">
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                @click="openReceiptDetail(row.id)"
                v-hasPermi="['lunaerp:finance:payment:query']"
              >
                详情
              </el-button>

              <!-- 草稿态操作 (PENDING 或 DRAFT) -->
              <template v-if="row.approvalStatus === 'PENDING' || row.approvalStatus === 'DRAFT'">
                <el-button
                  link
                  type="primary"
                  @click="openEditReceipt(row)"
                  v-hasPermi="['lunaerp:finance:payment:update']"
                >
                  编辑
                </el-button>
                <el-button
                  link
                  type="success"
                  @click="handleSubmitReceipt(row)"
                  v-hasPermi="['lunaerp:finance:payment:create']"
                >
                  提交
                </el-button>
                <el-button
                  link
                  type="danger"
                  @click="handleDeleteReceipt(row)"
                  v-hasPermi="['lunaerp:finance:payment:delete']"
                >
                  删除
                </el-button>
              </template>

              <!-- 已驳回操作 (REJECTED) -->
              <template v-else-if="row.approvalStatus === 'REJECTED'">
                <el-button
                  link
                  type="primary"
                  @click="openEditReceipt(row)"
                  v-hasPermi="['lunaerp:finance:payment:update']"
                >
                  重新编辑
                </el-button>
              </template>

              <!-- 已过账操作 (COMPLETED) -->
              <template v-else-if="row.status === 'COMPLETED'">
                <el-button
                  v-if="row.receiptType === 'ADVANCE'"
                  link
                  type="warning"
                  @click="openAdvanceWriteoffDialog(row)"
                  v-hasPermi="['lunaerp:finance:payment:writeoff']"
                >
                  预收转核销
                </el-button>
                <el-button
                  link
                  type="danger"
                  @click="handleReverseReceipt(row)"
                  v-hasPermi="['lunaerp:finance:payment:reverse']"
                >
                  红冲
                </el-button>
              </template>
            </template>
          </el-table-column>

          <template #empty>
            <el-empty description="暂无客户收款数据" :image-size="80" />
          </template>
        </el-table>

        <!-- 分页 -->
        <Pagination
          :total="receiptTotal"
          v-model:page="receiptQuery.pageNo"
          v-model:limit="receiptQuery.pageSize"
          @pagination="getReceiptList"
        />
      </el-tab-pane>

      <!-- ====================== TAB 2: 付款申请 ====================== -->
      <el-tab-pane label="付款申请" name="apply">
        <!-- 搜索区 -->
        <el-form
          class="-mb-15px mt-2"
          :model="applyQuery"
          ref="applyQueryFormRef"
          :inline="true"
          label-width="80px"
        >
          <el-form-item label="关键词" prop="keyword">
            <el-input
              v-model="applyQuery.keyword"
              placeholder="申请单号 / 供应商名称"
              clearable
              @keyup.enter="handleApplyQuery"
              class="!w-220px"
            />
          </el-form-item>

          <el-form-item label="供应商" prop="supplierId">
            <el-select
              v-model="applyQuery.supplierId"
              placeholder="全部供应商"
              clearable
              filterable
              remote
              reserve-keyword
              :remote-method="searchSuppliers"
              :loading="supplierLoading"
              class="!w-200px"
            >
              <el-option
                v-for="item in supplierOptions"
                :key="item.id"
                :label="item.companyName"
                :value="item.id!"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="类型" prop="applyType">
            <el-select v-model="applyQuery.applyType" placeholder="全部类型" clearable class="!w-140px">
              <el-option
                v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_PAYMENT_APPLY_TYPE)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="审批状态" prop="approvalStatus">
            <el-select
              v-model="applyQuery.approvalStatus"
              placeholder="全部状态"
              clearable
              class="!w-140px"
            >
              <el-option
                v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_APPROVAL_INSTANCE_STATUS)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item>
            <el-button @click="handleApplyQuery">
              <Icon icon="ep:search" class="mr-2px" />搜索
            </el-button>
            <el-button @click="resetApplyQuery">
              <Icon icon="ep:refresh" class="mr-2px" />重置
            </el-button>
            <el-button
              type="primary"
              plain
              @click="openCreateApply()"
              v-hasPermi="['lunaerp:finance:payment:apply']"
            >
              <Icon icon="ep:plus" class="mr-2px" />发起付款申请
            </el-button>
          </el-form-item>
        </el-form>

        <!-- 申请单列表表格 -->
        <el-table v-loading="applyLoading" :data="applyList" border stripe class="mt-4">
          <el-table-column label="申请单号" prop="applyNo" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">
              <el-link type="primary" :underline="false" @click="openApplyDetail(row.id)">
                <span class="cell-code">{{ row.applyNo }}</span>
              </el-link>
            </template>
          </el-table-column>

          <el-table-column label="申请类型" prop="applyType" width="110" align="center">
            <template #default="{ row }">
              <dict-tag :type="DICT_TYPE.LUNAERP_PAYMENT_APPLY_TYPE" :value="row.applyType" />
            </template>
          </el-table-column>

          <el-table-column label="供应商名称" prop="supplierName" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">{{ row.supplierName || '—' }}</template>
          </el-table-column>

          <el-table-column label="关联对账单" prop="reconNo" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.reconNo" class="cell-code text-xs">{{ row.reconNo }}</span>
              <span v-else class="text-gray-400">—</span>
            </template>
          </el-table-column>

          <el-table-column label="申请金额" prop="amount" width="130" align="right">
            <template #default="{ row }">
              <span class="font-bold text-red-600">￥{{ formatAmount(row.amount) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="已核销金额" prop="allocatedAmount" width="130" align="right">
            <template #default="{ row }">
              <span class="font-bold text-green-600">￥{{ formatAmount(row.allocatedAmount || 0) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="审批状态" prop="approvalStatus" width="110" align="center">
            <template #default="{ row }">
              <dict-tag :type="DICT_TYPE.LUNAERP_APPROVAL_INSTANCE_STATUS" :value="row.approvalStatus" />
            </template>
          </el-table-column>

          <el-table-column
            label="创建时间"
            prop="createTime"
            width="160"
            align="center"
            :formatter="dateFormatter"
          />

          <!-- 操作列 -->
          <el-table-column label="操作" width="230" align="center" fixed="right">
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                @click="openApplyDetail(row.id)"
                v-hasPermi="['lunaerp:finance:payment:query']"
              >
                详情
              </el-button>

              <!-- 草稿态操作 (PENDING 或 DRAFT) -->
              <template v-if="row.approvalStatus === 'PENDING' || row.approvalStatus === 'DRAFT'">
                <el-button
                  link
                  type="primary"
                  @click="openEditApply(row)"
                  v-hasPermi="['lunaerp:finance:payment:apply']"
                >
                  编辑
                </el-button>
                <el-button
                  link
                  type="success"
                  @click="handleSubmitApply(row)"
                  v-hasPermi="['lunaerp:finance:payment:apply']"
                >
                  提交
                </el-button>
                <el-button
                  link
                  type="danger"
                  @click="handleDeleteApply(row)"
                  v-hasPermi="['lunaerp:finance:payment:apply']"
                >
                  删除
                </el-button>
              </template>

              <!-- 审批中操作 (APPROVING) -->
              <template v-else-if="row.approvalStatus === 'APPROVING'">
                <el-button
                  link
                  type="warning"
                  @click="handleCancelApproveApply(row)"
                  v-hasPermi="['lunaerp:finance:payment:apply']"
                >
                  撤回
                </el-button>
              </template>

              <!-- 已驳回操作 (REJECTED) -->
              <template v-else-if="row.approvalStatus === 'REJECTED'">
                <el-button
                  link
                  type="primary"
                  @click="openEditApply(row)"
                  v-hasPermi="['lunaerp:finance:payment:apply']"
                >
                  重新编辑
                </el-button>
              </template>

              <!-- 已通过 (APPROVED) -->
              <template v-else-if="row.approvalStatus === 'APPROVED'">
                <el-button
                  link
                  type="success"
                  @click="handleGoToRegisterTab(row)"
                >
                  前往付款登记
                </el-button>
              </template>
            </template>
          </el-table-column>

          <template #empty>
            <el-empty description="暂无付款申请数据" :image-size="80" />
          </template>
        </el-table>

        <!-- 分页 -->
        <Pagination
          :total="applyTotal"
          v-model:page="applyQuery.pageNo"
          v-model:limit="applyQuery.pageSize"
          @pagination="getApplyList"
        />
      </el-tab-pane>

      <!-- ====================== TAB 2: 付款登记 ====================== -->
      <el-tab-pane label="付款登记" name="register">
        <!-- 上半区：待核销付款申请 -->
        <div class="mb-6 p-4 bg-[#f8fafc] rounded border border-[#e2e8f0]">
          <div class="flex items-center justify-between mb-3 pb-2 border-b border-[#e2e8f0]">
            <div class="flex items-center gap-2">
              <span class="font-bold text-sm text-[#1e293b]">待核销付款申请（已审批未核销完）</span>
              <el-tag size="small" type="success">审批已通过</el-tag>
            </div>
            <div class="flex items-center gap-2">
              <el-select
                v-model="pendingSupplierId"
                placeholder="筛选供应商"
                clearable
                filterable
                remote
                reserve-keyword
                :remote-method="searchSuppliers"
                :loading="supplierLoading"
                class="!w-200px"
                @change="getPendingApplies"
              >
                <el-option
                  v-for="item in supplierOptions"
                  :key="item.id"
                  :label="item.companyName"
                  :value="item.id!"
                />
              </el-select>
              <el-button @click="getPendingApplies">
                <Icon icon="ep:refresh" class="mr-2px" />刷新
              </el-button>
              <el-button
                type="primary"
                :disabled="selectedPendingApplies.length === 0"
                @click="openRegisterDialog(selectedPendingApplies)"
                v-hasPermi="['lunaerp:finance:payment:register']"
              >
                <Icon icon="ep:money" class="mr-2px" />
                付款登记 (已选 {{ selectedPendingApplies.length }} 笔)
              </el-button>
            </div>
          </div>

          <el-table
            v-loading="pendingLoading"
            :data="pendingApplies"
            border
            size="small"
            stripe
            @selection-change="handlePendingSelectionChange"
          >
            <el-table-column type="selection" width="45" align="center" />
            <el-table-column label="申请单号" prop="applyNo" min-width="140">
              <template #default="{ row }">
                <span class="cell-code">{{ row.applyNo }}</span>
              </template>
            </el-table-column>
            <el-table-column label="类型" prop="applyType" width="100" align="center">
              <template #default="{ row }">
                <dict-tag :type="DICT_TYPE.LUNAERP_PAYMENT_APPLY_TYPE" :value="row.applyType" />
              </template>
            </el-table-column>
            <el-table-column label="供应商" prop="supplierName" min-width="150" show-overflow-tooltip />
            <el-table-column label="申请金额" prop="amount" width="120" align="right">
              <template #default="{ row }">
                <span class="font-bold text-red-600">￥{{ formatAmount(row.amount) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="已核销金额" prop="allocatedAmount" width="120" align="right">
              <template #default="{ row }">
                ￥{{ formatAmount(row.allocatedAmount || 0) }}
              </template>
            </el-table-column>
            <el-table-column label="剩余可核销" width="130" align="right">
              <template #default="{ row }">
                <span class="font-bold text-green-600">
                  ￥{{ formatAmount(row.amount - (row.allocatedAmount || 0)) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column
              label="申请时间"
              prop="createTime"
              width="150"
              align="center"
              :formatter="dateFormatter"
            />
            <el-table-column label="操作" width="110" align="center">
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  @click="openRegisterDialog([row])"
                  v-hasPermi="['lunaerp:finance:payment:register']"
                >
                  去登记
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="当前无待核销的付款申请" :image-size="60" />
            </template>
          </el-table>
        </div>

        <!-- 下半区：付款单流水列表 -->
        <div>
          <div class="flex items-center justify-between mb-3">
            <span class="font-bold text-sm text-[#1e293b]">付款单流水列表</span>
          </div>

          <el-form
            class="-mb-15px"
            :model="paymentQuery"
            ref="paymentQueryFormRef"
            :inline="true"
            label-width="80px"
          >
            <el-form-item label="供应商" prop="supplierId">
              <el-select
                v-model="paymentQuery.supplierId"
                placeholder="全部供应商"
                clearable
                filterable
                remote
                reserve-keyword
                :remote-method="searchSuppliers"
                :loading="supplierLoading"
                class="!w-200px"
              >
                <el-option
                  v-for="item in supplierOptions"
                  :key="item.id"
                  :label="item.companyName"
                  :value="item.id!"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="资金账户" prop="accountId">
              <el-select
                v-model="paymentQuery.accountId"
                placeholder="全部账户"
                clearable
                class="!w-180px"
              >
                <el-option
                  v-for="acc in DEFAULT_FINANCE_ACCOUNTS"
                  :key="acc.id"
                  :label="acc.name"
                  :value="acc.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item>
              <el-button @click="handlePaymentQuery">
                <Icon icon="ep:search" class="mr-2px" />搜索
              </el-button>
              <el-button @click="resetPaymentQuery">
                <Icon icon="ep:refresh" class="mr-2px" />重置
              </el-button>
            </el-form-item>
          </el-form>

          <el-table v-loading="paymentLoading" :data="paymentList" border stripe class="mt-4">
            <el-table-column label="付款单号" prop="paymentNo" min-width="150">
              <template #default="{ row }">
                <el-link type="primary" :underline="false" @click="openPaymentDetail(row.id)">
                  <span class="cell-code">{{ row.paymentNo }}</span>
                </el-link>
              </template>
            </el-table-column>

            <el-table-column label="收款供应商" prop="supplierName" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">{{ row.supplierName || '—' }}</template>
            </el-table-column>

            <el-table-column label="付款资金账户" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">
                {{ getAccountName(row.accountId) }}
              </template>
            </el-table-column>

            <el-table-column label="实付金额" prop="payAmount" width="130" align="right">
              <template #default="{ row }">
                <span class="font-bold text-red-600">￥{{ formatAmount(row.payAmount) }}</span>
              </template>
            </el-table-column>

            <el-table-column label="预付抵扣" prop="prepayUsedAmount" width="120" align="right">
              <template #default="{ row }">
                <span class="font-bold text-orange-600">￥{{ formatAmount(row.prepayUsedAmount || 0) }}</span>
              </template>
            </el-table-column>

            <el-table-column label="支付总计" width="130" align="right">
              <template #default="{ row }">
                <span class="font-bold text-blue-600">
                  ￥{{ formatAmount(Number(row.payAmount || 0) + Number(row.prepayUsedAmount || 0)) }}
                </span>
              </template>
            </el-table-column>

            <el-table-column label="核销申请单" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">
                <template v-if="row.items && row.items.length > 0">
                  <el-tag
                    v-for="refItem in row.items"
                    :key="refItem.id || refItem.applyNo"
                    size="small"
                    class="mr-1 mb-1"
                  >
                    {{ refItem.applyNo }} (￥{{ formatAmount(refItem.allocateAmount) }})
                  </el-tag>
                </template>
                <span v-else class="text-gray-400">—</span>
              </template>
            </el-table-column>

            <el-table-column label="状态" width="90" align="center">
              <template #default>
                <el-tag type="success" size="small">已完成</el-tag>
              </template>
            </el-table-column>

            <el-table-column
              label="登记时间"
              prop="createTime"
              width="160"
              align="center"
              :formatter="dateFormatter"
            />

            <!-- 操作列（付款单无编辑/删除操作，红冲二期） -->
            <el-table-column label="操作" width="90" align="center" fixed="right">
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  @click="openPaymentDetail(row.id)"
                  v-hasPermi="['lunaerp:finance:payment:query']"
                >
                  详情
                </el-button>
              </template>
            </el-table-column>

            <template #empty>
              <el-empty description="暂无付款单流水数据" :image-size="80" />
            </template>
          </el-table>

          <Pagination
            :total="paymentTotal"
            v-model:page="paymentQuery.pageNo"
            v-model:limit="paymentQuery.pageSize"
            @pagination="getPaymentList"
          />
        </div>
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>

  <!-- 付款申请发起/编辑弹窗 -->
  <PaymentApplyFormDialog ref="applyFormDialogRef" @success="handleApplySuccess" />

  <!-- 付款申请详情抽屉 -->
  <PaymentApplyDetailDrawer ref="applyDetailDrawerRef" @go-to-register="handleGoToRegisterFromDrawer" />

  <!-- 付款登记弹窗 -->
  <PaymentRegisterDialog ref="registerDialogRef" @success="handleRegisterSuccess" />

  <!-- 付款单流水详情弹窗 -->
  <PaymentDetailDialog ref="paymentDetailDialogRef" />

  <!-- 客户收款单发起/编辑弹窗 -->
  <ReceiptFormDialog ref="receiptFormDialogRef" @success="handleReceiptSuccess" />

  <!-- 客户收款单详情弹窗 -->
  <ReceiptDetailDialog ref="receiptDetailDialogRef" />

  <!-- 预收转核销弹窗 -->
  <AdvanceWriteoffDialog ref="advanceWriteoffDialogRef" @success="handleReceiptSuccess" />
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import { useMessage } from '@/hooks/web/useMessage'
import * as PaymentApi from '@/api/lunaerp-elmui/finance/payment'
import type {
  PaymentApplyVO,
  PaymentApplyPageParam,
  PaymentVO,
  PaymentPageParam,
  AccountSimpleVO
} from '@/api/lunaerp-elmui/finance/payment'
import * as SupplierApi from '@/api/lunaerp-elmui/baseinfo/supplier'
import type { SupplierVO } from '@/api/lunaerp-elmui/baseinfo/supplier'
import * as CustomerApi from '@/api/lunaerp-elmui/baseinfo/customer'
import type { CustomerVO } from '@/api/lunaerp-elmui/baseinfo/customer'
import { formatAmount, DEFAULT_FINANCE_ACCOUNTS } from './utils'
import PaymentApplyFormDialog from './components/PaymentApplyFormDialog.vue'
import PaymentApplyDetailDrawer from './components/PaymentApplyDetailDrawer.vue'
import PaymentRegisterDialog from './components/PaymentRegisterDialog.vue'
import PaymentDetailDialog from './components/PaymentDetailDialog.vue'
import ReceiptFormDialog from './components/ReceiptFormDialog.vue'
import ReceiptDetailDialog from './components/ReceiptDetailDialog.vue'
import AdvanceWriteoffDialog from './components/AdvanceWriteoffDialog.vue'

defineOptions({ name: 'LunaerpElmuiPayment' })

const route = useRoute()
const message = useMessage()

const activeTab = ref<'receipt' | 'apply' | 'register'>('receipt')

// ====================== 客户收款逻辑 ======================
const receiptLoading = ref(false)
const receiptTotal = ref(0)
const receiptList = ref<PaymentVO[]>([])
const receiptQueryFormRef = ref()
const receiptFormDialogRef = ref()
const receiptDetailDialogRef = ref()
const advanceWriteoffDialogRef = ref()
const receiptDateRange = ref<[string, string] | null>(null)

const receiptQuery = reactive<PaymentPageParam>({
  pageNo: 1,
  pageSize: 20,
  direction: 'IN',
  keyword: '',
  customerId: undefined,
  accountId: undefined,
  receiptType: undefined,
  approvalStatus: undefined,
  status: undefined,
  receiptDateStart: undefined,
  receiptDateEnd: undefined
})

// 客户下拉列表
const customerOptions = ref<CustomerVO[]>([])
const customerLoading = ref(false)
const searchCustomers = async (query: string) => {
  customerLoading.value = true
  try {
    const res = await CustomerApi.getCustomerPage({
      pageNo: 1,
      pageSize: 30,
      search: query || '',
      status: 'ACTIVE'
    })
    customerOptions.value = res.list || []
  } finally {
    customerLoading.value = false
  }
}

// 资金账户简要列表（用于收款筛选）
const accountSimpleOptions = ref<AccountSimpleVO[]>([])
const loadAccountSimpleOptions = async () => {
  try {
    accountSimpleOptions.value = await PaymentApi.getAccountSimpleList()
  } catch (e) {
    // 降级使用内置
  }
}

const getReceiptList = async () => {
  receiptLoading.value = true
  try {
    if (receiptDateRange.value && receiptDateRange.value.length === 2) {
      receiptQuery.receiptDateStart = receiptDateRange.value[0]
      receiptQuery.receiptDateEnd = receiptDateRange.value[1]
    } else {
      receiptQuery.receiptDateStart = undefined
      receiptQuery.receiptDateEnd = undefined
    }
    const res = await PaymentApi.getPaymentPage(receiptQuery)
    receiptList.value = res.list
    receiptTotal.value = res.total
  } finally {
    receiptLoading.value = false
  }
}

const handleReceiptQuery = () => {
  receiptQuery.pageNo = 1
  getReceiptList()
}

const resetReceiptQuery = () => {
  receiptDateRange.value = null
  receiptQueryFormRef.value?.resetFields()
  handleReceiptQuery()
}

const openCreateReceipt = () => {
  receiptFormDialogRef.value?.open('create')
}

const openEditReceipt = (row: PaymentVO) => {
  receiptFormDialogRef.value?.open('edit', row)
}

const openReceiptDetail = (id: number) => {
  receiptDetailDialogRef.value?.open(id)
}

const handleReceiptSuccess = () => {
  getReceiptList()
}

const handleSubmitReceipt = async (row: PaymentVO) => {
  try {
    await message.confirm(`确定提交客户收款单【${row.paymentNo}】进行审批吗？`, '提交审批')
    await PaymentApi.submitReceipt(row.id)
    message.success('已提交审批')
    getReceiptList()
  } catch {}
}

const handleDeleteReceipt = async (row: PaymentVO) => {
  try {
    await message.confirm(`确定删除草稿收款单【${row.paymentNo}】吗？删除后进入回收站。`, '删除收款单')
    await PaymentApi.deleteReceipt(row.id)
    message.success('已删除草稿')
    getReceiptList()
  } catch {}
}

const openAdvanceWriteoffDialog = (row: PaymentVO) => {
  advanceWriteoffDialogRef.value?.open(row)
}

const handleReverseReceipt = async (row: PaymentVO) => {
  try {
    await message.confirm(
      `确定对客户收款单【${row.paymentNo}】进行红冲吗？红冲后分录将冲回，账户余额扣减，核销分录自动重开，此操作不可撤销！`,
      '收款单红冲确认'
    )
    await PaymentApi.reverseReceipt(row.id)
    message.success('收款单已成功红冲')
    getReceiptList()
  } catch {}
}

// 供应商下拉列表
const supplierOptions = ref<SupplierVO[]>([])
const supplierLoading = ref(false)
const searchSuppliers = async (query: string) => {
  supplierLoading.value = true
  try {
    const res = await SupplierApi.getSupplierPage({
      pageNo: 1,
      pageSize: 30,
      search: query || '',
      status: 'ACTIVE'
    })
    supplierOptions.value = res.list
  } finally {
    supplierLoading.value = false
  }
}

// ====================== 付款申请逻辑 ======================
const applyLoading = ref(false)
const applyTotal = ref(0)
const applyList = ref<PaymentApplyVO[]>([])
const applyQueryFormRef = ref()
const applyFormDialogRef = ref()
const applyDetailDrawerRef = ref()

const applyQuery = reactive<PaymentApplyPageParam>({
  pageNo: 1,
  pageSize: 20,
  keyword: '',
  supplierId: undefined,
  applyType: undefined,
  approvalStatus: undefined
})

const getApplyList = async () => {
  applyLoading.value = true
  try {
    const res = await PaymentApi.getPaymentApplyPage(applyQuery)
    applyList.value = res.list
    applyTotal.value = res.total
  } finally {
    applyLoading.value = false
  }
}

const handleApplyQuery = () => {
  applyQuery.pageNo = 1
  getApplyList()
}

const resetApplyQuery = () => {
  applyQueryFormRef.value?.resetFields()
  handleApplyQuery()
}

const openCreateApply = () => {
  applyFormDialogRef.value?.open('create')
}

const openEditApply = (row: PaymentApplyVO) => {
  applyFormDialogRef.value?.open('edit', row)
}

const openApplyDetail = (id: number) => {
  applyDetailDrawerRef.value?.open(id)
}

const handleApplySuccess = () => {
  getApplyList()
}

const handleSubmitApply = async (row: PaymentApplyVO) => {
  try {
    await message.confirm(`确定提交付款申请【${row.applyNo}】进行审批吗？`, '提交审批')
    await PaymentApi.submitPaymentApply(row.id)
    message.success('已提交审批')
    getApplyList()
  } catch {}
}

const handleCancelApproveApply = async (row: PaymentApplyVO) => {
  try {
    await message.confirm(`确定撤回付款申请【${row.applyNo}】的审批吗？`, '撤回审批')
    await PaymentApi.cancelApprovePaymentApply(row.id)
    message.success('已撤回审批')
    getApplyList()
  } catch {}
}

const handleDeleteApply = async (row: PaymentApplyVO) => {
  try {
    await message.confirm(`确定删除草稿付款申请【${row.applyNo}】吗？删除后进入回收站。`, '删除申请')
    await PaymentApi.deletePaymentApply(row.id)
    message.success('已移入回收站')
    getApplyList()
  } catch {}
}

const handleGoToRegisterTab = (row: PaymentApplyVO) => {
  activeTab.value = 'register'
  pendingSupplierId.value = row.supplierId
  getPendingApplies()
}

const handleGoToRegisterFromDrawer = (apply: PaymentApplyVO) => {
  handleGoToRegisterTab(apply)
}

// ====================== 付款登记逻辑 ======================
const pendingLoading = ref(false)
const pendingSupplierId = ref<number | undefined>(undefined)
const pendingApplies = ref<PaymentApplyVO[]>([])
const selectedPendingApplies = ref<PaymentApplyVO[]>([])
const registerDialogRef = ref()

const paymentLoading = ref(false)
const paymentTotal = ref(0)
const paymentList = ref<PaymentVO[]>([])
const paymentQueryFormRef = ref()
const paymentDetailDialogRef = ref()

const paymentQuery = reactive<PaymentPageParam>({
  pageNo: 1,
  pageSize: 20,
  supplierId: undefined,
  accountId: undefined
})

const getAccountName = (accountId: number) => {
  const acc = DEFAULT_FINANCE_ACCOUNTS.find((a) => a.id === accountId)
  return acc ? acc.name : `账户 #${accountId}`
}

/** 查询 APPROVED 且未核销完的付款申请 */
const getPendingApplies = async () => {
  pendingLoading.value = true
  try {
    const res = await PaymentApi.getPaymentApplyPage({
      pageNo: 1,
      pageSize: 100,
      approvalStatus: 'APPROVED',
      supplierId: pendingSupplierId.value
    })
    // 过滤出未核销完的单据 (amount > allocatedAmount)
    pendingApplies.value = (res.list || []).filter(
      (item) => item.amount - (item.allocatedAmount || 0) > 0.001
    )
  } finally {
    pendingLoading.value = false
  }
}

const handlePendingSelectionChange = (selection: PaymentApplyVO[]) => {
  selectedPendingApplies.value = selection
}

const openRegisterDialog = (applies: PaymentApplyVO[]) => {
  registerDialogRef.value?.open(applies)
}

const handleRegisterSuccess = () => {
  getPendingApplies()
  getPaymentList()
  getApplyList()
}

/** 查询已登记付款单列表 */
const getPaymentList = async () => {
  paymentLoading.value = true
  try {
    const res = await PaymentApi.getPaymentPage(paymentQuery)
    paymentList.value = res.list
    paymentTotal.value = res.total
  } finally {
    paymentLoading.value = false
  }
}

const handlePaymentQuery = () => {
  paymentQuery.pageNo = 1
  getPaymentList()
}

const resetPaymentQuery = () => {
  paymentQueryFormRef.value?.resetFields()
  handlePaymentQuery()
}

const openPaymentDetail = (id: number) => {
  paymentDetailDialogRef.value?.open(id)
}

const handleTabChange = (name: any) => {
  if (name === 'receipt') {
    getReceiptList()
  } else if (name === 'apply') {
    getApplyList()
  } else if (name === 'register') {
    getPendingApplies()
    getPaymentList()
  }
}

onMounted(async () => {
  await searchSuppliers('')
  await searchCustomers('')
  await loadAccountSimpleOptions()

  // 检查是否从供应商对账单直接跳转过来生成付款申请
  if (route.query.reconId && route.query.action === 'create') {
    activeTab.value = 'apply'
    const reconId = Number(route.query.reconId)
    const supplierId = route.query.supplierId ? Number(route.query.supplierId) : undefined
    applyFormDialogRef.value?.open('create', {
      applyType: 'RECON',
      reconId,
      supplierId
    })
    await getApplyList()
  } else {
    await getReceiptList()
  }
})
</script>

<style scoped>
.cell-code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-weight: 500;
  color: var(--el-color-primary);
}
</style>
