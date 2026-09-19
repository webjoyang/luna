import { Layout } from '@/utils/routerHelper'

const { t } = useI18n()
/**
 * redirect: noredirect        当设置 noredirect 的时候该路由在面包屑导航中不可被点击
 * name:'router-name'          设定路由的名字，一定要填写不然使用<keep-alive>时会出现各种问题
 * meta : {
 hidden: true              当设置 true 的时候该路由不会再侧边栏出现 如404，login等页面(默认 false)

 alwaysShow: true          当你一个路由下面的 children 声明的路由大于1个时，自动会变成嵌套的模式，
 只有一个时，会将那个子路由当做根路由显示在侧边栏，
 若你想不管路由下面的 children 声明的个数都显示你的根路由，
 你可以设置 alwaysShow: true，这样它就会忽略之前定义的规则，
 一直显示根路由(默认 false)

 title: 'title'            设置该路由在侧边栏和面包屑中展示的名字

 icon: 'svg-name'          设置该路由的图标

 noCache: true             如果设置为true，则不会被 <keep-alive> 缓存(默认 false)

 breadcrumb: false         如果设置为false，则不会在breadcrumb面包屑中显示(默认 true)

 affix: true               如果设置为true，则会一直固定在tag项中(默认 false)

 noTagsView: true          如果设置为true，则不会出现在tag中(默认 false)

 activeMenu: '/dashboard'  显示高亮的路由路径

 followAuth: '/dashboard'  跟随哪个路由进行权限过滤

 canTo: true               设置为true即使hidden为true，也依然可以进行路由跳转(默认 false)
 }
 **/
