<template>
  <div class="h5-inspection-order-page min-h-screen bg-gray-50 pb-20">
    <!-- 顶部导航栏 -->
    <van-nav-bar
      title="检验单详情"
      left-arrow
      fixed
      placeholder
      safe-area-inset-top
      @click-left="goBack"
    />

    <!-- 内容区域 -->
    <div class="p-3">
      <!-- 1. 加载中状态 -->
      <div v-if="loading" class="flex flex-col items-center justify-center py-20 text-gray-400">
        <van-loading type="spinner" color="#1989fa" size="32px" />
        <span class="text-13px mt-3">正在读取检验单详情...</span>
      </div>

      <!-- 2. 错误/查无此单状态 -->
      <div v-else-if="errorMessage" class="bg-white rounded-xl p-6 shadow-xs text-center my-4">
        <van-empty image="error" :description="errorMessage" />
        <div class="mt-4 max-w-xs mx-auto">
          <van-button type="primary" block round @click="goBack"> 返回质检列表 </van-button>
        </div>
      </div>

      <!-- 3. 检验单详情展示 -->
      <div v-else-if="order" class="space-y-3">
        <!-- 只读提示 Banner（已完成单据只读） -->
        <van-notice-bar
          v-if="order.readonly"
          left-icon="info-o"
          color="#07c160"
          background="#edfbf3"
          text="提示：该检验单已录检完成，当前处于只读模式"
          class="rounded-lg shadow-xs"
        />
        <van-notice-bar
          v-else
          left-icon="records-o"
          color="#1989fa"
          background="#ecf9ff"
          text="提示：检验单录检中，支持现场查看与拍照核验"
          class="rounded-lg shadow-xs"
        />

        <!-- 主表头卡片 -->
        <div class="order-header-card bg-white rounded-xl p-4 shadow-xs border border-gray-100">
          <div class="flex items-center justify-between pb-3 border-b border-gray-100">
            <div>
              <span class="text-11px text-gray-400">检验单号</span>
              <div class="font-mono text-17px font-bold text-gray-900 mt-0.5 break-all">
                {{ order.code }}
              </div>
            </div>
            <div class="flex items-center space-x-1.5">
              <van-tag
                :color="order.type === 'GREIGE' ? '#1989fa' : '#7232dd'"
                plain
                round
                size="medium"
              >
                {{ order.type === 'GREIGE' ? '胚布' : '成品' }}
              </van-tag>
              <van-tag :type="getStatusTagType(order.status)" round size="medium">
                {{ getStatusLabel(order.status) }}
              </van-tag>
            </div>
          </div>

          <!-- 核心字段网格 -->
          <div class="grid grid-cols-2 gap-3 pt-3 text-13px">
            <div>
              <span class="text-gray-400 text-12px block">产品品名</span>
              <div class="font-medium text-gray-800 mt-0.5 truncate">
                {{ order.productName || '—' }}
              </div>
            </div>
            <div>
              <span class="text-gray-400 text-12px block">产品编码</span>
              <div class="font-mono text-gray-800 mt-0.5 truncate">
                {{ order.productCode || '—' }}
              </div>
            </div>
            <div>
              <span class="text-gray-400 text-12px block">承检方</span>
              <div class="text-gray-800 mt-0.5 truncate">
                {{ order.inspectionFactoryName || '本厂' }}
              </div>
            </div>
            <div>
              <span class="text-gray-400 text-12px block">包装模式</span>
              <div class="text-gray-800 mt-0.5">
                {{ order.packageMode === 'PIECE' ? '匹装 (PIECE)' : '卷装 (ROLL)' }}
              </div>
            </div>
            <div>
              <span class="text-gray-400 text-12px block">目标重量</span>
              <div class="font-mono text-gray-800 mt-0.5">
                {{ order.targetWeight ? order.targetWeight + ' kg' : '未定' }}
              </div>
            </div>
            <div>
              <span class="text-gray-400 text-12px block">单据日期</span>
              <div class="text-gray-800 mt-0.5 font-mono">
                {{ order.orderDate || '—' }}
              </div>
            </div>
          </div>

          <!-- 备注 -->
          <div v-if="order.remark" class="mt-3 pt-2.5 border-t border-gray-50 text-12px">
            <span class="text-gray-400">备注：</span>
            <span class="text-gray-600">{{ order.remark }}</span>
          </div>
        </div>

        <!-- 胚布扫码录检入口与占位卡片 -->
        <div
          v-if="isGreigePieceMode"
          class="mode-action-card bg-white rounded-xl p-4 shadow-xs border border-primary/30"
        >
          <div class="flex items-center justify-between mb-3">
            <div class="flex items-center space-x-2">
              <div
                class="w-8 h-8 rounded-full bg-blue-50 text-primary flex items-center justify-center"
              >
                <van-icon name="scan" size="18" />
              </div>
              <div>
                <span class="text-14px font-bold text-gray-900">胚布扫码录检</span>
                <span class="text-11px text-primary block mt-0.5">
                  已接入 14 号 ticket-scan 自动交接编排
                </span>
              </div>
            </div>
            <van-button
              v-hasPermi="['lunaerp:workshop:h5:inspection']"
              v-if="!order.readonly"
              type="primary"
              size="small"
              icon="scan"
              round
              @click="triggerScanner"
            >
              扫布票码
            </van-button>
          </div>

          <!-- 扫码定位到新行占位提示 -->
          <div
            v-if="currentScannedTicket && !currentScannedItem"
            class="scan-hit-alert bg-amber-50 border border-amber-200 rounded-lg p-3 text-12px text-amber-900 mb-3"
          >
            <div class="flex items-center font-bold mb-1">
              <van-icon name="passed" color="#ed6a0c" class="mr-1" />
              已自动交接建单！待录检新行
            </div>
            <div
              >布票编号：<span class="font-mono font-medium">{{ currentScannedTicket }}</span></div
            >
            <div
              >建议匹号：<span class="font-mono font-medium">{{
                suggestedPieceNo || currentScannedTicket
              }}</span></div
            >
          </div>

          <!-- 扫码定位到已有录检行提示 -->
          <div
            v-else-if="currentScannedTicket && currentScannedItem"
            class="scan-hit-alert bg-blue-50 border border-blue-200 rounded-lg p-3 text-12px text-blue-900 mb-3"
          >
            <div class="flex items-center font-bold mb-1">
              <van-icon name="checked" color="#1989fa" class="mr-1" />
              已定位到布票既有录检行（改录模式）
            </div>
            <div
              >布票编号：<span class="font-mono font-medium">{{ currentScannedTicket }}</span></div
            >
            <div
              >匹号：<span class="font-mono font-medium">{{
                currentScannedItem.pieceNo || currentScannedTicket
              }}</span></div
            >
          </div>

          <p v-else class="text-12px text-gray-500 bg-gray-50 rounded-lg p-2.5">
            扫描挡车工完工布票二维码，系统自动归一关联胚布检验单并定位录检行。
          </p>

          <!-- 胚布录检表单（新建 / 改录） -->
          <div
            v-if="currentScannedTicket && !order.readonly"
            class="inspection-form-card pt-3 border-t border-gray-100"
          >
            <div class="flex items-center justify-between mb-3">
              <span class="text-13px font-bold text-gray-900 flex items-center">
                <van-icon :name="currentScannedItem ? 'edit' : 'add-o'" class="mr-1 text-primary" />
                {{ currentScannedItem ? '改录原行明细' : '录入新行明细' }}
              </span>
              <van-tag :type="currentScannedItem ? 'primary' : 'warning'" size="small" round>
                {{ currentScannedItem ? '改录' : '新建' }}
              </van-tag>
            </div>

            <van-form @submit="handleSaveItem">
              <van-cell-group
                inset
                class="!mx-0 mb-3 border border-gray-100 rounded-lg overflow-hidden"
              >
                <!-- 匹号（缺省布票号，可修改） -->
                <van-field
                  v-model="form.pieceNo"
                  label="匹号"
                  placeholder="请输入匹号（缺省布票码）"
                  clearable
                  :rules="[{ required: true, message: '匹号不能为空' }]"
                />

                <!-- 缸号（可填可空） -->
                <van-field v-model="form.vatNo" label="缸号" placeholder="胚布可选填" clearable />

                <!-- 检验等级（字典 2232: A / B / C / DEFECTIVE） -->
                <van-field name="grade" label="检验等级" required>
                  <template #input>
                    <van-radio-group
                      v-model="form.grade"
                      direction="horizontal"
                      class="flex flex-wrap gap-2"
                    >
                      <van-radio name="A">一等品(A)</van-radio>
                      <van-radio name="B">二等品(B)</van-radio>
                      <van-radio name="C">三等品(C)</van-radio>
                      <van-radio name="DEFECTIVE">等外品</van-radio>
                    </van-radio-group>
                  </template>
                </van-field>

                <!-- 实称重量 KG -->
                <van-field
                  v-model="form.measuredWeight"
                  type="number"
                  label="实称重量"
                  placeholder="请输入实称重量"
                  required
                  :rules="[
                    { required: true, message: '实称重量不能为空' },
                    { validator: (val) => Number(val) > 0, message: '重量必须大于 0' }
                  ]"
                >
                  <template #extra>
                    <span class="text-gray-400 text-12px">kg</span>
                  </template>
                </van-field>

                <!-- 实测米数 -->
                <van-field
                  v-model="form.lengthM"
                  type="number"
                  label="实测米数"
                  placeholder="请输入实测米数"
                  :rules="[
                    { validator: (val) => !val || Number(val) > 0, message: '米数必须大于 0' }
                  ]"
                >
                  <template #extra>
                    <span class="text-gray-400 text-12px">m</span>
                  </template>
                </van-field>

                <!-- 现场瑕疵照片（<=6 张） -->
                <div class="px-4 py-2.5">
                  <H5PhotoUploader
                    v-model="form.photos"
                    :max-count="6"
                    directory="workshop/inspection"
                  />
                </div>

                <!-- 备注 -->
                <van-field
                  v-model="form.remark"
                  label="录检备注"
                  placeholder="选填，现场瑕疵/布面情况说明"
                  rows="2"
                  autosize
                  type="textarea"
                  maxlength="200"
                  show-word-limit
                />
              </van-cell-group>

              <!-- 提交按钮与放弃修改按钮 -->
              <div class="flex items-center space-x-2 pt-1">
                <van-button
                  v-if="currentScannedItem"
                  v-hasPermi="['lunaerp:workshop:h5:inspection']"
                  plain
                  size="small"
                  round
                  type="default"
                  class="flex-1"
                  @click="resetFormToNew"
                >
                  取消改录
                </van-button>
                <van-button
                  v-hasPermi="['lunaerp:workshop:h5:inspection']"
                  type="primary"
                  size="small"
                  round
                  block
                  :loading="submitting"
                  loading-text="正在保存..."
                  native-type="submit"
                  class="flex-2"
                >
                  {{ currentScannedItem ? '保存改录' : '保存录检明细' }}
                </van-button>
              </div>
            </van-form>
          </div>
        </div>

        <!-- 成品 ROLL 模式录检卡片 -->
        <div
          v-else
          class="mode-action-card bg-white rounded-xl p-4 shadow-xs border border-primary/30"
        >
          <div class="flex items-center justify-between mb-3">
            <div class="flex items-center space-x-2">
              <div
                class="w-8 h-8 rounded-full bg-blue-50 text-primary flex items-center justify-center"
              >
                <van-icon name="records-o" size="18" />
              </div>
              <div>
                <span class="text-14px font-bold text-gray-900">成品逐卷录检</span>
                <span class="text-11px text-primary block mt-0.5">
                  输入/扫码卷号，自动记忆缸号与查重定位
                </span>
              </div>
            </div>
            <div class="flex items-center space-x-2">
              <van-tag type="primary" plain round size="small">ROLL</van-tag>
              <van-button
                v-hasPermi="['lunaerp:workshop:h5:inspection']"
                v-if="!order.readonly"
                type="primary"
                size="small"
                icon="scan"
                round
                @click="triggerScanner"
              >
                扫卷码
              </van-button>
            </div>
          </div>

          <!-- 扫码/查重命中已有行改录提示 -->
          <div
            v-if="currentScannedItem"
            class="scan-hit-alert bg-blue-50 border border-blue-200 rounded-lg p-3 text-12px text-blue-900 mb-3"
          >
            <div class="flex items-center font-bold mb-1">
              <van-icon name="checked" color="#1989fa" class="mr-1" />
              已定位到已有录检行（改录模式）
            </div>
            <div
              >缸号：<span class="font-mono font-medium">{{ form.vatNo }}</span
              >，卷号：<span class="font-mono font-medium">{{ form.rollNo }}</span></div
            >
          </div>
          <div
            v-else-if="form.rollNo"
            class="scan-hit-alert bg-amber-50 border border-amber-200 rounded-lg p-3 text-12px text-amber-900 mb-3"
          >
            <div class="flex items-center font-bold mb-1">
              <van-icon name="passed" color="#ed6a0c" class="mr-1" />
              新卷录入模式
            </div>
            <div
              >缸号：<span class="font-mono font-medium">{{ form.vatNo || '待输入' }}</span
              >，卷号：<span class="font-mono font-medium">{{ form.rollNo }}</span></div
            >
          </div>
          <p v-else class="text-12px text-gray-500 bg-gray-50 rounded-lg p-2.5 mb-3">
            现场逐卷录检：输入/扫码卷号，同 (缸号,卷号) 自动定位改录；缸号自动记忆上次录入值。
          </p>

          <!-- 成品录检表单（仅非只读展示） -->
          <div v-if="!order.readonly" class="inspection-form-card pt-1 border-t border-gray-100">
            <div class="flex items-center justify-between mb-3 mt-2">
              <span class="text-13px font-bold text-gray-900 flex items-center">
                <van-icon :name="currentScannedItem ? 'edit' : 'add-o'" class="mr-1 text-primary" />
                {{ currentScannedItem ? '改录原行明细' : '录入新卷明细' }}
              </span>
              <van-tag :type="currentScannedItem ? 'primary' : 'warning'" size="small" round>
                {{ currentScannedItem ? '改录' : '新建' }}
              </van-tag>
            </div>

            <van-form @submit="handleSaveItem">
              <van-cell-group
                inset
                class="!mx-0 mb-3 border border-gray-100 rounded-lg overflow-hidden"
              >
                <!-- 缸号（必填，记忆上次值） -->
                <van-field
                  v-model="form.vatNo"
                  label="缸号"
                  placeholder="请输入缸号（自动记忆）"
                  clearable
                  required
                  :rules="[{ required: true, message: '缸号不能为空' }]"
                  @blur="handleRollNoOrVatNoChange"
                />

                <!-- 卷号（必填，支持扫码和手输） -->
                <van-field
                  v-model="form.rollNo"
                  label="卷号"
                  placeholder="请输入或扫码卷号"
                  clearable
                  required
                  :rules="[{ required: true, message: '卷号不能为空' }]"
                  @blur="handleRollNoOrVatNoChange"
                >
                  <template #extra>
                    <van-icon
                      name="scan"
                      size="20"
                      class="text-primary cursor-pointer px-1"
                      @click="triggerScanner"
                    />
                  </template>
                </van-field>

                <!-- 检验等级（字典 2232: A / B / C / DEFECTIVE） -->
                <van-field name="grade" label="检验等级" required>
                  <template #input>
                    <van-radio-group
                      v-model="form.grade"
                      direction="horizontal"
                      class="flex flex-wrap gap-2"
                    >
                      <van-radio name="A">一等品(A)</van-radio>
                      <van-radio name="B">二等品(B)</van-radio>
                      <van-radio name="C">三等品(C)</van-radio>
                      <van-radio name="DEFECTIVE">等外品</van-radio>
                    </van-radio-group>
                  </template>
                </van-field>

                <!-- 实称重量 KG -->
                <van-field
                  v-model="form.measuredWeight"
                  type="number"
                  label="实称重量"
                  placeholder="请输入实称重量"
                  required
                  :rules="[
                    { required: true, message: '实称重量不能为空' },
                    { validator: (val) => Number(val) > 0, message: '重量必须大于 0' }
                  ]"
                >
                  <template #extra>
                    <span class="text-gray-400 text-12px">kg</span>
                  </template>
                </van-field>

                <!-- 实测米数 -->
                <van-field
                  v-model="form.lengthM"
                  type="number"
                  label="实测米数"
                  placeholder="请输入实测米数"
                  :rules="[
                    { validator: (val) => !val || Number(val) > 0, message: '米数必须大于 0' }
                  ]"
                >
                  <template #extra>
                    <span class="text-gray-400 text-12px">m</span>
                  </template>
                </van-field>

                <!-- 现场瑕疵照片（<=6 张） -->
                <div class="px-4 py-2.5">
                  <H5PhotoUploader
                    v-model="form.photos"
                    :max-count="6"
                    directory="workshop/inspection"
                  />
                </div>

                <!-- 备注 -->
                <van-field
                  v-model="form.remark"
                  label="录检备注"
                  placeholder="选填，现场瑕疵/布面情况说明"
                  rows="2"
                  autosize
                  type="textarea"
                  maxlength="200"
                  show-word-limit
                />
              </van-cell-group>

              <!-- 提交按钮与放弃修改按钮 -->
              <div class="flex items-center space-x-2 pt-1">
                <van-button
                  v-if="currentScannedItem"
                  v-hasPermi="['lunaerp:workshop:h5:inspection']"
                  plain
                  size="small"
                  round
                  type="default"
                  class="flex-1"
                  @click="resetFormToNew"
                >
                  取消改录
                </van-button>
                <van-button
                  v-hasPermi="['lunaerp:workshop:h5:inspection']"
                  type="primary"
                  size="small"
                  round
                  block
                  :loading="submitting"
                  loading-text="正在保存..."
                  native-type="submit"
                  class="flex-2"
                >
                  {{ currentScannedItem ? '保存改录' : '保存录检明细' }}
                </van-button>
              </div>
            </van-form>
          </div>
        </div>

        <!-- 录检明细列表区域（含 photos 照片展示） -->
        <div class="items-section">
          <div class="flex items-center justify-between px-1 mb-2">
            <div class="text-14px font-bold text-gray-900 flex items-center space-x-1.5">
              <span>录检明细</span>
              <span class="text-12px text-gray-400 font-normal">
                (已录 {{ order.items?.length || 0 }}
                {{ order.packageMode === 'PIECE' ? '匹' : '卷' }})
              </span>
            </div>
          </div>

          <!-- 明细列表 -->
          <div v-if="order.items && order.items.length > 0" class="space-y-2.5">
            <div
              v-for="(item, index) in order.items"
              :key="item.id || index"
              :class="[
                'item-card bg-white rounded-xl p-3.5 shadow-xs border transition-all',
                isItemHighlighted(item)
                  ? 'border-primary ring-2 ring-primary/20 bg-blue-50/20'
                  : 'border-gray-100'
              ]"
            >
              <!-- 头部：卷号/匹号 + 等级 -->
              <div class="flex items-center justify-between pb-2 border-b border-gray-100">
                <div class="flex items-center space-x-2">
                  <span class="text-14px font-bold text-gray-900 font-mono">
                    {{
                      item.rollNo
                        ? '卷号: ' + item.rollNo
                        : item.pieceNo
                          ? '匹号: ' + item.pieceNo
                          : '第 ' + (index + 1) + ' 项'
                    }}
                  </span>
                  <span
                    v-if="item.vatNo"
                    class="text-11px text-gray-500 font-mono bg-gray-100 px-1.5 py-0.5 rounded"
                  >
                    缸号: {{ item.vatNo }}
                  </span>
                </div>
                <van-tag :type="getGradeTagType(item.grade)" round size="medium">
                  {{ getGradeLabel(item.grade) }}
                </van-tag>
              </div>

              <!-- 明细物理参数 -->
              <div class="grid grid-cols-3 gap-2 py-2 text-12px">
                <div>
                  <span class="text-gray-400 block">实称重量</span>
                  <span class="font-medium text-gray-800 font-mono">
                    {{ item.measuredWeight != null ? item.measuredWeight + ' kg' : '—' }}
                  </span>
                </div>
                <div>
                  <span class="text-gray-400 block">米数</span>
                  <span class="font-medium text-gray-800 font-mono">
                    {{ item.lengthM != null ? item.lengthM + ' m' : '—' }}
                  </span>
                </div>
                <div>
                  <span class="text-gray-400 block">机台号</span>
                  <span class="font-medium text-gray-800 font-mono">
                    {{ item.machineNo || '—' }}
                  </span>
                </div>
              </div>

              <!-- 备注 -->
              <div v-if="item.remark" class="text-12px text-gray-500 pt-1 border-t border-gray-50">
                <span class="text-gray-400">备注: </span>{{ item.remark }}
              </div>

              <!-- 瑕疵现场照片区域（含 photos 预览） -->
              <div
                v-if="item.photos && item.photos.length > 0"
                class="photos-section mt-2 pt-2 border-t border-gray-50"
              >
                <div class="flex items-center justify-between text-11px text-gray-400 mb-1.5">
                  <span class="flex items-center">
                    <van-icon name="photograph" class="mr-1" />
                    现场照片 ({{ item.photos.length }} 张)
                  </span>
                  <span>点击可放大查看</span>
                </div>
                <div class="flex flex-wrap gap-2">
                  <van-image
                    v-for="(photoUrl, pIndex) in item.photos"
                    :key="pIndex"
                    :src="photoUrl"
                    width="64px"
                    height="64px"
                    fit="cover"
                    radius="6px"
                    class="border border-gray-200 cursor-pointer shadow-2xs"
                    @click="handlePreviewPhotos(item.photos, pIndex)"
                  />
                </div>
              </div>
            </div>
          </div>

          <!-- 空明细状态 -->
          <van-empty
            v-else
            description="暂无录检明细，待现场扫码/逐卷录入"
            class="bg-white rounded-xl shadow-xs py-8"
          />
        </div>
      </div>
    </div>

    <!-- 底部/弹窗扫码组件 -->
    <H5Scanner ref="scannerRef" @scan="handleScanResult" />
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showImagePreview, showLoadingToast, closeToast } from 'vant'
import H5Scanner from '../components/Scanner.vue'
import H5PhotoUploader from '../components/PhotoUploader.vue'
import {
  getInspection,
  ticketScan,
  saveInspectionItem,
  H5_INSPECTION_ITEM_DUPLICATE,
  WorkshopInspectionDetailVO,
  WorkshopInspectionItemVO,
  WorkshopInspectionItemSaveReqVO
} from '@/api/h5/workshop'
import { saveDraft, getDraft, clearDraft } from '@/utils/h5-draft'

