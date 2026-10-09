<template>
  <Dialog v-model="dialogVisible" title="导入客户档案" width="460">
    <el-upload
      ref="uploadRef"
      v-model:file-list="fileList"
      :auto-upload="false"
      :disabled="formLoading"
      :limit="1"
      accept=".xlsx, .xls"
      drag
      :on-exceed="handleExceed"
    >
      <Icon icon="ep:upload" class="text-48px text-[var(--el-color-primary)]" />
      <div class="el-upload__text mt-10px">将 Excel 文件拖到此处，或<em>点击上传</em></div>
      <template #tip>
        <div class="el-upload__tip text-center mt-12px text-12px text-[var(--el-text-color-secondary)]">
          <div>仅支持 .xlsx, .xls 格式，单次最多导入 5000 行。</div>
          <div class="mt-6px">
            <el-link
              :underline="false"
              type="primary"
              style="font-size: 13px"
              @click="handleDownloadTemplate"
            >
              <Icon icon="ep:download" class="mr-4px" />下载导入模板
            </el-link>
          </div>
        </div>
      </template>
    </el-upload>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" :loading="formLoading" @click="submitForm">
        开始导入
      </el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { ref, nextTick } from 'vue'
import { ElMessage, ElNotification, type UploadUserFile } from 'element-plus'
import download from '@/utils/download'
import * as CustomerApi from '@/api/lunaerp-elmui/baseinfo/customer'

defineOptions({ name: 'CustomerImportDialog' })

const emit = defineEmits(['success'])

const dialogVisible = ref(false)
const formLoading = ref(false)
const uploadRef = ref()
const fileList = ref<UploadUserFile[]>([])

const open = () => {
  dialogVisible.value = true
  fileList.value = []
  formLoading.value = false
  nextTick(() => {
    uploadRef.value?.clearFiles()
  })
}

defineExpose({ open })

const handleExceed = () => {
  ElMessage.warning('只能上传一个文件！如需更换请先移除当前文件')
}

const handleDownloadTemplate = async () => {
  try {
    const res = await CustomerApi.downloadCustomerTemplate()
    download.excel(res, '客户导入模板.xlsx')
  } catch (e) {
    ElMessage.error('下载模板失败')
  }
}

const submitForm = async () => {
  if (fileList.value.length === 0 || !fileList.value[0].raw) {
    ElMessage.error('请先选择要上传的 Excel 文件')
    return
  }

  formLoading.value = true
  try {
    const rawFile = fileList.value[0].raw
    const res = await CustomerApi.importCustomer(rawFile)

    // postOriginal 返回 AxiosResponse，res.data 可能为 Blob 或 JSON
    const data = res.data
    if (data instanceof Blob) {
      if (data.type === 'application/json') {
        const text = await data.text()
        const json = JSON.parse(text)
        if (json.code !== 0) {
          ElNotification.error({ title: json.msg || '导入失败' })
          return
        }
        ElMessage.success(`导入成功，共写入 ${json.data} 条客户档案`)
        dialogVisible.value = false
        emit('success')
        return
      }
      // 返回了错误明细 Excel
      download.excel(data, '客户导入错误明细.xlsx')
      ElNotification.error({
        title: '导入失败',
        message: '导入数据存在错误，已自动下载错误明细 Excel，请修改后重新导入！',
        duration: 5000
      })
      return
    }

    if (data && typeof data === 'object') {
      if (data.code === 0) {
        ElMessage.success(`导入成功，共写入 ${data.data} 条客户档案`)
        dialogVisible.value = false
        emit('success')
      } else {
        ElNotification.error({ title: data.msg || '导入失败' })
      }
    }
  } catch (error: any) {
    console.error('导入客户失败:', error)
  } finally {
    formLoading.value = false
  }
}
</script>
