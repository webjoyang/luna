<script lang="ts" setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { formatDate } from '@/utils/formatTime'
import * as NotifyMessageApi from '@/api/system/notify/message'
import { useUserStoreWithOut } from '@/store/modules/user'
import { propTypes } from '@/utils/propTypes'
import { parseNotifyParams, getNotifyJumpRoute } from '@/utils/notifyRoute'
import MyNotifyMessageDetail from '@/views/system/notify/my/MyNotifyMessageDetail.vue'

defineOptions({ name: 'Message' })

defineProps({
  color: propTypes.string.def('')
})

const { push } = useRouter()
const userStore = useUserStoreWithOut()
const activeName = ref('notice')
const unreadCount = ref(0) // 未读消息数量
const list = ref<any[]>([]) // 消息列表
const detailRef = ref()
let unreadCountTimer: ReturnType<typeof setInterval> | undefined

// 获得消息列表
const getList = async () => {
  list.value = await NotifyMessageApi.getUnreadNotifyMessageList()
  // 强制设置 unreadCount 为 0，避免小红点因为轮询太慢，不消除
  unreadCount.value = 0
}

// 获得未读消息数
const getUnreadCount = async () => {
  NotifyMessageApi.getUnreadNotifyMessageCount().then((data) => {
    unreadCount.value = data
  })
}

// 点击单条消息：按 bizType 路由跳转，无路由约定则展示详情
const handleMessageClick = async (item: any) => {
  if (!item.readStatus && item.id) {
    try {
      await NotifyMessageApi.updateNotifyMessageRead([item.id])
      item.readStatus = true
    } catch (e) {
      console.error('标记站内信已读失败', e)
    }
  }

  const params = parseNotifyParams(item.templateParams)
  const jumpRoute = getNotifyJumpRoute(params)
  if (jumpRoute) {
    push(jumpRoute)
  } else {
    detailRef.value?.open(item)
  }
}

// 跳转我的站内信
const goMyList = () => {
  push({
    name: 'MyNotifyMessage'
  })
}

// ========== 初始化 =========
onMounted(() => {
  // 首次加载小红点
  getUnreadCount()
  // 轮询刷新小红点
  unreadCountTimer = setInterval(
    () => {
      if (userStore.getIsSetUser) {
        getUnreadCount()
      } else {
        unreadCount.value = 0
      }
    },
    1000 * 60 * 2
  )
})

onBeforeUnmount(() => {
  if (unreadCountTimer) {
    clearInterval(unreadCountTimer)
    unreadCountTimer = undefined
  }
})
</script>
<template>
  <div class="message">
    <ElPopover :width="400" placement="bottom" trigger="click">
      <template #reference>
        <ElBadge :is-dot="unreadCount > 0" class="item">
          <Icon :size="18" class="cursor-pointer" icon="ep:bell" :color="color" @click="getList" />
        </ElBadge>
      </template>
      <ElTabs v-model="activeName">
        <ElTabPane label="我的站内信" name="notice">
          <el-scrollbar class="message-list">
            <template v-for="item in list" :key="item.id">
              <div class="message-item cursor-pointer" @click="handleMessageClick(item)">
                <img alt="" class="message-icon" src="@/assets/imgs/avatar.gif" />
                <div class="message-content">
                  <span class="message-title">
                    {{ item.templateNickname }}：{{ item.templateContent }}
                  </span>
                  <span class="message-date">
                    {{ formatDate(item.createTime) }}
                  </span>
                </div>
              </div>
            </template>
          </el-scrollbar>
        </ElTabPane>
      </ElTabs>
      <!-- 更多 -->
      <div style="margin-top: 10px; text-align: right">
        <el-button type="primary" @click="goMyList">
          <Icon icon="ep:view" class="mr-1px" /> 查看全部
        </el-button>
      </div>
    </ElPopover>

    <!-- 详情弹窗：无路由约定时展示 -->
    <MyNotifyMessageDetail ref="detailRef" />
  </div>
</template>
<style lang="scss" scoped>
// 铃铛对齐修复：
// DOM 链路 div.message > ElPopover > ElBadge(inline-block) > Icon
// 默认 ElBadge 是 inline-block，在父 flex 容器里按 baseline 对齐，导致图标比其他 ElIcon 视觉偏下 1-2px
// 解法：让 .message 本身变 flex-center 容器，且穿透设置内部 .el-badge 也为 flex-center
// 这样从父到子整条链路都走几何中心对齐，不再受 inline-block baseline 影响
.message {
  display: flex;
  align-items: center;
  height: 100%;

  :deep(.el-badge) {
    display: flex;
    align-items: center;
  }
}

.message-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 260px;
  line-height: 45px;
}

.message-list {
  display: flex;
  height: 400px;
  flex-direction: column;

  .message-item {
    display: flex;
    align-items: center;
    padding: 15px 10px;
    border-bottom: 1px solid var(--el-border-color-light);
    border-radius: 4px;
    cursor: pointer;
    transition: background-color 0.2s;

    &:hover {
      background-color: var(--el-fill-color-light);
    }

    &:last-child {
      border: none;
    }

    .message-icon {
      width: 40px;
      height: 40px;
      margin: 0 20px 0 5px;
    }

    .message-content {
      display: flex;
      flex-direction: column;

      .message-title {
        margin-bottom: 5px;
      }

      .message-date {
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }
    }
  }
}
</style>
