<template>
  <div class="loom-container">
    <el-tabs v-model="activeTab" type="border-card" class="loom-tabs" @tab-change="handleTabChange">
      <!-- ==================== Tab 1: 织机档案 ==================== -->
      <el-tab-pane label="织机档案" name="loom">
        <!-- 搜索区 -->
        <ContentWrap>
          <el-form :model="queryParams" ref="queryFormRef" :inline="true" class="-mb-15px">
            <el-form-item label="关键词" prop="search">
              <el-input
                v-model="queryParams.search"
                placeholder="机台号 / 型号 / 备注"
                clearable
                @keyup.enter="handleQuery"
                class="!w-200px"
              />
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select
                v-model="queryParams.status"
                placeholder="全部状态"
                clearable
                class="!w-120px"
              >
                <el-option label="启用" value="ACTIVE" />
                <el-option label="停用" value="INACTIVE" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button @click="handleQuery">
                <Icon icon="ep:search" class="mr-1" />搜索
              </el-button>
              <el-button @click="resetQuery">
                <Icon icon="ep:refresh" class="mr-1" />重置
              </el-button>
            </el-form-item>
          </el-form>
        </ContentWrap>

        <!-- 列表与操作区 -->
        <ContentWrap class="mt-15px">
          <div class="flex items-center justify-between mb-15px">
            <div class="flex items-center gap-2">
              <span class="text-16px font-bold">织机列表</span>
              <span class="text-12px text-gray-400">（车间织机基础主数据；IoT快照为实时运行辅助列，不回写织机启用/停用状态）</span>
            </div>
            <div class="flex items-center gap-2">
              <el-button
                type="primary"
                plain
                @click="openDialog('create')"
                v-hasPermi="['lunaerp:production:loom:create']"
              >
                <Icon icon="ep:plus" class="mr-1" /> 新增织机
              </el-button>
              <el-button @click="getList">
                <Icon icon="ep:refresh" class="mr-1" /> 刷新
              </el-button>
            </div>
          </div>

          <el-table
            v-loading="loading"
            :data="list"
            border
            stripe
            class="mt-10px"
          >
            <el-table-column label="机台号" prop="loomNo" min-width="130">
              <template #default="{ row }">
                <span class="font-bold text-primary">{{ row.loomNo }}</span>
              </template>
            </el-table-column>
            <el-table-column label="型号" prop="model" min-width="160" :show-overflow-tooltip="true">
              <template #default="{ row }">
                {{ row.model || '—' }}
              </template>
            </el-table-column>
            <el-table-column label="织机档案状态" prop="status" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
                  {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>

            <!-- IoT 辅助列：绑定设备与实时快照（不回写织机状态） -->
            <el-table-column label="IoT绑定设备" min-width="140" align="center">
              <template #default="{ row }">
                <span v-if="bindingMap[row.id]" class="font-mono text-12px text-blue-600">
                  {{ bindingMap[row.id].vendorDeviceId }}
                </span>
                <span v-else class="text-gray-400 text-12px">未绑定</span>
              </template>
            </el-table-column>
            <el-table-column label="IoT运行快照 (辅助列)" min-width="180" align="center">
              <template #default="{ row }">
                <div v-if="bindingMap[row.id] && realtimeMap[bindingMap[row.id].vendorDeviceId]">
                  <el-tooltip
                    effect="dark"
                    placement="top"
                    :content="`采集时间: ${realtimeMap[bindingMap[row.id].vendorDeviceId].fetchedAt || '—'}，在班: ${realtimeMap[bindingMap[row.id].vendorDeviceId].staffOnShift || '—'}`"
                  >
                    <div class="flex items-center justify-center gap-2">
                      <el-tag :type="getRealtimeStatusTag(realtimeMap[bindingMap[row.id].vendorDeviceId].status)" size="small">
                        {{ getRealtimeStatusText(realtimeMap[bindingMap[row.id].vendorDeviceId].status) }}
                      </el-tag>
                      <span class="text-12px text-gray-600 font-mono">
                        {{ realtimeMap[bindingMap[row.id].vendorDeviceId].runEfficiencyPct != null ? realtimeMap[bindingMap[row.id].vendorDeviceId].runEfficiencyPct + '%' : '' }}
                        {{ realtimeMap[bindingMap[row.id].vendorDeviceId].minuteCircleSpeed != null ? realtimeMap[bindingMap[row.id].vendorDeviceId].minuteCircleSpeed + ' RPM' : '' }}
                      </span>
                    </div>
                  </el-tooltip>
                </div>
                <span v-else class="text-gray-400 text-12px">无快照</span>
              </template>
            </el-table-column>

            <el-table-column label="备注" prop="remark" min-width="160" :show-overflow-tooltip="true">
              <template #default="{ row }">
                {{ row.remark || '—' }}
              </template>
            </el-table-column>
            <el-table-column
              label="创建时间"
              prop="createTime"
              width="160"
              align="center"
              :formatter="dateFormatter"
            />
            <el-table-column label="操作" width="220" align="center" fixed="right">
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  @click="openDialog('update', row)"
                  v-hasPermi="['lunaerp:production:loom:update']"
                >
                  <Icon icon="ep:edit" class="mr-1" />编辑
                </el-button>
                <el-button
                  v-if="row.status === 'ACTIVE'"
                  link
                  type="warning"
                  @click="handleToggleStatus(row, 'INACTIVE')"
                  v-hasPermi="['lunaerp:production:loom:update']"
                >
                  <Icon icon="ep:video-pause" class="mr-1" />停用
                </el-button>
                <el-button
                  v-else
                  link
                  type="success"
                  @click="handleToggleStatus(row, 'ACTIVE')"
                  v-hasPermi="['lunaerp:production:loom:update']"
                >
                  <Icon icon="ep:video-play" class="mr-1" />启用
                </el-button>
                <el-button
                  link
                  type="danger"
                  @click="handleDelete(row)"
                  v-hasPermi="['lunaerp:production:loom:delete']"
                >
                  <Icon icon="ep:delete" class="mr-1" />删除
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无织机数据" :image-size="60" />
            </template>
          </el-table>

          <Pagination
            :total="total"
            v-model:page="queryParams.pageNo"
            v-model:limit="queryParams.pageSize"
            @pagination="getList"
          />
        </ContentWrap>
      </el-tab-pane>

      <!-- ==================== Tab 2: 设备接入凭证 ==================== -->
      <el-tab-pane label="设备接入凭证" name="iot-credential">
        <ContentWrap>
          <el-form :model="credQueryParams" ref="credQueryFormRef" :inline="true" class="-mb-15px">
            <el-form-item label="关键词" prop="search">
              <el-input
                v-model="credQueryParams.search"
                placeholder="厂商账号 / 范围"
                clearable
                @keyup.enter="handleCredQuery"
                class="!w-200px"
              />
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select
                v-model="credQueryParams.status"
                placeholder="全部状态"
                clearable
                class="!w-120px"
              >
                <el-option label="启用" value="ACTIVE" />
                <el-option label="停用" value="INACTIVE" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button @click="handleCredQuery">
                <Icon icon="ep:search" class="mr-1" />搜索
              </el-button>
              <el-button @click="resetCredQuery">
                <Icon icon="ep:refresh" class="mr-1" />重置
              </el-button>
            </el-form-item>
          </el-form>
        </ContentWrap>

        <ContentWrap class="mt-15px">
          <div class="flex items-center justify-between mb-15px">
            <div class="flex items-center gap-2">
              <span class="text-16px font-bold">厂商接入凭证</span>
              <span class="text-12px text-gray-400">（厂商API凭证密码加密存储，前端不回显明文）</span>
            </div>
            <div class="flex items-center gap-2">
              <el-button
                type="primary"
                plain
                @click="openCredDialog('create')"
                v-hasPermi="['lunaerp:production:iot:vendor-credential:create']"
              >
                <Icon icon="ep:plus" class="mr-1" /> 新增凭证
              </el-button>
              <el-button @click="getCredentialList">
                <Icon icon="ep:refresh" class="mr-1" /> 刷新
              </el-button>
            </div>
          </div>

          <el-table
            v-loading="credLoading"
            :data="credList"
            border
            stripe
            class="mt-10px"
          >
            <el-table-column label="厂商账号" prop="vendorAccount" min-width="160">
              <template #default="{ row }">
                <span class="font-bold text-primary">{{ row.vendorAccount }}</span>
              </template>
            </el-table-column>
            <el-table-column label="密码状态" width="120" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.hasPassword" type="success" size="small">已配置加密</el-tag>
                <el-tag v-else type="info" size="small">未设置</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="设备范围说明" prop="scope" min-width="200" :show-overflow-tooltip="true">
              <template #default="{ row }">
                {{ row.scope || '—' }}
              </template>
            </el-table-column>
            <el-table-column label="状态" prop="status" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
                  {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              label="最后同步时间"
              prop="lastSyncAt"
              width="170"
              align="center"
              :formatter="dateFormatter"
            />
            <el-table-column
              label="创建时间"
              prop="createTime"
              width="170"
              align="center"
              :formatter="dateFormatter"
            />
            <el-table-column label="操作" width="160" align="center" fixed="right">
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  @click="openCredDialog('update', row)"
                  v-hasPermi="['lunaerp:production:iot:vendor-credential:update']"
                >
                  <Icon icon="ep:edit" class="mr-1" />编辑
                </el-button>
                <el-button
                  link
                  type="danger"
                  @click="handleCredDelete(row)"
                  v-hasPermi="['lunaerp:production:iot:vendor-credential:delete']"
                >
                  <Icon icon="ep:delete" class="mr-1" />删除
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无凭证数据" :image-size="60" />
            </template>
          </el-table>

          <Pagination
            :total="credTotal"
            v-model:page="credQueryParams.pageNo"
            v-model:limit="credQueryParams.pageSize"
            @pagination="getCredentialList"
          />
        </ContentWrap>
      </el-tab-pane>

      <!-- ==================== Tab 3: 设备绑定 ==================== -->
      <el-tab-pane label="设备绑定" name="iot-binding">
        <ContentWrap>
          <el-form :model="bindingQueryParams" ref="bindingQueryFormRef" :inline="true" class="-mb-15px">
            <el-form-item label="设备标识" prop="search">
              <el-input
                v-model="bindingQueryParams.search"
                placeholder="厂商设备标识"
                clearable
                @keyup.enter="handleBindingQuery"
                class="!w-200px"
              />
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select
                v-model="bindingQueryParams.status"
                placeholder="全部状态"
                clearable
                class="!w-120px"
              >
                <el-option label="启用" value="ACTIVE" />
                <el-option label="停用" value="INACTIVE" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button @click="handleBindingQuery">
                <Icon icon="ep:search" class="mr-1" />搜索
              </el-button>
              <el-button @click="resetBindingQuery">
                <Icon icon="ep:refresh" class="mr-1" />重置
              </el-button>
            </el-form-item>
          </el-form>
        </ContentWrap>

        <ContentWrap class="mt-15px">
          <div class="flex items-center justify-between mb-15px">
            <div class="flex items-center gap-2">
              <span class="text-16px font-bold">设备绑定列表</span>
              <span class="text-12px text-gray-400">（厂商侧设备标识 → 系统织机映射路由，同设备重复绑定严格受阻）</span>
            </div>
            <div class="flex items-center gap-2">
              <el-button
                type="primary"
                plain
                @click="openBindingDialog('create')"
                v-hasPermi="['lunaerp:production:iot:device-binding:create']"
              >
                <Icon icon="ep:plus" class="mr-1" /> 新增绑定
              </el-button>
              <el-button @click="getBindingList">
                <Icon icon="ep:refresh" class="mr-1" /> 刷新
              </el-button>
            </div>
          </div>

          <el-table
            v-loading="bindingLoading"
            :data="bindingList"
            border
            stripe
            class="mt-10px"
          >
            <el-table-column label="厂商侧设备标识" prop="vendorDeviceId" min-width="160">
              <template #default="{ row }">
                <span class="font-bold font-mono text-primary">{{ row.vendorDeviceId }}</span>
              </template>
            </el-table-column>
            <el-table-column label="绑定系统机台号" prop="loomNo" min-width="150">
              <template #default="{ row }">
                <el-tag v-if="row.loomNo" type="success">{{ row.loomNo }}</el-tag>
                <span v-else class="text-gray-400">ID: {{ row.loomId }}</span>
              </template>
            </el-table-column>
            <el-table-column label="织机型号" prop="loomModel" min-width="160" :show-overflow-tooltip="true">
              <template #default="{ row }">
                {{ row.loomModel || '—' }}
              </template>
            </el-table-column>
            <el-table-column label="绑定状态" prop="status" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
                  {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              label="最后同步时间"
              prop="lastSyncAt"
              width="170"
              align="center"
              :formatter="dateFormatter"
            />
            <el-table-column
              label="创建时间"
              prop="createTime"
              width="170"
              align="center"
              :formatter="dateFormatter"
            />
            <el-table-column label="操作" width="160" align="center" fixed="right">
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  @click="openBindingDialog('update', row)"
                  v-hasPermi="['lunaerp:production:iot:device-binding:update']"
                >
                  <Icon icon="ep:edit" class="mr-1" />编辑
                </el-button>
                <el-button
                  link
                  type="danger"
                  @click="handleBindingDelete(row)"
                  v-hasPermi="['lunaerp:production:iot:device-binding:delete']"
                >
                  <Icon icon="ep:delete" class="mr-1" />解绑
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无设备绑定数据" :image-size="60" />
            </template>
          </el-table>

          <Pagination
            :total="bindingTotal"
            v-model:page="bindingQueryParams.pageNo"
            v-model:limit="bindingQueryParams.pageSize"
            @pagination="getBindingList"
          />
        </ContentWrap>
      </el-tab-pane>

      <!-- ==================== Tab 4: 实时快照 ==================== -->
      <el-tab-pane label="实时快照" name="iot-realtime">
        <ContentWrap>
          <el-form :model="realtimeQueryParams" ref="realtimeQueryFormRef" :inline="true" class="-mb-15px">
            <el-form-item label="关键词" prop="search">
              <el-input
                v-model="realtimeQueryParams.search"
                placeholder="厂商设备标识 / 设备编号 / 在班人员"
                clearable
                @keyup.enter="handleRealtimeQuery"
                class="!w-240px"
              />
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select
                v-model="realtimeQueryParams.status"
                placeholder="全部状态"
                clearable
                class="!w-120px"
              >
                <el-option label="运行中 (running)" value="running" />
                <el-option label="待机 (idle)" value="idle" />
                <el-option label="异常 (abnormal)" value="abnormal" />
                <el-option label="停用 (disabled)" value="disabled" />
                <el-option label="离线 (offline)" value="offline" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button @click="handleRealtimeQuery">
                <Icon icon="ep:search" class="mr-1" />搜索
              </el-button>
              <el-button @click="resetRealtimeQuery">
                <Icon icon="ep:refresh" class="mr-1" />重置
              </el-button>
            </el-form-item>
          </el-form>
        </ContentWrap>

        <ContentWrap class="mt-15px">
          <div class="flex items-center justify-between mb-15px">
            <div class="flex items-center gap-2">
              <span class="text-16px font-bold">设备实时运行快照</span>
              <span class="text-12px text-gray-400">（只保留最新值，支持手工录入或模拟采集，快照绝不回写织机状态）</span>
            </div>
            <div class="flex items-center gap-2">
              <el-button
                type="primary"
                plain
                @click="openRealtimeDialog()"
                v-hasPermi="['lunaerp:production:iot:machine-realtime:record']"
              >
                <Icon icon="ep:edit-pen" class="mr-1" /> 手工录入快照
              </el-button>
              <el-button
                type="success"
                plain
                :loading="mockCollecting"
                @click="handleSimulatedCollect"
                v-hasPermi="['lunaerp:production:iot:machine-realtime:collect']"
              >
                <Icon icon="ep:cpu" class="mr-1" /> 模拟采集
              </el-button>
              <el-button @click="getRealtimeList">
                <Icon icon="ep:refresh" class="mr-1" /> 刷新
              </el-button>
            </div>
          </div>

          <el-table
            v-loading="realtimeLoading"
            :data="realtimeList"
            border
            stripe
            class="mt-10px"
          >
            <el-table-column label="厂商设备标识" prop="vendorDeviceId" min-width="150">
              <template #default="{ row }">
                <span class="font-bold font-mono text-primary">{{ row.vendorDeviceId }}</span>
              </template>
            </el-table-column>
            <el-table-column label="设备出厂编号" prop="deviceSn" min-width="140">
              <template #default="{ row }">
                {{ row.deviceSn || '—' }}
              </template>
            </el-table-column>
            <el-table-column label="绑定机台" prop="loomNo" min-width="120" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.loomNo" type="success" size="small">{{ row.loomNo }}</el-tag>
                <span v-else class="text-gray-400 text-12px">未关联</span>
              </template>
            </el-table-column>
            <el-table-column label="实时状态" prop="status" width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="getRealtimeStatusTag(row.status)" size="small">
                  {{ getRealtimeStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="运转效率" prop="runEfficiencyPct" width="110" align="right">
              <template #default="{ row }">
                <span v-if="row.runEfficiencyPct != null" class="font-bold text-green-600">
                  {{ row.runEfficiencyPct }}%
                </span>
                <span v-else>—</span>
              </template>
            </el-table-column>
            <el-table-column label="分钟圈速" prop="minuteCircleSpeed" width="110" align="right">
              <template #default="{ row }">
                <span v-if="row.minuteCircleSpeed != null" class="font-mono">
                  {{ row.minuteCircleSpeed }} RPM
                </span>
                <span v-else>—</span>
              </template>
            </el-table-column>
            <el-table-column label="班次圈数" prop="shiftCircles" width="120" align="right">
              <template #default="{ row }">
                {{ row.shiftCircles != null ? row.shiftCircles.toLocaleString() : '—' }}
              </template>
            </el-table-column>
            <el-table-column label="限制圈速" prop="limitSpeed" width="100" align="right">
              <template #default="{ row }">
                {{ row.limitSpeed != null ? row.limitSpeed + ' RPM' : '—' }}
              </template>
            </el-table-column>
            <el-table-column label="运转/在班分钟" width="130" align="center">
              <template #default="{ row }">
                {{ row.runMinutes != null ? row.runMinutes : '0' }} / {{ row.shiftMinutes != null ? row.shiftMinutes : '0' }} m
              </template>
            </el-table-column>
            <el-table-column label="在班人员" prop="staffOnShift" width="110" align="center">
              <template #default="{ row }">
                {{ row.staffOnShift || '—' }}
              </template>
            </el-table-column>
            <el-table-column
              label="采集/录入时间"
              prop="fetchedAt"
              width="170"
              align="center"
              :formatter="dateFormatter"
            />
            <el-table-column label="操作" width="120" align="center" fixed="right">
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  @click="openRealtimeDialog(row)"
                  v-hasPermi="['lunaerp:production:iot:machine-realtime:record']"
                >
                  <Icon icon="ep:edit-pen" class="mr-1" />手工更新
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无实时快照数据" :image-size="60" />
            </template>
          </el-table>

          <Pagination
            :total="realtimeTotal"
            v-model:page="realtimeQueryParams.pageNo"
            v-model:limit="realtimeQueryParams.pageSize"
            @pagination="getRealtimeList"
          />
        </ContentWrap>
      </el-tab-pane>
    </el-tabs>

    <!-- ==================== 织机新增 / 修改 对话框 ==================== -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="520px"
      append-to-body
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="90px"
      >
        <el-form-item label="机台号" prop="loomNo">
          <div class="flex items-center gap-2 w-full">
            <el-input
              v-model="formData.loomNo"
              placeholder="留空自动取号（如 LM20260001），或输入自定义编号"
              maxlength="32"
              class="flex-1"
            />
            <el-button
              v-if="dialogAction === 'create'"
              @click="handleFetchNextNo"
              type="info"
              plain
            >
              获取编号
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="型号" prop="model">
          <el-input
            v-model="formData.model"
            placeholder="例如：34″·28G / 喷水织机"
            maxlength="64"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio label="ACTIVE">启用</el-radio>
            <el-radio label="INACTIVE">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            type="textarea"
            v-model="formData.remark"
            placeholder="备注说明信息"
            :rows="3"
            maxlength="500"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="formSubmitting" @click="submitForm">
          确 定
        </el-button>
      </template>
    </el-dialog>

    <!-- ==================== 厂商凭证新增 / 修改 对话框 ==================== -->
    <el-dialog
      v-model="credDialogVisible"
      :title="credDialogTitle"
      width="520px"
      append-to-body
      destroy-on-close
    >
      <el-form
        ref="credFormRef"
        :model="credFormData"
        :rules="credFormRules"
        label-width="100px"
      >
        <el-form-item label="厂商账号" prop="vendorAccount">
          <el-input
            v-model="credFormData.vendorAccount"
            placeholder="请输入厂商API账号（租户内唯一）"
            maxlength="64"
          />
        </el-form-item>
        <el-form-item label="接入密码" prop="vendorPassword">
          <el-input
            v-model="credFormData.vendorPassword"
            type="password"
            show-password
            :placeholder="credDialogAction === 'create' ? '请输入密码（写入即加密，不回显明文）' : '留空则保持原密码不变'"
            maxlength="64"
          />
        </el-form-item>
        <el-form-item label="设备范围" prop="scope">
          <el-input
            v-model="credFormData.scope"
            placeholder="例如：1号车间织造大圆机"
            maxlength="255"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="credFormData.status">
            <el-radio label="ACTIVE">启用</el-radio>
            <el-radio label="INACTIVE">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="credDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="credSubmitting" @click="submitCredForm">
          确 定
        </el-button>
      </template>
    </el-dialog>

    <!-- ==================== 设备绑定新增 / 修改 对话框 ==================== -->
    <el-dialog
      v-model="bindingDialogVisible"
      :title="bindingDialogTitle"
      width="520px"
      append-to-body
      destroy-on-close
    >
      <el-form
        ref="bindingFormRef"
        :model="bindingFormData"
        :rules="bindingFormRules"
        label-width="110px"
      >
        <el-form-item label="厂商设备标识" prop="vendorDeviceId">
          <el-input
            v-model="bindingFormData.vendorDeviceId"
            placeholder="例如：DEV-ZX-001（厂商侧唯一设备编号）"
            maxlength="64"
          />
        </el-form-item>
        <el-form-item label="绑定织机" prop="loomId">
          <el-select
            v-model="bindingFormData.loomId"
            placeholder="请选择车间织机"
            filterable
            class="w-full"
          >
            <el-option
              v-for="loom in allLoomList"
              :key="loom.id"
              :label="`${loom.loomNo} (${loom.model || '标准'})`"
              :value="loom.id!"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="绑定状态" prop="status">
          <el-radio-group v-model="bindingFormData.status">
            <el-radio label="ACTIVE">启用</el-radio>
            <el-radio label="INACTIVE">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindingDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="bindingSubmitting" @click="submitBindingForm">
          确 定
        </el-button>
      </template>
    </el-dialog>

    <!-- ==================== 实时快照手工录入 对话框 ==================== -->
    <el-dialog
      v-model="realtimeDialogVisible"
      title="手工录入/更新实时快照"
      width="560px"
      append-to-body
      destroy-on-close
    >
      <el-form
        ref="realtimeFormRef"
        :model="realtimeFormData"
        :rules="realtimeFormRules"
        label-width="110px"
      >
        <el-form-item label="厂商设备标识" prop="vendorDeviceId">
          <el-input
            v-model="realtimeFormData.vendorDeviceId"
            placeholder="请输入厂商侧设备标识"
            maxlength="64"
          />
        </el-form-item>
        <el-form-item label="设备出厂编号" prop="deviceSn">
          <el-input
            v-model="realtimeFormData.deviceSn"
            placeholder="例如：SN-882201"
            maxlength="64"
          />
        </el-form-item>
        <el-form-item label="实时运行状态" prop="status">
          <el-select v-model="realtimeFormData.status" class="w-full">
            <el-option label="运行中 (running)" value="running" />
            <el-option label="待机 (idle)" value="idle" />
            <el-option label="异常 (abnormal)" value="abnormal" />
            <el-option label="停用 (disabled)" value="disabled" />
            <el-option label="离线 (offline)" value="offline" />
          </el-select>
        </el-form-item>
        <div class="grid grid-cols-2 gap-2">
          <el-form-item label="运转效率(%)" prop="runEfficiencyPct">
            <el-input-number
              v-model="realtimeFormData.runEfficiencyPct"
              :min="0"
              :max="100"
              :precision="2"
              class="!w-full"
            />
          </el-form-item>
          <el-form-item label="分钟圈速" prop="minuteCircleSpeed">
            <el-input-number
              v-model="realtimeFormData.minuteCircleSpeed"
              :min="0"
              :max="100"
              class="!w-full"
            />
          </el-form-item>
        </div>
        <div class="grid grid-cols-2 gap-2">
          <el-form-item label="班次圈数" prop="shiftCircles">
            <el-input-number
              v-model="realtimeFormData.shiftCircles"
              :min="0"
              class="!w-full"
            />
          </el-form-item>
          <el-form-item label="限制圈速" prop="limitSpeed">
            <el-input-number
              v-model="realtimeFormData.limitSpeed"
              :min="0"
              class="!w-full"
            />
          </el-form-item>
        </div>
        <div class="grid grid-cols-2 gap-2">
          <el-form-item label="运转分钟" prop="runMinutes">
            <el-input-number
              v-model="realtimeFormData.runMinutes"
              :min="0"
              class="!w-full"
            />
          </el-form-item>
          <el-form-item label="在班分钟" prop="shiftMinutes">
            <el-input-number
              v-model="realtimeFormData.shiftMinutes"
              :min="0"
              class="!w-full"
            />
          </el-form-item>
        </div>
        <el-form-item label="在班人员" prop="staffOnShift">
          <el-input
            v-model="realtimeFormData.staffOnShift"
            placeholder="例如：张工"
            maxlength="64"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="realtimeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="realtimeSubmitting" @click="submitRealtimeForm">
          保 存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { type FormInstance, type FormRules } from 'element-plus'
import { useMessage } from '@/hooks/web/useMessage'
import { dateFormatter } from '@/utils/formatTime'
import {
  getLoomPage,
  getLoomListAll,
  createLoom,
  updateLoom,
  deleteLoom,
  getNextLoomNo,
  type LoomVO,
  type LoomPageParam
} from '@/api/lunaerp-elmui/production/loom'
import {
  getVendorCredentialPage,
  createVendorCredential,
  updateVendorCredential,
  deleteVendorCredential,
  type VendorCredentialVO,
  type VendorCredentialPageParam,
  getDeviceBindingPage,
  createDeviceBinding,
  updateDeviceBinding,
  deleteDeviceBinding,
  type DeviceBindingVO,
  type DeviceBindingPageParam,
  getMachineRealtimePage,
  recordRealtimeData,
  triggerSimulatedCollection,
  type MachineRealtimeVO,
  type MachineRealtimePageParam
} from '@/api/lunaerp-elmui/production/iot'

defineOptions({ name: 'LunaerpElmuiProductionLoom' })

const message = useMessage()

const activeTab = ref('loom')

// ==================== 1. 织机列表 ====================
const loading = ref(false)
const list = ref<LoomVO[]>([])
const total = ref(0)
const allLoomList = ref<LoomVO[]>([])

// IoT 映射字典（用于主表辅助列展示）
const bindingMap = ref<Record<number, DeviceBindingVO>>({})
const realtimeMap = ref<Record<string, MachineRealtimeVO>>({})

const queryParams = reactive<LoomPageParam>({
  pageNo: 1,
  pageSize: 10,
  search: '',
  status: undefined
})
const queryFormRef = ref<FormInstance>()

const getList = async () => {
  loading.value = true
  try {
    const data = await getLoomPage(queryParams)
    list.value = data.list
    total.value = data.total
    // 异步加载绑定与快照映射作为辅助列
    loadAuxiliaryIotData()
  } finally {
    loading.value = false
  }
}

const loadAuxiliaryIotData = async () => {
  try {
    const bindings = await getDeviceBindingPage({ pageNo: 1, pageSize: 200 })
    const bMap: Record<number, DeviceBindingVO> = {}
    if (bindings && bindings.list) {
      for (const b of bindings.list) {
        if (b.loomId) {
          bMap[b.loomId] = b
        }
      }
    }
    bindingMap.value = bMap

    const realtimes = await getMachineRealtimePage({ pageNo: 1, pageSize: 200 })
    const rMap: Record<string, MachineRealtimeVO> = {}
    if (realtimes && realtimes.list) {
      for (const r of realtimes.list) {
        if (r.vendorDeviceId) {
          rMap[r.vendorDeviceId] = r
        }
      }
    }
    realtimeMap.value = rMap
  } catch {}
}

const loadAllLooms = async () => {
  try {
    allLoomList.value = await getLoomListAll()
  } catch {}
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  queryParams.pageNo = 1
  getList()
}

// 织机弹窗
const dialogVisible = ref(false)
const dialogAction = ref<'create' | 'update'>('create')
const dialogTitle = ref('')
const formSubmitting = ref(false)
const formRef = ref<FormInstance>()

const formData = reactive<LoomVO>({
  id: undefined,
  loomNo: '',
  model: '',
  status: 'ACTIVE',
  remark: ''
})

const formRules = reactive<FormRules>({
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})

const openDialog = (action: 'create' | 'update', row?: LoomVO) => {
  dialogAction.value = action
  dialogTitle.value = action === 'create' ? '新增织机' : '修改织机'
  dialogVisible.value = true
  if (action === 'create') {
    Object.assign(formData, {
      id: undefined,
      loomNo: '',
      model: '',
      status: 'ACTIVE',
      remark: ''
    })
  } else if (row) {
    Object.assign(formData, {
      id: row.id,
      loomNo: row.loomNo,
      model: row.model,
      status: row.status,
      remark: row.remark
    })
  }
}

const handleFetchNextNo = async () => {
  try {
    const nextNo = await getNextLoomNo()
    formData.loomNo = nextNo
  } catch {}
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  formSubmitting.value = true
  try {
    if (dialogAction.value === 'create') {
      await createLoom(formData)
      message.success('新增织机成功')
    } else {
      await updateLoom(formData)
      message.success('修改织机成功')
    }
    dialogVisible.value = false
    await getList()
    loadAllLooms()
  } finally {
    formSubmitting.value = false
  }
}

const handleToggleStatus = async (row: LoomVO, newStatus: string) => {
  const actionText = newStatus === 'ACTIVE' ? '启用' : '停用'
  try {
    await message.confirm(`确认要${actionText}机台【${row.loomNo}】吗？${newStatus === 'INACTIVE' ? '停用后不可被新机台任务选择。' : ''}`)
    await updateLoom({
      id: row.id,
      status: newStatus
    })
    message.success(`${actionText}成功`)
    await getList()
  } catch {}
}

const handleDelete = async (row: LoomVO) => {
  try {
    await message.confirm(`确认要删除织机【${row.loomNo}】吗？若已被机台任务引用将禁止删除，引导改用停用。`)
    await deleteLoom(row.id!)
    message.success('删除织机成功')
    await getList()
    loadAllLooms()
  } catch {}
}

// ==================== 2. 设备接入凭证 ====================
const credLoading = ref(false)
const credList = ref<VendorCredentialVO[]>([])
const credTotal = ref(0)
const credQueryParams = reactive<VendorCredentialPageParam>({
  pageNo: 1,
  pageSize: 10,
  search: '',
  status: undefined
})
const credQueryFormRef = ref<FormInstance>()

const getCredentialList = async () => {
  credLoading.value = true
  try {
    const data = await getVendorCredentialPage(credQueryParams)
    credList.value = data.list
    credTotal.value = data.total
  } finally {
    credLoading.value = false
  }
}

const handleCredQuery = () => {
  credQueryParams.pageNo = 1
  getCredentialList()
}

const resetCredQuery = () => {
  credQueryFormRef.value?.resetFields()
  credQueryParams.pageNo = 1
  getCredentialList()
}

const credDialogVisible = ref(false)
const credDialogAction = ref<'create' | 'update'>('create')
const credDialogTitle = ref('')
const credSubmitting = ref(false)
const credFormRef = ref<FormInstance>()

const credFormData = reactive<VendorCredentialVO>({
  id: undefined,
  vendorAccount: '',
  vendorPassword: '',
  scope: '',
  status: 'ACTIVE'
})

const credFormRules = reactive<FormRules>({
  vendorAccount: [{ required: true, message: '厂商账号不能为空', trigger: 'blur' }]
})

const openCredDialog = (action: 'create' | 'update', row?: VendorCredentialVO) => {
  credDialogAction.value = action
  credDialogTitle.value = action === 'create' ? '新增接入凭证' : '修改接入凭证'
  credDialogVisible.value = true
  if (action === 'create') {
    Object.assign(credFormData, {
      id: undefined,
      vendorAccount: '',
      vendorPassword: '',
      scope: '',
      status: 'ACTIVE'
    })
  } else if (row) {
    Object.assign(credFormData, {
      id: row.id,
      vendorAccount: row.vendorAccount,
      vendorPassword: '', // 编辑时不回显原密码
      scope: row.scope,
      status: row.status
    })
  }
}

const submitCredForm = async () => {
  if (!credFormRef.value) return
  await credFormRef.value.validate()
  credSubmitting.value = true
  try {
    if (credDialogAction.value === 'create') {
      await createVendorCredential(credFormData)
      message.success('新增接入凭证成功（密码已加密存储）')
    } else {
      await updateVendorCredential(credFormData)
      message.success('修改接入凭证成功')
    }
    credDialogVisible.value = false
    await getCredentialList()
  } finally {
    credSubmitting.value = false
  }
}

const handleCredDelete = async (row: VendorCredentialVO) => {
  try {
    await message.confirm(`确认要删除厂商账号【${row.vendorAccount}】的接入凭证吗？`)
    await deleteVendorCredential(row.id!)
    message.success('删除凭证成功')
    await getCredentialList()
  } catch {}
}

// ==================== 3. 设备绑定 ====================
const bindingLoading = ref(false)
const bindingList = ref<DeviceBindingVO[]>([])
const bindingTotal = ref(0)
const bindingQueryParams = reactive<DeviceBindingPageParam>({
  pageNo: 1,
  pageSize: 10,
  search: '',
  status: undefined
})
const bindingQueryFormRef = ref<FormInstance>()

const getBindingList = async () => {
  bindingLoading.value = true
  try {
    const data = await getDeviceBindingPage(bindingQueryParams)
    bindingList.value = data.list
    bindingTotal.value = data.total
  } finally {
    bindingLoading.value = false
  }
}

const handleBindingQuery = () => {
  bindingQueryParams.pageNo = 1
  getBindingList()
}

const resetBindingQuery = () => {
  bindingQueryFormRef.value?.resetFields()
  bindingQueryParams.pageNo = 1
  getBindingList()
}

const bindingDialogVisible = ref(false)
const bindingDialogAction = ref<'create' | 'update'>('create')
const bindingDialogTitle = ref('')
const bindingSubmitting = ref(false)
const bindingFormRef = ref<FormInstance>()

const bindingFormData = reactive<DeviceBindingVO>({
  id: undefined,
  vendorDeviceId: '',
  loomId: undefined as unknown as number,
  status: 'ACTIVE'
})

const bindingFormRules = reactive<FormRules>({
  vendorDeviceId: [{ required: true, message: '厂商设备标识不能为空', trigger: 'blur' }],
  loomId: [{ required: true, message: '请选择绑定的织机', trigger: 'change' }]
})

const openBindingDialog = (action: 'create' | 'update', row?: DeviceBindingVO) => {
  bindingDialogAction.value = action
  bindingDialogTitle.value = action === 'create' ? '新增设备绑定' : '修改设备绑定'
  bindingDialogVisible.value = true
  loadAllLooms()
  if (action === 'create') {
    Object.assign(bindingFormData, {
      id: undefined,
      vendorDeviceId: '',
      loomId: undefined,
      status: 'ACTIVE'
    })
  } else if (row) {
    Object.assign(bindingFormData, {
      id: row.id,
      vendorDeviceId: row.vendorDeviceId,
      loomId: row.loomId,
      status: row.status
    })
  }
}

const submitBindingForm = async () => {
  if (!bindingFormRef.value) return
  await bindingFormRef.value.validate()
  bindingSubmitting.value = true
  try {
    if (bindingDialogAction.value === 'create') {
      await createDeviceBinding(bindingFormData)
      message.success('设备绑定成功')
    } else {
      await updateDeviceBinding(bindingFormData)
      message.success('修改绑定成功')
    }
    bindingDialogVisible.value = false
    await getBindingList()
    loadAuxiliaryIotData()
  } finally {
    bindingSubmitting.value = false
  }
}

const handleBindingDelete = async (row: DeviceBindingVO) => {
  try {
    await message.confirm(`确认要解绑设备【${row.vendorDeviceId}】与织机【${row.loomNo || row.loomId}】的映射关系吗？`)
    await deleteDeviceBinding(row.id!)
    message.success('解绑成功')
    await getBindingList()
    loadAuxiliaryIotData()
  } catch {}
}

// ==================== 4. 实时快照 ====================
const realtimeLoading = ref(false)
const realtimeList = ref<MachineRealtimeVO[]>([])
const realtimeTotal = ref(0)
const mockCollecting = ref(false)

const realtimeQueryParams = reactive<MachineRealtimePageParam>({
  pageNo: 1,
  pageSize: 10,
  search: '',
  status: undefined
})
const realtimeQueryFormRef = ref<FormInstance>()

const getRealtimeList = async () => {
  realtimeLoading.value = true
  try {
    const data = await getMachineRealtimePage(realtimeQueryParams)
    realtimeList.value = data.list
    realtimeTotal.value = data.total
  } finally {
    realtimeLoading.value = false
  }
}

const handleRealtimeQuery = () => {
  realtimeQueryParams.pageNo = 1
  getRealtimeList()
}

const resetRealtimeQuery = () => {
  realtimeQueryFormRef.value?.resetFields()
  realtimeQueryParams.pageNo = 1
  getRealtimeList()
}

const handleSimulatedCollect = async () => {
  mockCollecting.value = true
  try {
    const count = await triggerSimulatedCollection()
    message.success(`模拟采集完成，已更新 ${count} 台设备实时快照`)
    await getRealtimeList()
    loadAuxiliaryIotData()
  } finally {
    mockCollecting.value = false
  }
}

const realtimeDialogVisible = ref(false)
const realtimeSubmitting = ref(false)
const realtimeFormRef = ref<FormInstance>()

const realtimeFormData = reactive<MachineRealtimeVO>({
  vendorDeviceId: '',
  deviceSn: '',
  status: 'running',
  runEfficiencyPct: 90.0,
  minuteCircleSpeed: 24,
  shiftCircles: 10000,
  limitSpeed: 28,
  runMinutes: 400,
  shiftMinutes: 480,
  staffOnShift: ''
})

const realtimeFormRules = reactive<FormRules>({
  vendorDeviceId: [{ required: true, message: '厂商设备标识不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})

const openRealtimeDialog = (row?: MachineRealtimeVO) => {
  realtimeDialogVisible.value = true
  if (row) {
    Object.assign(realtimeFormData, {
      vendorDeviceId: row.vendorDeviceId,
      deviceSn: row.deviceSn || '',
      status: row.status || 'running',
      runEfficiencyPct: row.runEfficiencyPct != null ? row.runEfficiencyPct : 90.0,
      minuteCircleSpeed: row.minuteCircleSpeed != null ? row.minuteCircleSpeed : 24,
      shiftCircles: row.shiftCircles != null ? row.shiftCircles : 10000,
      limitSpeed: row.limitSpeed != null ? row.limitSpeed : 28,
      runMinutes: row.runMinutes != null ? row.runMinutes : 400,
      shiftMinutes: row.shiftMinutes != null ? row.shiftMinutes : 480,
      staffOnShift: row.staffOnShift || ''
    })
  } else {
    Object.assign(realtimeFormData, {
      vendorDeviceId: '',
      deviceSn: '',
      status: 'running',
      runEfficiencyPct: 92.5,
      minuteCircleSpeed: 26,
      shiftCircles: 12000,
      limitSpeed: 28,
      runMinutes: 420,
      shiftMinutes: 480,
      staffOnShift: ''
    })
  }
}

const submitRealtimeForm = async () => {
  if (!realtimeFormRef.value) return
  await realtimeFormRef.value.validate()
  realtimeSubmitting.value = true
  try {
    await recordRealtimeData(realtimeFormData)
    message.success('快照录入成功（已更新最新值，未修改织机档案状态）')
    realtimeDialogVisible.value = false
    await getRealtimeList()
    loadAuxiliaryIotData()
  } finally {
    realtimeSubmitting.value = false
  }
}

const REALTIME_STATUS_MAP: Record<string, { tag: '' | 'success' | 'warning' | 'danger' | 'info'; label: string }> = {
  running: { tag: 'success', label: '运行中' },
  idle: { tag: 'warning', label: '待机' },
  abnormal: { tag: 'danger', label: '异常' },
  disabled: { tag: 'info', label: '停用' },
  offline: { tag: 'info', label: '离线' }
}

const getRealtimeStatusTag = (status?: string) => {
  return (status && REALTIME_STATUS_MAP[status]?.tag) || 'info'
}

const getRealtimeStatusText = (status?: string) => {
  return (status && REALTIME_STATUS_MAP[status]?.label) || status || '—'
}

const handleTabChange = (tabName: string) => {
  if (tabName === 'loom') {
    getList()
  } else if (tabName === 'iot-credential') {
    getCredentialList()
  } else if (tabName === 'iot-binding') {
    getBindingList()
  } else if (tabName === 'iot-realtime') {
    getRealtimeList()
  }
}

onMounted(() => {
  getList()
  loadAllLooms()
})
</script>

<style scoped>
.loom-container {
  padding: 0;
}
</style>