defineOptions({ name: 'H5InspectionOrder' })

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const errorMessage = ref('')
const order = ref<WorkshopInspectionDetailVO | null>(null)

// 扫码定位上下文
const scannerRef = ref()
const currentScannedTicket = ref<string>('')
const currentWeavingTicketId = ref<number | null>(null)
const currentItemId = ref<number | null>(null)
const suggestedPieceNo = ref<string>('')
const currentScannedItem = ref<WorkshopInspectionItemVO | null>(null)

// 录检表单上下文
const LAST_VAT_NO_KEY = 'workshop:h5:inspection:lastVatNo'

// 模式区分：成品或卷装走 ROLL 模式；胚布且非卷装走 GREIGE PIECE 模式
const isRollMode = computed(
  () =>
    order.value?.packageMode === 'ROLL' ||
    (order.value?.type === 'FINISHED' && order.value?.packageMode !== 'PIECE')
)
const isGreigePieceMode = computed(
  () => order.value?.type === 'GREIGE' && order.value?.packageMode !== 'ROLL'
)

interface InspectionFormState {
  rollNo?: string
  pieceNo?: string
  vatNo: string
  grade: string
  measuredWeight: string | number
  lengthM: string | number
  remark: string
  photos: string[]
}

const DRAFT_NS = 'inspection:greige:item'
const submitting = ref(false)

