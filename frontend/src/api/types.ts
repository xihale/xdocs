// ==================== 类型定义 ====================

export interface User {
  id: number;
  username: string;
  email: string;
  nickname: string;
  avatarUrl: string;
  role: number;
  status: number;
  createTime: string;
  updateTime: string;
}

export interface Team {
  id: number;
  name: string;
  description: string;
  ownerId: number;
  avatarUrl: string;
  createTime: string;
  updateTime: string;
}

export interface TeamVO extends Team {
  ownerName: string | null;
  ownerAvatar: string | null;
  memberCount: number;
}

export interface TeamMember {
  id: number;
  teamId: number;
  userId: number;
  role: number;
  joinStatus: number;
  inviteBy: number | null;
  joinTime: string;
}

export interface KnowledgeBase {
  id: number;
  name: string;
  description: string;
  visibility: number;
  ownerType: number;
  ownerId: number;
  creatorId: number;
  createTime: string;
  updateTime: string;
}

export interface Article {
  id: number;
  knowledgeBaseId: number;
  title: string;
  summary: string | null;
  content: string | null;
  contentFormat: string | null;
  authorId: number;
  status: number;
  coverImage: string | null;
  createTime: string;
  updateTime: string;
}

export interface ArticleVO extends Article {
  authorName: string | null;
  authorAvatar: string | null;
  kbName: string | null;
  likeCount?: number;
  liked?: boolean;
  favorited?: boolean;
  canEdit?: boolean;
  teamName?: string | null;
  teamId?: number | null;
}

export interface CommentItem {
  id: number;
  articleId: number;
  userId: number;
  username: string | null;
  nickname: string | null;
  avatarUrl: string | null;
  parentId: number | null;
  replyToId: number | null;
  replyToNickname: string | null;
  content: string;
  createTime: string;
}

export interface ChatMessage {
  id: number;
  articleId: number;
  senderId: number;
  senderName: string | null;
  senderAvatar: string | null;
  messageType: number;
  content: string;
  createTime: string;
}

export interface TeamMemberVO {
  id: number;
  teamId: number;
  userId: number;
  username: string | null;
  nickname: string | null;
  avatarUrl: string | null;
  role: number;
  roleName: string;
  joinStatus: number;
  joinTime: string;
}

export interface KbMemberVO {
  id: number;
  kbId: number;
  userId: number;
  username: string | null;
  nickname: string | null;
  avatarUrl: string | null;
  role: number;
  roleName: string;
  inviteStatus: number;
  joinTime: string;
}

export interface UploadFile {
  id: number;
  bizType: string | null;
  bizId: number | null;
  fileName: string;
  fileUrl: string;
  fileSize: number | null;
  uploaderId: number;
  createTime: string;
}

// ==================== 用户资料（含社交统计） ====================

export interface UserProfile extends User {
  followingCount: number;
  followerCount: number;
  isFollowed: boolean;
}

// ==================== 关注/粉丝列表项 ====================

export interface FollowUser {
  id: number;
  username: string;
  nickname: string | null;
  avatarUrl: string | null;
}

// ==================== 收藏文章列表项 ====================

export interface FavoriteArticle {
  id: number;
  title: string;
  updateTime: string;
}

// ==================== 浏览记录列表项 ====================

export interface HistoryArticle {
  id: number;
  title: string;
  updateTime: string;
}

// ==================== 搜索用户结果项 ====================

export interface SearchUser {
  id: number;
  username: string;
  nickname: string | null;
  avatarUrl: string | null;
}

// ==================== 团队待处理邀请 ====================

export interface PendingInvite {
  id: number;
  teamId: number;
  teamName: string;
  inviterName: string | null;
  inviteTime: string;
}

// ==================== 通知 ====================

export interface NotificationItem {
  id: number;
  type: number;
  title: string;
  content: string | null;
  link: string | null;
  isRead: number;
  createTime: string;
  senderId?: number;
  senderName?: string | null;
  senderAvatar?: string | null;
}

export const NotificationType = {
  TEAM_INVITE: 0,
  KB_INVITE: 1,
  TEAM_NEW_ARTICLE: 2,
  COMMENT: 3,
  LIKE: 4,
  MEMBER_CHANGE: 5,
  FOLLOW: 6,
  FOLLOW_ARTICLE: 7,
} as const;

// ==================== 知识库待处理邀请 ====================

export interface KbPendingInvite {
  id: number;
  kbId: number;
  kbName: string | null;
  role: number;
  inviteStatus: number;
  joinTime: string;
}

// ==================== 常量枚举 ====================

export const TeamRole = { OWNER: 0, ADMIN: 1, MEMBER: 2 } as const;
export const KbRole = { OWNER: 0, ADMIN: 1, EDITOR: 2, VIEWER: 3 } as const;
export const JoinStatus = { INVITED: 0, ACCEPTED: 1, REJECTED: 2 } as const;
export const Visibility = { PRIVATE: 0, PUBLIC: 1 } as const;
export const OwnerType = { USER: 0, TEAM: 1 } as const;
export const ArticleStatus = { DRAFT: 0, PUBLISHED: 1 } as const;