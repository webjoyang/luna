import request from '@/config/axios'

/** 回收站项 VO */
export interface RecycleItemRespVO {
  bizType: string
  bizId: number
  displayName: string
  deletedBy?: string
  deletedTime?: string | Date
}

/** 回收站分页 Request VO */
export interface RecyclePageReqVO extends PageParam {
  bizType: string
}

/** 回收站恢复 Request VO */
export interface RecycleRestoreReqVO {
  bizType: string
  bizId: number
}

/** 分页查询回收站已软删列表 */
export const getRecyclePage = (params: RecyclePageReqVO) => {
  return request.get({ url: '/lunaerp/common/recycle/page', params })
}

/** 恢复回收站项 */
export const restoreRecycleItem = (data: RecycleRestoreReqVO) => {
  return request.put({ url: '/lunaerp/common/recycle/restore', data })
}

/** 彻底删除回收站项（物理删除） */
export const purgeRecycleItem = (bizType: string, bizId: number) => {
  return request.delete({ url: '/lunaerp/common/recycle/purge', params: { bizType, bizId } })
}