const defaultForm = (): InspectionFormState => ({
  rollNo: '',
  pieceNo: '',
  vatNo: '',
  grade: 'A',
  measuredWeight: '',
  lengthM: '',
  remark: '',
  photos: []
})

const form = ref<InspectionFormState>(defaultForm())

// 缸号记忆存取函数
const saveLastVatNo = (vatNo: string) => {
  if (!vatNo) return
  try {
    if (order.value?.id) {
      localStorage.setItem(`${LAST_VAT_NO_KEY}_${order.value.id}`, vatNo)
    }
    localStorage.setItem(LAST_VAT_NO_KEY, vatNo)
  } catch (e) {
    console.warn('localStorage error', e)
  }
}

const getLastVatNo = (): string => {
  try {
    if (order.value?.id) {
      const perOrder = localStorage.getItem(`${LAST_VAT_NO_KEY}_${order.value.id}`)
      if (perOrder) return perOrder
    }
    const globalVat = localStorage.getItem(LAST_VAT_NO_KEY)
    if (globalVat) return globalVat
    if (order.value?.items && order.value.items.length > 0) {
      const lastItem = order.value.items[order.value.items.length - 1]
      if (lastItem.vatNo) return lastItem.vatNo
    }
  } catch (e) {
    console.warn('localStorage error', e)
  }
  return ''
}

