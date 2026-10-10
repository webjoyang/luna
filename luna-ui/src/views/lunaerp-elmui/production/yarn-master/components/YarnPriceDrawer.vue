<template>
  <el-drawer
    v-model="visible"
    :title="drawerTitle"
    size="780px"
    destroy-on-close
    append-to-body
  >
    <div v-loading="loading" class="yarn-price-drawer-content">
      <!-- 顶部：最近采购单价只读提示区（取最近一条已审核采购单明细） -->
      <el-card shadow="never" class="!border-blue-200 !bg-blue-50/50 mb-4">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-3">
            <el-icon class="text-blue-500 text-xl"><InfoFilled /></el-icon>
            <div>
              <div class="text-14px font-bold text-gray-800">
                最近采购价（只读参考）：
                <span v-if="latestPurchase.hasOrder" class="text-primary text-16px font-mono">
                  ¥{{ Number(latestPurchase.price).toFixed(2) }} / KG
                </span>
                <span v-else class="text-gray-400 font-normal">暂无已审核采购记录</span>
              </div>
              <div v-if="latestPurchase.hasOrder" class="text-12px text-gray-500 mt-1 flex gap-4">
                <span>采购单号：{{ latestPurchase.orderNo || '-' }}</span>
                <span>下单日期：{{ latestPurchase.orderDate || '-' }}</span>
                <span>供应商：{{ latestPurchase.supplierName || '-' }}</span>
              </div>
            </div>
          </div>
          <el-tag type="info" size="small">人工参考 · 不自动回写</el-tag>
        </div>
      </el-card>

      <!-- 选项卡：采购参考价（缺省） / 市场参考价 -->
      <el-tabs v-model="activeTab" type="card" @tab-change="handleTabChange">
        <el-tab-pane label="采购参考价 (PURCHASE)" name="PURCHASE" />
        <el-tab-pane label="市场参考价 (MARKET)" name="MARKET" />
      </el-tabs>

      <!-- 工具栏 -->
      <div class="flex items-center justify-between mb-3 mt-2">
        <div class="text-13px text-gray-500">
          按生效日期倒序排列，成本卡优先取「不晚于当日」的最新一条有效价格
        </div>
        <div class="flex items-center gap-2">
          <el-button
            type="primary"
            plain
            size="small"
            @click="openAddDialog"
            v-hasPermi="['lunaerp:production:yarn-price:create']"
          >
            <el-icon class="mr-1"><Plus /></el-icon> 新增价格
          </el-button>
          <el-button
            type="success"
            plain
            size="small"
            @click="handleDownloadTemplate"
            v-hasPermi="['lunaerp:production:yarn-price:create']"
          >
            <el-icon class="mr-1"><Download /></el-icon> 模板
          </el-button>
          <el-button
            type="warning"
            plain
            size="small"
            @click="openImportDialog"
            v-hasPermi="['lunaerp:production:yarn-price:create']"
          >
            <el-icon class="mr-1"><Upload /></el-icon> 导入
          </el-button>
        </div>
      </div>

      <!-- 历史价格表格 -->
      <el-table
        :data="priceList"
        border
        stripe
        size="small"
        max-height="460"
        empty-text="暂无价格记录"
      >
        <el-table-column label="#" type="index" width="50" align="center" />
        <el-table-column label="生效日期" prop="effectiveDate" width="130" align="center">
          <template #default="{ row }">
            <span class="font-mono font-medium">{{ row.effectiveDate }}</span>
          </template>
        </el-table-column>
        <el-table-column label="单价(元/KG)" prop="price" width="130" align="right">
          <template #default="{ row }">
            <span class="font-mono text-primary font-bold">
              ¥{{ Number(row.price).toFixed(2) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="来源" prop="source" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.source === 'HAND'" size="small">手工</el-tag>
            <el-tag v-else-if="row.source === 'EXCEL'" type="success" size="small">Excel</el-tag>
            <el-tag v-else-if="row.source === 'PURCHASE_AGG'" type="warning" size="small">采购聚合</el-tag>
            <el-tag v-else size="small" type="info">{{ row.source || '手工' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="140" show-overflow-tooltip />
        <el-table-column label="更新时间" prop="updateTime" width="150" align="center" />
        <el-table-column label="操作" width="130" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              size="small"
              @click="openEditDialog(row)"
              v-hasPermi="['lunaerp:production:yarn-price:update']"
            >
              编辑
            </el-button>
            <el-button
              link
              type="danger"
              size="small"
              @click="handleDelete(row)"
              v-hasPermi="['lunaerp:production:yarn-price:delete']"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑价格对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      :title="editDialogTitle"
      width="450px"
      append-to-body
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="90px"
      >
        <el-form-item label="价类">
          <el-tag>{{ activeTab === 'PURCHASE' ? '采购参考价' : '市场参考价' }}</el-tag>
        </el-form-item>
        <el-form-item label="生效日期" prop="effectiveDate">
          <el-date-picker
            v-model="formData.effectiveDate"
            type="date"
            placeholder="请选择生效日期"
            value-format="YYYY-MM-DD"
            class="!w-full"
          />
        </el-form-item>
        <el-form-item label="单价" prop="price">
          <el-input-number
            v-model="formData.price"
            :precision="4"
            :step="0.1"
            :min="0.0001"
            placeholder="元/KG"
            class="!w-full"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="3"
            placeholder="选填，如：月初调价、大宗采购"
            maxlength="255"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- Excel 导入对话框 -->
    <el-dialog
      v-model="importDialogVisible"
      title="批量导入纱价"
      width="460px"
      append-to-body
      destroy-on-close
    >
      <el-upload
        ref="uploadRef"
        v-model:file-list="fileList"
        :auto-upload="false"
        :disabled="importing"
        :limit="1"
        accept=".xlsx, .xls"
        drag
      >
        <el-icon class="text-48px text-primary"><UploadFilled /></el-icon>
        <div class="el-upload__text mt-2">将 Excel 文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="text-12px text-gray-500 text-center mt-2">
            仅支持 .xlsx, .xls 格式，上限 5000 行。失败将返回错误明细 Excel。
          </div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="handleImportSubmit">
          开始导入
        </el-button>
      </template>
    </el-dialog>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox, ElNotification, type FormInstance, type FormRules, type UploadUserFile } from 'element-plus'
import { InfoFilled, Plus, Download, Upload, UploadFilled } from '@element-plus/icons-vue'
import download from '@/utils/download'
import {
  getYarnPriceListBySpec,
  getLatestPurchasePrice,
  createYarnPrice,
  updateYarnPrice,
  deleteYarnPrice,
  downloadYarnPriceTemplate,
  importYarnPrice,
  type YarnPriceVO,
  type YarnPriceSaveVO,
  type YarnLatestPurchasePriceVO
} from '@/api/lunaerp/production/yarn-price'

const visible = ref(false)
const loading = ref(false)
const activeTab = ref<'PURCHASE' | 'MARKET'>('PURCHASE')

const currentSpec = ref<{ id: number; specName: string; standardName: string } | null>(null)

const latestPurchase = ref<YarnLatestPurchasePriceVO>({
  hasOrder: false
})

const priceList = ref<YarnPriceVO[]>([])

const drawerTitle = computed(() => {
  if (!currentSpec.value) return '价格记录'
  return `价格记录 - ${currentSpec.value.specName} (${currentSpec.value.standardName || '未设标准名'})`
})

// 表单相关
const editDialogVisible = ref(false)
const editDialogTitle = ref('')
const submitting = ref(false)
const formRef = ref<FormInstance>()
const formData = reactive<YarnPriceSaveVO>({
  id: undefined,
  yarnSpecId: 0,
  priceType: 'PURCHASE',
  price: 0,
  effectiveDate: '',
  source: 'HAND',
  remark: ''
})

const formRules: FormRules = {
  effectiveDate: [{ required: true, message: '请选择生效日期', trigger: 'change' }],
  price: [{ required: true, message: '请输入大于0的单价', trigger: 'blur' }]
}

// 导入相关
const importDialogVisible = ref(false)
const importing = ref(false)
const uploadRef = ref()
const fileList = ref<UploadUserFile[]>([])

const open = (spec: { id: number; specName: string; standardName: string }) => {
  currentSpec.value = spec
  visible.value = true
  activeTab.value = 'PURCHASE'
  loadData()
}

defineExpose({ open })

const loadData = async () => {
  if (!currentSpec.value) return
  loading.value = true
  try {
    const [priceData, latestData] = await Promise.all([
      getYarnPriceListBySpec(currentSpec.value.id, activeTab.value),
      getLatestPurchasePrice(currentSpec.value.id)
    ])
    priceList.value = priceData || []
    latestPurchase.value = latestData || { hasOrder: false }
  } catch (e: any) {
    ElMessage.error(e?.msg || '加载价格记录失败')
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  loadData()
}

const openAddDialog = () => {
  if (!currentSpec.value) return
  editDialogTitle.value = '新增价格记录'
  formData.id = undefined
  formData.yarnSpecId = currentSpec.value.id
  formData.priceType = activeTab.value
  formData.price = undefined as any
  formData.effectiveDate = new Date().toISOString().substring(0, 10)
  formData.source = 'HAND'
  formData.remark = ''
  editDialogVisible.value = true
}

const openEditDialog = (row: YarnPriceVO) => {
  editDialogTitle.value = '编辑价格记录'
  formData.id = row.id
  formData.yarnSpecId = row.yarnSpecId
  formData.priceType = row.priceType
  formData.price = row.price
  formData.effectiveDate = row.effectiveDate
  formData.source = row.source
  formData.remark = row.remark
  editDialogVisible.value = true
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate()

  submitting.value = true
  try {
    if (formData.id) {
      await updateYarnPrice(formData)
      ElMessage.success('修改价格成功')
    } else {
      await createYarnPrice(formData)
      ElMessage.success('新增价格成功')
    }
    editDialogVisible.value = false
    loadData()
  } catch (e: any) {
    // 错误在全局响应拦截器已提示
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row: YarnPriceVO) => {
  try {
    await ElMessageBox.confirm(`确认删除生效日期为【${row.effectiveDate}】的价格记录吗？可在回收站恢复`, '提示', {
      type: 'warning'
    })
    await deleteYarnPrice(row.id!)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    // 取消
  }
}

const handleDownloadTemplate = async () => {
  try {
    const res = await downloadYarnPriceTemplate()
    download.excel(res, '纱价导入模板.xlsx')
  } catch (e) {
    ElMessage.error('下载模板失败')
  }
}

const openImportDialog = () => {
  fileList.value = []
  importDialogVisible.value = true
}

const handleImportSubmit = async () => {
  if (fileList.value.length === 0 || !fileList.value[0].raw) {
    ElMessage.error('请选择要导入的 Excel 文件')
    return
  }

  importing.value = true
  try {
    const rawFile = fileList.value[0].raw
    const res = await importYarnPrice(rawFile)

    const data = res.data
    if (data instanceof Blob) {
      if (data.type === 'application/json') {
        const text = await data.text()
        const json = JSON.parse(text)
        if (json.code !== 0) {
          ElNotification.error({ title: json.msg || '导入失败' })
          return
        }
        ElMessage.success(`导入成功，共写入 ${json.data} 条价格记录`)
        importDialogVisible.value = false
        loadData()
        return
      }
      // 返回了错误明细 Excel
      download.excel(data, '纱价导入错误明细.xlsx')
      ElNotification.error({
        title: '导入失败',
        message: '导入数据存在错误，已自动下载错误明细 Excel，请修改后重新导入！',
        duration: 5000
      })
      return
    }

    if (data && typeof data === 'object') {
      if (data.code === 0) {
        ElMessage.success(`导入成功，共写入 ${data.data} 条价格记录`)
        importDialogVisible.value = false
        loadData()
      } else {
        ElNotification.error({ title: data.msg || '导入失败' })
      }
    }
  } catch (e: any) {
    console.error('导入纱价失败:', e)
  } finally {
    importing.value = false
  }
}
</script>

<style scoped>
.yarn-price-drawer-content {
  padding: 0 4px;
}
</style>
