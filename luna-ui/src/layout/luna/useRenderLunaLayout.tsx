import { computed } from 'vue'
import { useAppStore } from '@/store/modules/app'
import { Menu } from '@/layout/components/Menu'
import { Collapse } from '@/layout/components/Collapse'
import { TagsView } from '@/layout/components/TagsView'
import AppView from '../components/AppView.vue'
import { ElScrollbar } from 'element-plus'
import { useDesign } from '@/hooks/web/useDesign'
import LunaToolPanel from './LunaToolPanel.vue'
import LunaMenuReopen from './LunaMenuReopen.vue'
import type { LunaAutoCollapseApi } from './useLunaAutoCollapse'

const { getPrefixCls } = useDesign()

const prefixCls = getPrefixCls('layout')

const appStore = useAppStore()

const pageLoading = computed(() => appStore.getPageLoading)

// 标签页
const tagsView = computed(() => appStore.getTagsView)

// 菜单折叠：true 时侧栏宽度为 0（完全隐藏）
const collapse = computed(() => appStore.getCollapse)

// 折叠图标开关
const hamburger = computed(() => appStore.getHamburger)

// 是否是移动端
const mobile = computed(() => appStore.getMobile)

// 固定头部
const fixedHeader = computed(() => appStore.getFixedHeader)

export const useRenderLunaLayout = (autoCollapse?: LunaAutoCollapseApi | null) => {
  const renderLuna = () => {
    return (
      <>
        {/* 侧栏：无 Logo；折叠时完全隐藏（含移动端） */}
        {!collapse.value ? (
          <div
            class={[
              'absolute top-0 left-0 h-full layout-border__right flex flex-col bg-[var(--left-menu-bg-color)]',
              { '!fixed z-3000': mobile.value }
            ]}
            style="transition: all var(--transition-time-02); width: var(--left-menu-max-width);"
            onMouseenter={() => autoCollapse?.cancelCollapse()}
            onMouseleave={() => autoCollapse?.scheduleCollapse()}
          >
            {hamburger.value ? (
              <div class="flex h-40px shrink-0 items-center px-12px" title="折叠菜单">
                <Collapse class="custom-hover" color="var(--left-menu-text-color)"></Collapse>
              </div>
            ) : undefined}
            <div class="min-h-0 flex-1">
              <Menu class="h-full"></Menu>
            </div>
            <LunaToolPanel></LunaToolPanel>
          </div>
        ) : (
          <LunaMenuReopen></LunaMenuReopen>
        )}
        {/* 内容区：折叠时全宽，无顶栏 ToolHeader */}
        <div
          class={[
            `${prefixCls}-content`,
            'absolute top-0 h-[100%]',
            {
              'w-[calc(100%-var(--left-menu-max-width))] left-[var(--left-menu-max-width)]':
                !collapse.value && !mobile.value,
              'w-full left-0': collapse.value || mobile.value
            }
          ]}
          style="transition: all var(--transition-time-02);"
        >
          <ElScrollbar
            v-loading={pageLoading.value}
            class={[
              `${prefixCls}-content-scrollbar`,
              {
                '!h-[calc(100%-var(--tags-view-height))] mt-[var(--tags-view-height)]':
                  fixedHeader.value && tagsView.value
              }
            ]}
          >
            <div
              class={[
                {
                  'fixed top-0 left-0 z-10': fixedHeader.value,
                  'w-[calc(100%-var(--left-menu-max-width))] !left-[var(--left-menu-max-width)]':
                    !collapse.value && fixedHeader.value && !mobile.value,
                  '!w-full !left-0': collapse.value || mobile.value
                }
              ]}
              style="transition: all var(--transition-time-02);"
            >
              {tagsView.value ? (
                <TagsView class="layout-border__top layout-border__bottom"></TagsView>
              ) : undefined}
            </div>

            <AppView></AppView>
          </ElScrollbar>
        </div>
      </>
    )
  }

  return {
    renderLuna
  }
}