// 草稿缓存按 单据ID_布票/卷号 分键
const draftKey = computed(() => {
  if (!order.value?.id) return ''
  if (isRollMode.value) {
    const rollKey =
      form.value.vatNo && form.value.rollNo
        ? `${form.value.vatNo}_${form.value.rollNo}`
        : 'roll_new'
    return `${order.value.id}_${rollKey}`
  }
  const ticketKey = currentWeavingTicketId.value || currentScannedTicket.value || 'new'
  return `${order.value.id}_${ticketKey}`
})

// 自动同步草稿至 localStorage（新建模式未提交时）
watch(
  () => form.value,
  (newVal) => {
    if (!submitting.value && draftKey.value && !currentScannedItem.value) {
      if (isRollMode.value) {
        if (newVal.vatNo || newVal.rollNo || newVal.measuredWeight) {
          saveDraft(DRAFT_NS, draftKey.value, newVal)
        }
      } else {
        if (currentScannedTicket.value) {
          saveDraft(DRAFT_NS, draftKey.value, newVal)
        }
      }
    }
  },
  { deep: true }
)

const buildFormState = (
  source?: Partial<InspectionFormState> & Partial<WorkshopInspectionItemVO>
): InspectionFormState => ({
  rollNo: source?.rollNo || '',
  pieceNo: source?.pieceNo || suggestedPieceNo.value || currentScannedTicket.value,
  vatNo: source?.vatNo || (isRollMode.value ? getLastVatNo() : ''),
  grade: source?.grade || 'A',
  measuredWeight: source?.measuredWeight ?? '',
  lengthM: source?.lengthM ?? '',
  remark: source?.remark || '',
  photos: source?.photos ? [...source.photos] : []
})

