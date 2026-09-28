import request from '@/config/axios'

// 反馈记录 VO
export interface ProgressFeedbackNoteVO {
  id: number
  feedbackId: number
  content: string
  issueFlag: boolean
  resolvedAt?: string
  creator?: string
  createTime: string
}

// 反馈记录创建 VO
export interface ProgressFeedbackNoteCreateVO {
  feedbackId: number
  content: string
  issueFlag?: boolean
}

// 查询指定反馈缸的记录流水列表
export const getNoteListByFeedback = (feedbackId: number) => {
  return request.get<ProgressFeedbackNoteVO[]>({
    url: '/lunaerp/production/progress-feedback/note/list-by-feedback',
    params: { feedbackId }
  })
}

// 新建反馈记录
export const createNote = (data: ProgressFeedbackNoteCreateVO) => {
  return request.post<number>({
    url: '/lunaerp/production/progress-feedback/note/create',
    data
  })
}

// 问题闭环
export const resolveNote = (id: number) => {
  return request.post<boolean>({
    url: '/lunaerp/production/progress-feedback/note/resolve',
    params: { id }
  })
}

// 删除反馈记录（普通记录可删，已闭环问题禁删）
export const deleteNote = (id: number) => {
  return request.delete<boolean>({
    url: `/lunaerp/production/progress-feedback/note/${id}`
  })
}
