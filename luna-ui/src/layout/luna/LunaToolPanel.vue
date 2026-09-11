<script lang="tsx">
import { computed, defineComponent, inject } from 'vue'
import type { VNode } from 'vue'
import router from '@/router'
import { Message } from '@/layout/components/Message'
import { UserInfo } from '@/layout/components/UserInfo'
import { Screenfull } from '@/layout/components/Screenfull'
import { SizeDropdown } from '@/layout/components/SizeDropdown'
import { LocaleDropdown } from '@/layout/components/LocaleDropdown'
import RouterSearch from '@/components/RouterSearch/index.vue'
import TenantVisit from '@/layout/components/TenantVisit/index.vue'
import FmsAccountSetSwitch from '@/views/fms/components/account-set/FmsAccountSetSwitch.vue'
import { useSetting } from '@/layout/components/Setting'
import { useAppStore } from '@/store/modules/app'
import { useDesign } from '@/hooks/web/useDesign'
import { ElButton, ElPopover } from 'element-plus'
import { Icon } from '@/components/Icon'
import { checkPermi } from '@/utils/permission'
import {
  LUNA_AUTO_COLLAPSE_KEY,
  type LunaAutoCollapseApi
} from './useLunaAutoCollapse'

const { getPrefixCls } = useDesign()

const prefixCls = getPrefixCls('luna-tool-panel')

const appStore = useAppStore()

// 全屏图标
const screenfull = computed(() => appStore.getScreenfull)

// 搜索
const search = computed(() => appStore.search)

// 尺寸图标
const size = computed(() => appStore.getSize)

// 多语言图标
const locale = computed(() => appStore.getLocale)

// 消息图标
const message = computed(() => appStore.getMessage)

// IM即时通讯图标
const im = computed(() => appStore.getIm)

// 租户切换权限
const hasTenantVisitPermission = computed(
  () => import.meta.env.VITE_APP_TENANT_ENABLE === 'true' && checkPermi(['system:tenant:visit'])
)

// 顶部聊天入口：在新标签页打开 IM 主页，避免挤掉当前后台页面
const goToChat = () => {
  const { href } = router.resolve({ name: 'ImHome' })
  window.open(href, '_blank')
}

export default defineComponent({
  name: 'LunaToolPanel',
  setup() {
    const { t } = useI18n()
    const { openSetting } = useSetting()
    const autoCollapse = inject<LunaAutoCollapseApi | null>(LUNA_AUTO_COLLAPSE_KEY, null)

    const onPopoverShow = () => {
      autoCollapse?.setMorePanelOpen(true)
    }

    const onPopoverHide = () => {
      autoCollapse?.setMorePanelOpen(false)
    }

    const renderToolRow = (
      label: string,
      control: VNode,
      options?: { onClick?: () => void; className?: string }
    ) => (
      <div
        class={[
          'luna-tool-row flex w-full cursor-pointer items-center gap-10px rounded-4px px-8px py-8px',
          'hover:bg-[var(--el-fill-color-light)]',
          options?.className
        ]}
        onClick={options?.onClick}
      >
        <div class="luna-tool-row__control flex shrink-0 items-center justify-center">{control}</div>
        <span class="text-13px text-[var(--el-text-color-primary)] leading-none">{label}</span>
      </div>
    )

    // 弹出框内竖排；纯图标项补充文字
    const renderTools = () => (
      <div class="min-w-220px flex flex-col gap-4px p-4px">
        <div class="luna-account-set w-full px-8px py-4px">
          <FmsAccountSetSwitch />
        </div>
        {hasTenantVisitPermission.value ? (
          <div class="luna-tenant-visit w-full px-8px py-4px">
            <TenantVisit />
          </div>
        ) : undefined}

        {renderToolRow(
          t('setting.projectSetting'),
          <Icon color="var(--el-text-color-primary)" size={18} icon="ep:setting" />,
          { onClick: openSetting, className: 'v-setting' }
        )}

        {screenfull.value
          ? renderToolRow(
              '全屏',
              <Screenfull class="custom-hover" color="var(--el-text-color-primary)"></Screenfull>
            )
          : undefined}

        {search.value
          ? renderToolRow(
              '搜索',
              <RouterSearch isModal={false} color="var(--el-text-color-primary)" />
            )
          : undefined}

        {size.value
          ? renderToolRow(
              '尺寸',
              <SizeDropdown class="custom-hover" color="var(--el-text-color-primary)"></SizeDropdown>
            )
          : undefined}

        {locale.value
          ? renderToolRow(
              '语言',
              <LocaleDropdown
                class="custom-hover"
                color="var(--el-text-color-primary)"
              ></LocaleDropdown>
            )
          : undefined}

        {message.value
          ? renderToolRow(
              '消息',
              <Message class="custom-hover" color="var(--el-text-color-primary)"></Message>
            )
          : undefined}

        {im.value
          ? renderToolRow(
              '即时通讯',
              <Icon color="var(--el-text-color-primary)" size={18} icon="ep:chat-dot-round" />,
              { onClick: goToChat }
            )
          : undefined}

        {/* UserInfo 本身已有头像+昵称，不再重复加文字 */}
        <div class="luna-tool-row flex w-full items-center rounded-4px px-8px py-8px hover:bg-[var(--el-fill-color-light)]">
          <UserInfo></UserInfo>
        </div>
      </div>
    )

    return () => (
      <div
        class={[
          prefixCls,
          'shrink-0 border-t border-t-[var(--left-menu-border-color,#2a3a4d)] border-t-solid p-10px'
        ]}
      >
        <ElPopover
          placement="right-end"
          width={280}
          trigger="click"
          teleported={false}
          onShow={onPopoverShow}
          onHide={onPopoverHide}
        >
          {{
            reference: () => (
              <ElButton class="w-full" text={true} bg={false}>
                <div class="flex items-center justify-center gap-6px text-[var(--left-menu-text-color)]">
                  <Icon icon="ep:setting" size={16} />
                  <span class="text-13px">{t('action.more')}</span>
                </div>
              </ElButton>
            ),
            default: renderTools
          }}
        </ElPopover>
      </div>
    )
  }
})
</script>

<style lang="scss" scoped>
.luna-account-set {
  :deep(.mr-8px) {
    margin-right: 0;
    width: 100%;
  }
}
.luna-tenant-visit {
  :deep(.el-select) {
    width: 100% !important;
  }
}
.luna-tool-row {
  :deep(.custom-hover),
  :deep(.v-setting) {
    padding: 0 !important;
  }
}
</style>