// 根据当前定位布票/已录行/草稿初始化表单
const initFormWithCurrentState = () => {
  if (currentScannedItem.value) {
    form.value = buildFormState(currentScannedItem.value)
  } else {
    const draft = draftKey.value ? getDraft<InspectionFormState>(DRAFT_NS, draftKey.value) : null
    form.value = buildFormState(draft || undefined)
  }
}

// 成品 ROLL 模式：查重与定位已有明细改录
const checkAndLocateRoll = () => {
  if (!isRollMode.value) return
  const curVat = (form.value.vatNo || '').trim()
  const curRoll = (form.value.rollNo || '').trim()
  if (!curRoll || !curVat) {
    if (currentItemId.value && !curRoll) {
      currentItemId.value = null
      currentScannedItem.value = null
    }
    return
  }

  const existItem = order.value?.items?.find(
    (it) => (it.vatNo || '').trim() === curVat && (it.rollNo || '').trim() === curRoll
  )

  if (existItem) {
    currentItemId.value = existItem.id
    currentScannedItem.value = existItem
    form.value.grade = existItem.grade || 'A'
    form.value.measuredWeight = existItem.measuredWeight != null ? existItem.measuredWeight : ''
    form.value.lengthM = existItem.lengthM != null ? existItem.lengthM : ''
    form.value.remark = existItem.remark || ''
    form.value.photos = existItem.photos ? [...existItem.photos] : []
    showToast({
      type: 'warning',
      message: '已定位到已有卷号记录（改录模式）'
    })
  } else {
    if (currentItemId.value != null) {
      currentItemId.value = null
      currentScannedItem.value = null
      form.value.grade = 'A'
      form.value.measuredWeight = ''
      form.value.lengthM = ''
      form.value.remark = ''
      form.value.photos = []
    }
  }
}

