<template>
  <Dialog
    :title="dialogTitle"
    v-model="dialogVisible"
    width="960px"
    :scroll="true"
    max-height="650px"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="110px"
      v-loading="formLoading"
    >
      <el-divider content-position="left">基本信息</el-divider>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="流程名称" prop="name">
            <el-input v-model="formData.name" placeholder="请输入流程名称，如：标准销售合同审批" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="单据类型" prop="bizType">
            <el-select
              v-model="formData.bizType"
              placeholder="请选择单据类型"
              class="w-full"
              :disabled="formType === 'update'"
            >
              <el-option
                v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_APPROVAL_BIZ_TYPE)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="流程状态" prop="status">
            <el-radio-group v-model="formData.status">
              <el-radio
                v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_APPROVAL_CONFIG_STATUS)"
                :key="dict.value"
                :value="dict.value"
              >
                {{ dict.label }}
              </el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="备注说明" prop="remark">
            <el-input
              v-model="formData.remark"
              type="textarea"
              :rows="2"
              placeholder="请输入流程备注说明（选填）"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">
        <div class="flex items-center gap-2">
          <span>审批节点链（按顺序执行）</span>
          <el-tag size="small" type="info">共 {{ nodes.length }} 个节点</el-tag>
        </div>
      </el-divider>

      <!-- 节点列表编辑器 -->
      <div class="node-list-container">
        <div
          v-for="(node, index) in nodes"
          :key="node._key || index"
          class="node-card mb-15px p-15px border border-gray-200 rounded-6px bg-gray-50/50"
        >
          <!-- 节点头部 -->
          <div class="node-header flex justify-between items-center mb-12px pb-8px border-b border-gray-200">
            <div class="flex items-center gap-2">
              <el-tag effect="dark" type="primary" round class="font-bold">
                #{{ node.sort }}
              </el-tag>
              <span class="font-semibold text-15px text-gray-800">
                {{ node.name || `节点 ${node.sort}` }}
              </span>
            </div>
            <div class="flex items-center gap-1">
              <el-button
                size="small"
                link
                type="primary"
                :disabled="index === 0"
                @click="handleMoveUp(index)"
              >
                <Icon icon="ep:top" class="mr-2px" /> 上移
              </el-button>
              <el-button
                size="small"
                link
                type="primary"
                :disabled="index === nodes.length - 1"
                @click="handleMoveDown(index)"
              >
                <Icon icon="ep:bottom" class="mr-2px" /> 下移
              </el-button>
              <el-button
                size="small"
                link
                type="danger"
                :disabled="nodes.length <= 1"
                @click="handleRemoveNode(index)"
              >
                <Icon icon="ep:delete" class="mr-2px" /> 删除
              </el-button>
            </div>
          </div>

          <!-- 节点表单项 -->
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item
                label="节点名称"
                :prop="`node_${index}_name`"
                class="!mb-12px"
                required
              >
                <el-input
                  v-model="node.name"
                  placeholder="请输入节点名称，如：部门主管审批"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item
                label="节点类型"
                class="!mb-12px"
                required
              >
                <el-radio-group v-model="node.nodeType">
                  <el-radio
                    v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_APPROVAL_NODE_TYPE)"
                    :key="dict.value"
                    :value="dict.value"
                  >
                    {{ dict.label }}
                  </el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item
                label="审批人来源"
                class="!mb-8px"
                required
              >
                <el-select
                  v-model="node.approverType"
                  placeholder="请选择来源"
                  class="w-full"
                  @change="handleApproverTypeChange(node, index)"
                >
                  <el-option
                    v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_APPROVAL_APPROVER_TYPE)"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="16">
              <el-form-item
                label="审批人员配置"
                class="!mb-8px"
                required
              >
                <!-- 来源：指定用户（多选） -->
                <div v-if="node.approverType === 'USER'" class="flex items-center gap-2 w-full">
                  <el-select
                    v-model="nodeUserIdsMap[index]"
                    multiple
                    filterable
                    placeholder="可在此多选用户"
                    class="flex-1"
                    collapse-tags
                    collapse-tags-tooltip
                  >
                    <el-option
                      v-for="user in userList"
                      :key="user.id"
                      :label="user.nickname || user.username"
                      :value="user.id"
                    >
                      <div class="flex items-center justify-between">
                        <span>{{ user.nickname || user.username }}</span>
                        <el-tag v-if="user.deptName" size="small" type="info">
                          {{ user.deptName }}
                        </el-tag>
                      </div>
                    </el-option>
                  </el-select>
                  <el-button
                    type="primary"
                    plain
                    size="small"
                    @click="openUserSelectDialog(index)"
                  >
                    <Icon icon="ep:user" class="mr-4px" /> 弹窗选择
                  </el-button>
                </div>

                <!-- 来源：指定岗位 -->
                <el-select
                  v-else-if="node.approverType === 'POST'"
                  v-model="node.approverIds"
                  placeholder="请选择指定岗位"
                  class="w-full"
                  filterable
                >
                  <el-option
                    v-for="post in postList"
                    :key="String(post.id)"
                    :label="post.name"
                    :value="String(post.id)"
                  />
                </el-select>

                <!-- 来源：指定角色 -->
                <el-select
                  v-else-if="node.approverType === 'ROLE'"
                  v-model="node.approverIds"
                  placeholder="请选择指定角色"
                  class="w-full"
                  filterable
                >
                  <el-option
                    v-for="role in roleList"
                    :key="String(role.id)"
                    :label="role.name"
                    :value="String(role.id)"
                  />
                </el-select>

                <!-- 来源：部门负责人 -->
                <div v-else-if="node.approverType === 'DEPT_LEADER'" class="text-gray-500 py-4px">
                  <el-tag type="success" size="small" class="mr-6px">自动解析</el-tag>
                  单据发起时将自动解析提交人所属部门的主管负责人
                </div>

                <!-- 来源：提交人直属上级 -->
                <div v-else-if="node.approverType === 'SUBMITTER_LEADER'" class="text-gray-500 py-4px">
                  <el-tag type="danger" size="small" class="mr-6px">自动解析</el-tag>
                  单据发起时将自动解析提交人的直属上级领导
                </div>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 添加节点按钮 -->
        <div class="text-center mt-10px">
          <el-button type="primary" plain class="w-full !border-dashed" @click="handleAddNode">
            <Icon icon="ep:plus" class="mr-5px" /> 添加审批节点
          </el-button>
        </div>
      </div>
    </el-form>

    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <!-- 用户选择弹窗（V2，支持部门树与多选搜索） -->
  <UserSelectDialogV2
    ref="userSelectDialogRef"
    :multiple="true"
    @selected="handleUsersSelected"
  />