const remainingRouter: AppRouteRecordRaw[] = [
  {
    path: '/redirect',
    component: Layout,
    name: 'RedirectRoot',
    children: [
      {
        path: '/redirect/:path(.*)',
        name: 'Redirect',
        component: () => import('@/views/Redirect/Redirect.vue'),
        meta: {}
      }
    ],
    meta: {
      hidden: true,
      noTagsView: true
    }
  },
  {
    path: '/',
    component: () => import('@/views/PublicHome/Index.vue'),
    name: 'PublicHome',
    meta: {
      hidden: true,
      noTagsView: true
    }
  },
  {
    path: '/index',
    component: Layout,
    name: 'Home',
    meta: {},
    children: [
      {
        path: '',
        component: () => import('@/views/Home/Index.vue'),
        name: 'Index',
        meta: {
          title: t('router.home'),
          icon: 'ep:home-filled',
          noCache: false,
          affix: true
        }
      }
    ]
  },
  {
    path: '/user',
    component: Layout,
    name: 'UserInfo',
    meta: {
      hidden: true
    },
    children: [
      {
        path: 'profile',
        component: () => import('@/views/Profile/Index.vue'),
        name: 'Profile',
        meta: {
          canTo: true,
          hidden: true,
          noTagsView: false,
          icon: 'ep:user',
          title: t('common.profile')
        }
      },
      {
        path: 'notify-message',
        component: () => import('@/views/system/notify/my/index.vue'),
        name: 'MyNotifyMessage',
        meta: {
          canTo: true,
          hidden: true,
          noTagsView: false,
          icon: 'ep:message',
          title: '我的站内信'
        }
      }
    ]
  },
  {
    path: '/dict',
    component: Layout,
    name: 'dict',
    meta: {
      hidden: true
    },
    children: [
      {
        path: 'type/data/:dictType',
        component: () => import('@/views/system/dict/data/index.vue'),
        name: 'SystemDictData',
        meta: {
          title: '字典数据',
          noCache: true,
          hidden: true,
          canTo: true,
          icon: '',
          activeMenu: '/system/dict'
        }
      }
    ]
  },
  {
    path: '/codegen',
    component: Layout,
    name: 'CodegenEdit',
    meta: {
      hidden: true
    },
    children: [
      {
        path: 'edit',
        component: () => import('@/views/infra/codegen/EditTable.vue'),
        name: 'InfraCodegenEditTable',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: 'ep:edit',
          title: '修改生成配置',
          activeMenu: 'infra/codegen/index'
        }
      }
    ]
  },
  {
    path: '/job',
    component: Layout,
    name: 'JobL',
    meta: {
      hidden: true
    },
    children: [
      {
        path: 'job-log',
        component: () => import('@/views/infra/job/logger/index.vue'),
        name: 'InfraJobLog',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: 'ep:edit',
          title: '调度日志',
          activeMenu: 'infra/job/index'
        }
      }
    ]
  },
  {
    path: '/login',
    component: () => import('@/views/Login/Login.vue'),
    name: 'Login',
    meta: {
      hidden: true,
      title: t('router.login'),
      noTagsView: true
    }
  },
  {
    path: '/luna-login',
    component: () => import('@/views/LunaLogin/LunaLogin.vue'),
    name: 'LunaLogin',
    meta: {
      hidden: true,
      title: '员工登录',
      noTagsView: true
    }
  },
  {
    path: '/pms/kb/document/share/:token',
    component: () => import('@/views/pms/kb/document/share/index.vue'),
    name: 'PmsKnowledgeDocumentShare',
    meta: {
      hidden: true,
      title: '知识文档分享',
      noTagsView: true
    }
  },
  {
    path: '/sso',
    component: () => import('@/views/Login/Login.vue'),
    name: 'SSOLogin',
    meta: {
      hidden: true,
      title: t('router.login'),
      noTagsView: true
    }
  },
  {
    path: '/social-login',
    component: () => import('@/views/Login/SocialLogin.vue'),
    name: 'SocialLogin',
    meta: {
      hidden: true,
      title: t('router.socialLogin'),
      noTagsView: true
    }
  },
  {
    path: '/403',
    component: () => import('@/views/Error/403.vue'),
    name: 'NoAccess',
    meta: {
      hidden: true,
      title: '403',
      noTagsView: true
    }
  },
  {
    path: '/404',
    component: () => import('@/views/Error/404.vue'),
    name: 'NoFound',
    meta: {
      hidden: true,
      title: '404',
      noTagsView: true
    }
  },
  {
    path: '/500',
    component: () => import('@/views/Error/500.vue'),
    name: 'Error',
    meta: {
      hidden: true,
      title: '500',
      noTagsView: true
    }
  },
  {
    path: '/:pathMatch(.*)*',
    component: () => import('@/views/Error/404.vue'),
    name: '',
    meta: {
      title: '404',
      hidden: true,
      breadcrumb: false
    }
  },
  {
    path: '/bpm',
    component: Layout,
    name: 'bpm',
    meta: {
      hidden: true
    },
    children: [
      {
        path: 'manager/form/edit',
        component: () => import('@/views/bpm/form/editor/index.vue'),
        name: 'BpmFormEditor',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '设计流程表单',
          activeMenu: '/bpm/manager/form'
        }
      },
      {
        path: 'manager/definition',
        component: () => import('@/views/bpm/model/definition/index.vue'),
        name: 'BpmProcessDefinition',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '流程定义',
          activeMenu: '/bpm/manager/model'
        }
      },
      {
        path: 'process-instance/detail',
        component: () => import('@/views/bpm/processInstance/detail/index.vue'),
        name: 'BpmProcessInstanceDetail',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '流程详情',
          activeMenu: '/bpm/task/my'
        },
        props: (route) => ({
          id: route.query.id,
          taskId: route.query.taskId,
          activityId: route.query.activityId
        })
      },
      {
        path: 'manager/model/create',
        component: () => import('@/views/bpm/model/form/index.vue'),
        name: 'BpmModelCreate',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '创建流程',
          activeMenu: '/bpm/manager/model'
        }
      },
      {
        path: 'manager/model/:type/:id',
        component: () => import('@/views/bpm/model/form/index.vue'),
        name: 'BpmModelUpdate',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '修改流程',
          activeMenu: '/bpm/manager/model'
        }
      }
    ]
  },
  {
    path: '/pms',
    component: Layout,
    name: 'PmsCenter',
    meta: { hidden: true },
    children: [
      {
        path: 'pm/iteration/detail/:id(\\d+)',
        name: 'PmsIterationDetail',
        meta: {
          title: '迭代详情',
          noCache: true,
          hidden: true,
          activeMenu: '/pms/pm/project/list'
        },
        component: () => import('@/views/pms/pm/iteration/detail/index.vue')
      },
      {
        path: 'pm/project/detail/:id(\\d+)',
        name: 'PmsProjectDetail',
        meta: {
          title: '项目详情',
          noCache: true,
          hidden: true,
          viewKey: 'PmsProjectDetail',
          activeMenu: '/pms/pm/project/list'
        },
        component: () => import('@/views/pms/pm/project/detail/index.vue')
      },
      {
        path: 'pm/project/config/:id(\\d+)',
        name: 'PmsProjectConfig',
        meta: {
          title: '项目设置',
          noCache: true,
          hidden: true,
          activeMenu: '/pms/pm/project/list'
        },
        component: () => import('@/views/pms/pm/project/config/index.vue')
      },
      {
        path: 'kb/library/:libraryId(\\d+)',
        name: 'PmsKnowledgeLibraryDetail',
        meta: {
          title: '知识库详情',
          noCache: true,
          hidden: true,
          canTo: true,
          viewKey: 'PmsKnowledgeDocument',
          activeMenu: '/pms/kb/library'
        },
        component: () => import('@/views/pms/kb/document/index.vue')
      },
      {
        path: 'kb/library/:libraryId(\\d+)/folder/:folderId(\\d+)',
        name: 'PmsKnowledgeFolderDetail',
        meta: {
          title: '文件夹详情',
          noCache: true,
          hidden: true,
          canTo: true,
          viewKey: 'PmsKnowledgeDocument',
          activeMenu: '/pms/kb/library'
        },
        component: () => import('@/views/pms/kb/document/index.vue')
      },
      {
        path: 'kb/library/:libraryId(\\d+)/document/:documentId(\\d+)',
        name: 'PmsKnowledgeDocumentDetail',
        meta: {
          title: '文档详情',
          noCache: true,
          hidden: true,
          canTo: true,
          viewKey: 'PmsKnowledgeDocument',
          activeMenu: '/pms/kb/library'
        },
        component: () => import('@/views/pms/kb/document/index.vue')
      }
    ]
  },
  {
    path: '/baseinfo',
    component: Layout,
    name: 'lunaerpElmuiBaseinfoStatic',
    meta: { hidden: true },
    children: [
      {
        path: 'customer/workspace',
        component: () => import('@/views/lunaerp-elmui/baseinfo/customer/workspace/index.vue'),
        name: 'LunaerpElmuiBaseinfoCustomerWorkspaceCreate',
        meta: {
          title: '新建客户',
          noCache: true,
          hidden: true,
          canTo: true,
          activeMenu: '/baseinfo/customer'
        }
      },
      {
        path: 'customer/workspace/:id',
        component: () => import('@/views/lunaerp-elmui/baseinfo/customer/workspace/index.vue'),
        name: 'LunaerpElmuiBaseinfoCustomerWorkspace',
        meta: {
          title: '客户工作台',
          noCache: true,
          hidden: true,
          canTo: true,
          activeMenu: '/baseinfo/customer'
        }
      },
      {
        path: 'supplier/workspace',
        component: () => import('@/views/lunaerp-elmui/baseinfo/supplier/workspace/index.vue'),
        name: 'LunaerpElmuiBaseinfoSupplierWorkspaceCreate',
        meta: {
          title: '新建供应商',
          noCache: true,
          hidden: true,
          canTo: true,
          activeMenu: '/baseinfo/supplier'
        }
      },
      {
        path: 'supplier/workspace/:id',
        component: () => import('@/views/lunaerp-elmui/baseinfo/supplier/workspace/index.vue'),
        name: 'LunaerpElmuiBaseinfoSupplierWorkspace',
        meta: {
          title: '供应商工作台',
          noCache: true,
          hidden: true,
          canTo: true,
          activeMenu: '/baseinfo/supplier'
        }
      }
    ]
  },
  {
    path: '/business',
    component: Layout,
    name: 'lunaerpElmuiBusinessStatic',
    meta: { hidden: true },
    children: [
      {
        path: 'sales-contract/edit/:id?',
        component: () => import('@/views/lunaerp-elmui/business/sales-contract/edit/index.vue'),
        name: 'LunaerpElmuiBusinessSalesContractEdit',
        meta: {
          title: '销售合同',
          noCache: true,
          hidden: true,
          canTo: true,
          activeMenu: '/business/sales-contract'
        }
      }
    ]
  },
  {
    path: '/production',
    component: Layout,
    name: 'lunaerpElmuiProductionStatic',
    meta: { hidden: true },
    children: [
      {
        path: 'greige-order/edit/:id?',
        component: () => import('@/views/lunaerp-elmui/production/greige-order/edit/index.vue'),
        name: 'LunaerpElmuiProductionGreigeOrderEdit',
        meta: {
          title: '胚布单',
          noCache: true,
          hidden: true,
          canTo: true,
          activeMenu: '/production/greige-order'
        }
      }
    ]
  },
  {
    path: '/hrm',
    component: Layout,
    name: 'HrmCenter',
    meta: { hidden: true },
    children: [
      {
        path: 'portal/opening-guide',
        name: 'HrmPortalOpeningGuide',
        meta: {
          title: '员工端开通引导',
          noCache: true,
          hidden: true,
          activeMenu: '/hrm/portal/home'
        },
        component: () => import('@/views/hrm/portal/opening-guide/index.vue')
      },
      {
        path: 'recruit/post/detail/:id',
        name: 'HrmRecruitPostDetail',
        meta: {
          title: '招聘职位详情',
          noCache: true,
          hidden: true,
          activeMenu: '/hrm/recruit/post'
        },
        component: () => import('@/views/hrm/recruit/post/detail/index.vue')
      },
      {
        path: 'recruit/candidate/detail/:id',
        name: 'HrmRecruitCandidateDetail',
        meta: {
          title: '招聘候选人详情',
          noCache: true,
          hidden: true,
          activeMenu: '/hrm/recruit/candidate'
        },
        component: () => import('@/views/hrm/recruit/candidate/detail/index.vue')
      },
      {
        path: 'employee/detail/:id',
        name: 'HrmEmployeeDetail',
        meta: {
          title: '员工档案详情',
          noCache: true,
          hidden: true,
          activeMenu: '/hrm/employee/list'
        },
        component: () => import('@/views/hrm/employee/detail/index.vue')
      },
      {
        path: 'dept/detail/:id',
        name: 'HrmDeptDetail',
        meta: {
          title: '组织详情',
          noCache: true,
          hidden: true,
          activeMenu: '/hrm/dept'
        },
        component: () => import('@/views/hrm/dept/detail/index.vue')
      },
      {
        path: 'attendance/month/detail/:employeeId',
        name: 'HrmAttendanceMonthDetail',
        meta: {
          title: '月度考勤详情',
          noCache: true,
          hidden: true,
          activeMenu: '/hrm/attendance/month'
        },
        component: () => import('@/views/hrm/attendance/month/detail/index.vue')
      },
      {
        path: 'performance/plan/detail/:id',
        name: 'HrmPerformancePlanDetail',
        meta: {
          title: '绩效计划详情',
          noCache: true,
          hidden: true,
          activeMenu: '/hrm/performance/plan'
        },
        component: () => import('@/views/hrm/performance/plan/detail/index.vue')
      },
      {
        path: 'performance/plan/form',
        name: 'HrmPerformancePlanForm',
        meta: {
          title: 'KPI 考核配置',
          noCache: true,
          hidden: true,
          canTo: true,
          activeMenu: '/hrm/performance/plan'
        },
        component: () => import('@/views/hrm/performance/plan/form/index.vue')
      },
      {
        path: 'performance/assessment/employee/:employeeId',
        name: 'HrmPerformanceAssessmentEmployee',
        meta: {
          title: '员工绩效档案',
          noCache: true,
          hidden: true,
          activeMenu: '/hrm/performance/assessment'
        },
        component: () => import('@/views/hrm/performance/assessment/employee/index.vue')
      },
      {
        path: 'performance/assessment/detail/:id',
        name: 'HrmPerformanceAssessmentDetail',
        meta: {
          title: '员工考核详情',
          noCache: true,
          hidden: true,
          activeMenu: '/hrm/performance/assessment'
        },
        component: () => import('@/views/hrm/performance/assessment/detail/index.vue')
      },
      {
        path: 'insurance/month-record/detail/:id',
        name: 'HrmInsuranceMonthRecordDetail',
        meta: {
          title: '月度社保详情',
          noCache: true,
          hidden: true,
          activeMenu: '/hrm/insurance/month-record'
        },
        component: () => import('@/views/hrm/insurance/month-record/detail/index.vue')
      },
      {
        path: 'salary/employee-info/detail/:id',
        name: 'HrmSalaryEmployeeInfoDetail',
        meta: {
          title: '薪资档案详情',
          noCache: true,
          hidden: true,
          activeMenu: '/hrm/salary/employee-info'
        },
        component: () => import('@/views/hrm/salary/employee-info/detail/index.vue')
      },
      {
        path: 'salary/history/detail/:id',
        name: 'HrmSalaryHistoryDetail',
        meta: {
          title: '历史工资详情',
          noCache: true,
          hidden: true,
          activeMenu: '/hrm/salary/history'
        },
        component: () => import('@/views/hrm/salary/month-record/detail/index.vue')
      },
      {
        path: 'salary/slip/detail/:id',
        name: 'HrmSalarySlipSendRecordDetail',
        meta: {
          title: '工资条发放详情',
          noCache: true,
          hidden: true,
          activeMenu: '/hrm/salary/slip'
        },
        component: () => import('@/views/hrm/salary/slip/send-record/detail/index.vue')
      }
    ]
  },
]

export default remainingRouter