const handleRollNoOrVatNoChange = () => {
  if (isRollMode.value) {
    if (form.value.vatNo) {
      saveLastVatNo(form.value.vatNo.trim())
    }
    checkAndLocateRoll()
  }
}

// 保存录检行（新建/改录，支持胚布 PIECE 与成品 ROLL）
const handleSaveItem = async () => {
  if (!order.value?.id) return

  if (isRollMode.value) {
    if (!form.value.vatNo || !form.value.vatNo.trim()) {
      showToast({ type: 'fail', message: '缸号不能为空' })
      return
    }
    if (!form.value.rollNo || !form.value.rollNo.trim()) {
      showToast({ type: 'fail', message: '卷号不能为空' })
      return
    }
  } else {
    if (!currentWeavingTicketId.value) {
      showToast({ type: 'fail', message: '未关联有效织造布票' })
      return
    }
  }

  if (!form.value.grade) {
    showToast({ type: 'fail', message: '请选择检验等级' })
    return
  }
  const weightNum = Number(form.value.measuredWeight)
  if (!form.value.measuredWeight || isNaN(weightNum) || weightNum <= 0) {
    showToast({ type: 'fail', message: '实称重量必须大于 0' })
    return
  }

  submitting.value = true
  try {
    const payload: WorkshopInspectionItemSaveReqVO = {
      id: currentItemId.value || undefined,
      inspectionOrderId: order.value.id,
      weavingTicketId: isRollMode.value ? undefined : currentWeavingTicketId.value || undefined,
      pieceNo: isRollMode.value ? undefined : form.value.pieceNo || currentScannedTicket.value,
      rollNo: isRollMode.value ? form.value.rollNo.trim() : undefined,
      vatNo: form.value.vatNo ? form.value.vatNo.trim() : undefined,
      grade: form.value.grade,
      measuredWeight: weightNum,
      lengthM: form.value.lengthM ? Number(form.value.lengthM) : undefined,
      remark: form.value.remark ? form.value.remark.trim() : undefined,
      photos: form.value.photos && form.value.photos.length > 0 ? form.value.photos : []
    }

    const savedId = await saveInspectionItem(payload)
    if (draftKey.value) {
      clearDraft(DRAFT_NS, draftKey.value)
    }

    if (isRollMode.value) {
      saveLastVatNo(form.value.vatNo.trim())
    }

    showToast({
      type: 'success',
      message: currentItemId.value ? '改录成功' : '录检成功'
    })

    await loadOrderDetail(false)

    if (isRollMode.value) {
      // 成功录检/改录后，保持缸号记忆，重置卷号及物理参数，准备录下一卷
      currentItemId.value = null
      currentScannedItem.value = null
      form.value.rollNo = ''
      form.value.measuredWeight = ''
      form.value.lengthM = ''
      form.value.remark = ''
      form.value.photos = []
      form.value.grade = 'A'
    } else {
      currentItemId.value = savedId
    }
  } catch (err: any) {
    const errCode = err?.code ?? err?.data?.code
    if (errCode === H5_INSPECTION_ITEM_DUPLICATE) {
      // 查重命中：不覆盖，携带该行数据定位改录
      const duplicateItem: WorkshopInspectionItemVO | undefined =
        err?.data?.data ||
        err?.data ||
        order.value?.items?.find(
          (it) =>
            (it.vatNo || '').trim() === (form.value.vatNo || '').trim() &&
            (it.rollNo || '').trim() === (form.value.rollNo || '').trim()
        )
      if (duplicateItem) {
        currentItemId.value = duplicateItem.id
        currentScannedItem.value = duplicateItem
        form.value.vatNo = duplicateItem.vatNo || form.value.vatNo
        form.value.rollNo = duplicateItem.rollNo || form.value.rollNo
        form.value.grade = duplicateItem.grade || 'A'
        form.value.measuredWeight =
          duplicateItem.measuredWeight != null ? duplicateItem.measuredWeight : ''
        form.value.lengthM = duplicateItem.lengthM != null ? duplicateItem.lengthM : ''
        form.value.remark = duplicateItem.remark || ''
        form.value.photos = duplicateItem.photos ? [...duplicateItem.photos] : []
        showToast({
          type: 'warning',
          message: '该缸号+卷号已存在，已定位到该行改录'
        })
        return
      }
    }

    showToast({
      type: 'fail',
      message: err?.message || err?.msg || '保存录检明细失败'
    })
  } finally {
    submitting.value = false
  }
}

