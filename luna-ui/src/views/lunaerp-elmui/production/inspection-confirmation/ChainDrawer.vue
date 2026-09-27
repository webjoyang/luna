<template>
  <el-drawer
    v-model="visible"
    title="认定链详情"
    size="580px"
    destroy-on-close
    append-to-body
  >
    <div v-loading="loading" class="chain-drawer-container">
      <!-- 头部：当前有效行摘要 -->
      <div class="mb-4 bg-gray-50 dark:bg-gray-800 p-3 rounded border border-gray-200 dark:border-gray-700">
        <div class="flex justify-between items-center mb-2">
          <span class="text-sm font-semibold text-gray-700 dark:text-gray-300">当前有效行摘要</span>
          <DictTag
            v-if="currentRecord?.status"
            :type="DICT_TYPE.LUNAERP_INSPECTION_CONFIRMATION_STATUS"
            :value="currentRecord.status"
          />
        </div>
        <el-descriptions :column="2" size="small" border>
          <el-descriptions-item label="卷号">
            <span class="font-semibold">{{ currentRecord?.rollNo || '—' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="缸号">{{ currentRecord?.vatNo || '—' }}</el-descriptions-item>
          <el-descriptions-item label="等级">
            <DictTag
              v-if="currentRecord?.grade"
              :type="DICT_TYPE.LUNAERP_INSPECTION_GRADE"
              :value="currentRecord.grade"
            />
            <span v-else>—</span>
          </el-descriptions-item>
          <el-descriptions-item label="机台号">{{ currentRecord?.machineNo || '—' }}</el-descriptions-item>
          <el-descriptions-item label="产品名称" :span="2">
            {{ currentRecord?.productName || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="来源单号" :span="2">
            {{ currentRecord?.inspectionOrderCode || (currentRecord?.source === 'MANUAL' ? '手工登记' : '—') }}
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <el-divider content-position="left">认定历史链（共 {{ chainList.length }} 轮次）</el-divider>

      <!-- 时间线：按创建时间正序列出同链全部行 -->
      <div v-if="chainList.length > 0" class="px-2">
        <el-timeline>
          <el-timeline-item
            v-for="(item, index) in chainList"
            :key="item.id"
            :timestamp="formatTime(item.createTime)"
            placement="top"
            :type="getTimelineType(item.status)"
            :hollow="item.status === 'PENDING'"
          >
            <el-card
              shadow="hover"
              :class="[
                'chain-card',
                item.status === 'REVOKED' ? 'chain-card-revoked' : '',
                item.id === currentRecord?.id ? 'chain-card-current' : ''
              ]"
            >
              <div class="flex justify-between items-center mb-2">
                <span class="font-semibold text-sm">
                  第 {{ index + 1 }} 轮
                  <el-tag
                    v-if="item.id === currentRecord?.id"
                    size="small"
                    type="warning"
                    effect="plain"
                    class="ml-2"
                  >
                    当前行
                  </el-tag>
                </span>
                <DictTag
                  :type="DICT_TYPE.LUNAERP_INSPECTION_CONFIRMATION_STATUS"
                  :value="item.status"
                />
              </div>

              <!-- 记录详情明细 -->
              <div class="text-xs space-y-1 text-gray-600 dark:text-gray-300">
                <div class="flex items-center">
                  <span class="w-20 text-gray-400">责任类型：</span>
                  <DictTag
                    v-if="item.responsibility"
                    :type="DICT_TYPE.LUNAERP_INSPECTION_RESPONSIBILITY"
                    :value="item.responsibility"
                  />
                  <span v-else class="text-gray-400">待认定</span>
                </div>
                <div class="flex items-center">
                  <span class="w-20 text-gray-400">挡车工：</span>
                  <span>{{ item.workerName || '—' }}</span>
                </div>
                <div v-if="item.confirmedByName" class="flex items-center">
                  <span class="w-20 text-gray-400">认定人：</span>
                  <span>{{ item.confirmedByName }}（{{ formatTime(item.confirmedAt) }}）</span>
                </div>
                <div v-if="item.revokedByName" class="flex items-center text-red-500">
                  <span class="w-20 text-red-400">撤销人：</span>
                  <span>{{ item.revokedByName }}（{{ formatTime(item.revokedAt) }}）</span>
                </div>
                <div v-if="item.note" class="flex items-start">
                  <span class="w-20 text-gray-400 flex-shrink-0">原因备注：</span>
                  <span class="break-all">{{ item.note }}</span>
                </div>
              </div>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </div>

      <el-empty v-else description="暂无认定链记录" :image-size="80" />
    </div>
  </el-drawer>
</template>

<script lang="ts" setup>
import { formatDate } from '@/utils/formatTime'
import { DICT_TYPE } from '@/utils/dict'
import * as InspectionConfirmationApi from '@/api/lunaerp-elmui/production/inspection-confirmation'
import type { InspectionConfirmationVO } from '@/api/lunaerp-elmui/production/inspection-confirmation'

const visible = ref(false)
const loading = ref(false)
const currentRecord = ref<InspectionConfirmationVO | null>(null)
const chainList = ref<InspectionConfirmationVO[]>([])

/** 时间格式化 */
const formatTime = (time: string | number | Date | null | undefined) => {
  if (!time) return '—'
  return formatDate(time) || '—'
}

/** 时间线节点类型 */
const getTimelineType = (status: string) => {
  switch (status) {
    case 'CONFIRMED':
      return 'success'
    case 'PENDING':
      return 'primary'
    case 'REVOKED':
    default:
      return 'info'
  }
}

/** 打开认定链抽屉 */
const open = async (id: number) => {
  visible.value = true
  loading.value = true
  currentRecord.value = null
  chainList.value = []
  try {
    const data = await InspectionConfirmationApi.getInspectionConfirmation(id)
    const records = data?.chainRecords || []
    chainList.value = records
    // 头部展示当前认定链上的有效行（非 REVOKED 行），如全撤销则保底为查询返回行
    const active = records.find((r) => r.status !== 'REVOKED') || data
    currentRecord.value = active
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>

<style lang="scss" scoped>
.chain-drawer-container {
  padding-bottom: 24px;
}

.chain-card {
  border-radius: 6px;
  transition: all 0.2s ease;
}

.chain-card-current {
  border-left: 3px solid var(--el-color-primary);
}

.chain-card-revoked {
  background-color: var(--el-fill-color-lighter);
  opacity: 0.72;
  border-color: var(--el-border-color-lighter);
}
</style>