</template>

<script setup lang="ts">
import { ref, reactive, nextTick } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'
import { useMessage } from '@/hooks/web/useMessage'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import {
  ApprovalConfigApi,
  ApprovalConfigVO,
  ApprovalConfigNodeVO
} from '@/api/approval/config'
import * as UserApi from '@/api/system/user'
import * as PostApi from '@/api/system/post'
import * as RoleApi from '@/api/system/role'
import UserSelectDialogV2 from '@/views/system/user/components/UserSelectDialogV2.vue'
import {
  createDefaultNode,
  reindexNodes,
  moveNodeUp,
  moveNodeDown,
  parseUserIds,
  serializeUserIds,
  validateNodes
} from './helper'

defineOptions({ name: 'ApprovalConfigForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref<'create' | 'update'>('create')

// 主表单数据
const formData = ref<ApprovalConfigVO>({
  id: undefined,
  bizType: '',
  name: '',
  status: 'ENABLED',
  remark: '',
  nodes: []
})

// 节点列表响应式对象（附带 _key 避免渲染闪烁）
interface EditableNode extends ApprovalConfigNodeVO {
  _key?: number
}
const nodes = ref<EditableNode[]>([])

// 每个节点的用户 ID 数组映射：nodeIndex -> number[]
const nodeUserIdsMap = reactive<Record<number, number[]>>({})

// 基础数据缓存
const userList = ref<UserApi.UserVO[]>([])
const postList = ref<PostApi.PostVO[]>([])
const roleList = ref<RoleApi.RoleVO[]>([])

// 弹窗相关
const userSelectDialogRef = ref()
const currentUserSelectIndex = ref<number>(-1)

const formRules = reactive({
  name: [{ required: true, message: '流程名称不能为空', trigger: 'blur' }],
  bizType: [{ required: true, message: '单据类型不能为空', trigger: 'change' }]
})
const formRef = ref()

let keySeed = 1
const generateKey = () => keySeed++

/** 加载字典与人员/岗位/角色候选数据 */
const loadBaseData = async () => {
  try {
    const [users, posts, roles] = await Promise.all([
      UserApi.getSimpleUserList(),
      PostApi.getSimplePostList(),
      RoleApi.getSimpleRoleList()
    ])
    userList.value = users || []
    postList.value = posts || []
    roleList.value = roles || []
  } catch (err) {
    console.error('加载系统人员/岗位/角色数据失败:', err)
  }
}

/** 打开表单弹窗 */
const open = async (type: 'create' | 'update', id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = type === 'create' ? '新建审批流程' : '编辑审批流程'
  formType.value = type
  resetForm()

  await loadBaseData()

  if (id) {
    formLoading.value = true
    try {
      const data = await ApprovalConfigApi.getApprovalConfig(id)
      formData.value = {
        id: data.id,
        bizType: data.bizType,
        name: data.name,
        status: data.status || 'ENABLED',
        remark: data.remark || ''
      }

      if (data.nodes && data.nodes.length > 0) {
        nodes.value = data.nodes.map((node) => ({
          ...node,
          _key: generateKey()
        }))
        // 初始化 nodeUserIdsMap
        nodes.value.forEach((node, idx) => {
          if (node.approverType === 'USER') {
            nodeUserIdsMap[idx] = parseUserIds(node.approverIds)
          } else {
            nodeUserIdsMap[idx] = []
          }
        })
      } else {
        initDefaultSingleNode()
      }
    } finally {
      formLoading.value = false
    }
  } else {
    initDefaultSingleNode()
  }
}

defineExpose({ open })

/** 初始化单个默认节点 */
const initDefaultSingleNode = () => {
  nodes.value = [{ ...createDefaultNode(1), _key: generateKey() }]
  nodeUserIdsMap[0] = []
}

/** 添加新节点 */
const handleAddNode = () => {
  const newSort = nodes.value.length + 1
  const newNode = {
    ...createDefaultNode(newSort),
    _key: generateKey()
  }
  nodes.value.push(newNode)
  nodeUserIdsMap[nodes.value.length - 1] = []
}

/** 删除节点 */
const handleRemoveNode = (index: number) => {
  nodes.value.splice(index, 1)
  // 重新映射 nodeUserIdsMap
  const newMap: Record<number, number[]> = {}
  nodes.value.forEach((node, idx) => {
    newMap[idx] = node.approverType === 'USER' ? (nodeUserIdsMap[idx >= index ? idx + 1 : idx] || []) : []
  })
  Object.keys(nodeUserIdsMap).forEach((k) => delete nodeUserIdsMap[Number(k)])
  Object.assign(nodeUserIdsMap, newMap)
  nodes.value = reindexNodes(nodes.value)
}

/** 上移节点 */
const handleMoveUp = (index: number) => {
  if (index <= 0) return
  // 交换 nodeUserIdsMap
  const tempUserIds = nodeUserIdsMap[index - 1]
  nodeUserIdsMap[index - 1] = nodeUserIdsMap[index]
  nodeUserIdsMap[index] = tempUserIds

  nodes.value = moveNodeUp(nodes.value, index)
}

/** 下移节点 */
const handleMoveDown = (index: number) => {
  if (index >= nodes.value.length - 1) return
  // 交换 nodeUserIdsMap
  const tempUserIds = nodeUserIdsMap[index + 1]
  nodeUserIdsMap[index + 1] = nodeUserIdsMap[index]
  nodeUserIdsMap[index] = tempUserIds

  nodes.value = moveNodeDown(nodes.value, index)
}

/** 审批人来源变更清理对应字段 */
const handleApproverTypeChange = (node: EditableNode, index: number) => {
  node.approverIds = ''
  nodeUserIdsMap[index] = []
}

/** 打开用户选择弹窗 */
const openUserSelectDialog = (index: number) => {
  currentUserSelectIndex.value = index
  const selectedIds = nodeUserIdsMap[index] || []
  userSelectDialogRef.value?.open(selectedIds)
}

/** 用户选择弹窗确认回调 */
const handleUsersSelected = (selectedUsers: UserApi.UserVO[]) => {
  if (currentUserSelectIndex.value < 0) return
  const ids = selectedUsers.map((u) => u.id)
  nodeUserIdsMap[currentUserSelectIndex.value] = ids
}

/** 提交表单 */
const emit = defineEmits(['success'])
const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate()

  // 同步 nodeUserIdsMap 回 node.approverIds
  const preparedNodes: ApprovalConfigNodeVO[] = nodes.value.map((node, idx) => {
    let approverIds = node.approverIds
    if (node.approverType === 'USER') {
      approverIds = serializeUserIds(nodeUserIdsMap[idx] || [])
    } else if (node.approverType === 'DEPT_LEADER' || node.approverType === 'SUBMITTER_LEADER') {
      approverIds = ''
    }
    return {
      sort: node.sort,
      name: node.name,
      nodeType: node.nodeType,
      approverType: node.approverType,
      approverIds
    }
  })

  // 严格校验节点完整性
  const validation = validateNodes(preparedNodes)
  if (!validation.valid) {
    message.warning(validation.message || '请完善节点配置')
    return
  }

  formLoading.value = true
  try {
    const payload: ApprovalConfigVO = {
      ...formData.value,
      nodes: preparedNodes
    }

    if (formType.value === 'create') {
      await ApprovalConfigApi.createApprovalConfig(payload)
      message.success('创建流程成功')
    } else {
      await ApprovalConfigApi.updateApprovalConfig(payload)
      message.success('修改流程成功')
    }

    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

/** 重置表单 */
const resetForm = () => {
  formData.value = {
    id: undefined,
    bizType: '',
    name: '',
    status: 'ENABLED',
    remark: '',
    nodes: []
  }
  nodes.value = []
  Object.keys(nodeUserIdsMap).forEach((k) => delete nodeUserIdsMap[Number(k)])
  formRef.value?.resetFields()
}
</script>

<style lang="scss" scoped>
.node-card {
  transition: all 0.2s ease;
  &:hover {
    border-color: var(--el-color-primary-light-5);
    background-color: var(--el-color-primary-light-9);
  }
}
</style>