// 取消改录重置为新建
const resetFormToNew = () => {
  currentScannedItem.value = null
  currentItemId.value = null
  if (isRollMode.value) {
    form.value.rollNo = ''
    form.value.grade = 'A'
    form.value.measuredWeight = ''
    form.value.lengthM = ''
    form.value.remark = ''
    form.value.photos = []
  } else {
    initFormWithCurrentState()
  }
}

const triggerScanner = () => {
  scannerRef.value?.openScanner?.()
}

// 扫码统一分发
const handleScanResult = (code: string) => {
  if (isRollMode.value) {
    handleRollScan(code)
  } else {
    handleTicketScan(code)
  }
}

// 成品 ROLL 模式：扫码卷号识别与查重定位
const handleRollScan = (code: string) => {
  const cleanCode = (code || '').trim()
  if (!cleanCode) return
  form.value.rollNo = cleanCode
  checkAndLocateRoll()
  showToast({
    type: 'success',
    message: '已扫码卷号: ' + cleanCode
  })
}

// 判断明细卡片是否为当前定位命中的行
const isItemHighlighted = (item: WorkshopInspectionItemVO) => {
  if (currentItemId.value && item.id === currentItemId.value) {
    return true
  }
  if (
    !isRollMode.value &&
    currentWeavingTicketId.value &&
    item.weavingTicketId === currentWeavingTicketId.value
  ) {
    return true
  }
  if (
    isRollMode.value &&
    form.value.vatNo &&
    form.value.rollNo &&
    (item.vatNo || '').trim() === form.value.vatNo.trim() &&
    (item.rollNo || '').trim() === form.value.rollNo.trim()
  ) {
    return true
  }
  return false
}

