<template>
  <div class="contacts-tab" v-loading="loading">
    <!-- 客户未保存（create 模式）时不允许维护联系人 -->
    <el-empty
      v-if="!customerId"
      :image-size="100"
      description="请先保存客户基础档案后再维护联系人"
    />
    <template v-else>
      <div class="contacts-toolbar">
        <el-button
          type="primary"
          plain
          @click="openFormDialog()"
          v-hasPermi="['lunaerp:baseinfo:customer-contact:create']"
        >
          <Icon icon="ep:plus" class="mr-2px" />新增联系人
        </el-button>
      </div>

      <el-table :data="list" border stripe>
        <el-table-column label="姓名" prop="name" min-width="120">
          <template #default="{ row }">
            <span class="cell-name">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column label="职位" prop="position" min-width="120">
          <template #default="{ row }">{{ row.position || '—' }}</template>
        </el-table-column>
        <el-table-column label="电话" prop="phone" min-width="140">
          <template #default="{ row }">{{ row.phone || '—' }}</template>
        </el-table-column>
        <el-table-column label="邮箱" prop="email" min-width="180">
          <template #default="{ row }">{{ row.email || '—' }}</template>
        </el-table-column>
        <el-table-column label="主联系人" prop="isPrimary" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isPrimary === 1" type="success" size="small">主联系人</el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              @click="openFormDialog(row)"
              v-hasPermi="['lunaerp:baseinfo:customer-contact:update']"
            >
              <Icon icon="ep:edit" class="mr-2px" />编辑
            </el-button>
            <el-button
              link
              type="danger"
              @click="handleDelete(row.id)"
              v-hasPermi="['lunaerp:baseinfo:customer-contact:delete']"
            >
              <Icon icon="ep:delete" class="mr-2px" />删除
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无联系人" :image-size="90" />
        </template>
      </el-table>
    </template>

    <!-- 新增/编辑弹窗：6 字段 name/position/phone/email/remark/isPrimary -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="560px"
      :close-on-click-modal="false"
      append-to-body
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="90px"
        class="contacts-form"
      >
        <el-form-item label="姓名" prop="name">
          <el-input v-model="formData.name" placeholder="请输入联系人姓名" />
        </el-form-item>
        <el-form-item label="职位" prop="position">
          <el-input v-model="formData.position" placeholder="请输入职位" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="formData.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="2"
            placeholder="请输入备注"
          />
        </el-form-item>
        <el-form-item label="主联系人" prop="isPrimary">
          <el-switch
            v-model="formData.isPrimary"
            :active-value="1"
            :inactive-value="0"
            active-text="主联系人"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import type { FormInstance, FormRules } from 'element-plus'
import * as ContactApi from '@/api/lunaerp-elmui/baseinfo/customer/contact'
import type { CustomerContactVO } from '@/api/lunaerp-elmui/baseinfo/customer/contact'

defineOptions({ name: 'LunaerpElmuiCustomerContactsTab' })

const props = defineProps<{
  /** 客户 ID，create 模式（未保存）为空 */
  customerId?: number | string | null
}>()

const message = useMessage()

const loading = ref(false)
const submitting = ref(false)
const list = ref<CustomerContactVO[]>([])

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const formData = reactive<CustomerContactVO>({
  id: undefined,
  customerId: 0,
  name: '',
  position: '',
  phone: '',
  email: '',
  remark: '',
  isPrimary: 0
})

const dialogTitle = computed(() => (formData.id ? '编辑联系人' : '新增联系人'))

const rules: FormRules = {
  name: [{ required: true, message: '联系人姓名不能为空', trigger: 'blur' }]
}

/** 加载当前客户的联系人（第 1 页、每页 20） */
const getList = async () => {
  if (!props.customerId) return
  loading.value = true
  try {
    const data = await ContactApi.getCustomerContactPage({
      customerId: Number(props.customerId),
      pageNo: 1,
      pageSize: 20
    })
    list.value = data.list
  } finally {
    loading.value = false
  }
}

/** 打开弹窗：row 为空走新增，否则回填走编辑（Tab 内联完成，不跳路由） */
const openFormDialog = (row?: CustomerContactVO) => {
  formRef.value?.resetFields()
  Object.assign(formData, {
    id: row?.id,
    customerId: Number(props.customerId),
    name: row?.name || '',
    position: row?.position || '',
    phone: row?.phone || '',
    email: row?.email || '',
    remark: row?.remark || '',
    isPrimary: row?.isPrimary ?? 0
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (formData.id) {
      await ContactApi.updateCustomerContact(formData)
      message.success('保存成功')
    } else {
      await ContactApi.createCustomerContact(formData)
      message.success('新增成功')
    }
    dialogVisible.value = false
    await getList()
  } finally {
    submitting.value = false
  }
}

/** 删除联系人（ElMessageBox 二次确认） */
const handleDelete = async (id?: number) => {
  if (!id) return
  try {
    await message.delConfirm('是否确认删除该联系人？')
    await ContactApi.deleteCustomerContact(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => {
  getList()
})
</script>

<style lang="scss" scoped>
.contacts-tab {
  padding: 16px;
}

.contacts-toolbar {
  margin-bottom: 12px;
}

.cell-name {
  font-weight: 600;
  color: var(--el-text-color-primary);
}
</style>