// 内部扫码布票处理
const handleTicketScan = async (code: string) => {
  const cleanCode = (code || '').trim()
  if (!cleanCode) return

  showLoadingToast({
    message: '正在识别布票并定位...',
    forbidClick: true,
    duration: 0
  })

  try {
    const res = await ticketScan(cleanCode)
    closeToast()
    if (res) {
      currentScannedTicket.value = cleanCode
      currentWeavingTicketId.value = res.weavingTicketId || null
      currentItemId.value = res.item?.id || null
      suggestedPieceNo.value = res.suggestedPieceNo || cleanCode
      currentScannedItem.value = res.item || null
      initFormWithCurrentState()

      if (res.inspectionOrderId !== order.value?.id) {
        // 如果扫描的布票归属于另一张检验单，自动路由至对应检验单
        router.push({
          path: '/h5/inspection/order',
          query: {
            id: res.inspectionOrderId,
            ticketCode: cleanCode,
            ticketId: res.weavingTicketId,
            itemId: res.item?.id,
            suggestedPieceNo: res.suggestedPieceNo || cleanCode
          }
        })
      } else {
        await loadOrderDetail(false)
        showToast({
          type: 'success',
          message: res.item ? '已定位到已录检明细' : '已自动交接，定位待录检行'
        })
      }
    }
  } catch (err: any) {
    closeToast()
    showToast({
      type: 'fail',
      message: err?.message || '布票扫码识别失败'
    })
  }
}

// 状态映射
const getStatusLabel = (status?: string) => {
  switch (status) {
    case 'DRAFT':
      return '草稿'
    case 'ACTIVE':
      return '录检中'
    case 'COMPLETED':
      return '已完成'
    case 'CANCELLED':
      return '已作废'
    default:
      return status || '—'
  }
}

const getStatusTagType = (
  status?: string
): 'default' | 'primary' | 'success' | 'warning' | 'danger' => {
  switch (status) {
    case 'DRAFT':
      return 'warning'
    case 'ACTIVE':
      return 'primary'
    case 'COMPLETED':
      return 'success'
    case 'CANCELLED':
      return 'default'
    default:
      return 'default'
  }
}

// 等级映射
const getGradeLabel = (grade?: string) => {
  switch (grade) {
    case 'A':
      return '一等品 (A)'
    case 'B':
      return '二等品 (B)'
    case 'C':
      return '三等品 (C)'
    case 'DEFECTIVE':
      return '等外品 (次品)'
    default:
      return grade || '未评级'
  }
}

const getGradeTagType = (
  grade?: string
): 'default' | 'primary' | 'success' | 'warning' | 'danger' => {
  switch (grade) {
    case 'A':
      return 'success'
    case 'B':
      return 'primary'
    case 'C':
      return 'warning'
    case 'DEFECTIVE':
      return 'danger'
    default:
      return 'default'
  }
}

// 图片预览
const handlePreviewPhotos = (photos?: string[], startPosition = 0) => {
  if (!photos || photos.length === 0) return
  showImagePreview({
    images: photos,
    startPosition
  })
}

// 加载检验单数据
const loadOrderDetail = async (showLoading = true) => {
  const idStr = route.query.id as string
  if (!idStr || isNaN(Number(idStr))) {
    errorMessage.value = '未指定检验单编号，请从列表进入'
    return
  }

  const orderId = Number(idStr)
  if (showLoading) {
    loading.value = true
  }
  errorMessage.value = ''

  try {
    const res = await getInspection(orderId)
    if (!res) {
      errorMessage.value = '检验单不存在或无权访问'
      return
    }
    order.value = res

    if (isRollMode.value && !form.value.vatNo) {
      const remembered = getLastVatNo()
      if (remembered) {
        form.value.vatNo = remembered
      }
    }

    // 匹配路由传参带来的扫码定位状态
    const scanTicket = (route.query.ticketCode as string) || (route.query.scanTicketCode as string)
    const ticketIdStr = route.query.ticketId as string
    const itemIdStr = route.query.itemId as string
    if (scanTicket) {
      currentScannedTicket.value = scanTicket
      suggestedPieceNo.value = (route.query.suggestedPieceNo as string) || scanTicket
      currentWeavingTicketId.value = ticketIdStr ? Number(ticketIdStr) : null
      currentItemId.value = itemIdStr ? Number(itemIdStr) : null

      if (res.items && res.items.length > 0) {
        const found = res.items.find(
          (it) =>
            (currentItemId.value && it.id === currentItemId.value) ||
            (currentWeavingTicketId.value && it.weavingTicketId === currentWeavingTicketId.value)
        )
        if (found) {
          currentScannedItem.value = found
          currentItemId.value = found.id
        }
      }
      initFormWithCurrentState()
    } else if (currentItemId.value && res.items && res.items.length > 0) {
      const found = res.items.find((it) => it.id === currentItemId.value)
      if (found) {
        currentScannedItem.value = found
      }
    }
  } catch (err: any) {
    errorMessage.value = err?.message || '读取检验单详情失败'
  } finally {
    if (showLoading) {
      loading.value = false
    }
  }
}

// 监听路由 id 变化，支持由当前单跳入另一张检验单时重新加载
watch(
  () => route.query.id,
  (newId, oldId) => {
    if (newId && newId !== oldId) {
      loadOrderDetail()
    }
  }
)

// 返回列表页
const goBack = () => {
  router.push('/h5/inspection')
}

onMounted(() => {
  loadOrderDetail()
})
</script>

<style lang="scss" scoped>
.h5-inspection-order-page {
  font-family:
    -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}
</style>
