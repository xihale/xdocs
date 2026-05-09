# 前端测试文档

## 测试框架

- **Vitest**: 单元测试框架（基于Vite）
- **React Testing Library**: React组件测试工具
- **MSW (Mock Service Worker)**: API请求拦截和模拟
- **JSDOM**: 浏览器环境模拟

## 测试结构

```
frontend/src/
├── api/
│   ├── __tests__/
│   │   └── api.test.ts          # API层测试
│   ├── index.ts                 # API请求函数
│   └── types.ts                 # TypeScript类型定义
├── components/                  # 组件目录
├── hooks/                       # 自定义Hook目录
├── stores/                      # 状态管理目录
├── views/                       # 页面组件目录
└── test/
    ├── setup.ts                 # 测试环境配置
    └── mocks/
        ├── handlers.ts          # MSW请求处理器
        └── browser.ts           # MSW浏览器端配置
```

## API测试

### 测试覆盖范围

#### Auth API
- ✅ 登录
- ✅ 注册
- ✅ 登出
- ✅ 获取当前用户
- ✅ 获取WebSocket令牌
- ✅ 发送邮件验证码
- ✅ 重置密码

#### User API
- ✅ 获取用户资料
- ✅ 更新昵称
- ✅ 更新头像
- ✅ 修改密码
- ✅ 关注用户
- ✅ 取消关注
- ✅ 获取关注列表
- ✅ 获取粉丝列表
- ✅ 获取收藏列表
- ✅ 获取浏览历史
- ✅ 删除浏览历史

#### Article API
- ✅ 创建文章
- ✅ 更新文章
- ✅ 删除文章
- ✅ 获取文章详情
- ✅ 获取文章列表
- ✅ 获取公开文章列表
- ✅ 保存文章
- ✅ 点赞文章
- ✅ 取消点赞
- ✅ 添加评论
- ✅ 获取评论列表
- ✅ 删除评论
- ✅ 收藏文章
- ✅ 取消收藏
- ✅ 记录浏览

#### KnowledgeBase API
- ✅ 创建知识库
- ✅ 更新知识库
- ✅ 删除知识库
- ✅ 获取知识库详情
- ✅ 获取知识库列表
- ✅ 获取我的知识库
- ✅ 授权成员
- ✅ 获取成员列表
- ✅ 移除成员

#### Team API
- ✅ 创建团队
- ✅ 获取团队列表
- ✅ 获取团队详情
- ✅ 获取待处理邀请
- ✅ 邀请成员
- ✅ 接受邀请
- ✅ 拒绝邀请
- ✅ 退出团队
- ✅ 更新成员角色
- ✅ 获取成员列表
- ✅ 踢出成员

#### Chat API
- ✅ 获取聊天记录
- ✅ 发送消息

#### Notification API
- ✅ 获取通知列表
- ✅ 获取未读数
- ✅ 标记已读
- ✅ 全部已读
- ✅ 删除通知
- ✅ WebSocket 实时推送

#### Upload API
- ✅ 上传图片
- ✅ 上传头像

#### Search API
- ✅ 搜索文章
- ✅ 搜索知识库
- ✅ 搜索用户

## 运行测试

### 安装依赖

```bash
cd frontend
npm install
```

### 运行所有测试

```bash
npm test
```

### 监听模式运行测试

```bash
npm run test:watch
```

### 生成测试覆盖率报告

```bash
npm run test:coverage
```

## 编写测试

### 基本示例

```typescript
import { describe, it, expect } from 'vitest';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { authApi } from '../index';

const server = setupServer();

beforeAll(() => {
  server.listen();
});

afterEach(() => {
  server.resetHandlers();
});

afterAll(() => {
  server.close();
});

describe('Auth API', () => {
  it('should login successfully', async () => {
    // 模拟API响应
    server.use(
      http.post('/api/auth/login', () => {
        return HttpResponse.json({
          code: 200,
          message: 'success',
          data: mockUser,
        });
      })
    );

    // 调用API
    const result = await authApi.login('testuser', 'password123', 'captcha');
    
    // 断言结果
    expect(result).toEqual(mockUser);
  });
});
```

### 测试React组件

```typescript
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import MyComponent from './MyComponent';

describe('MyComponent', () => {
  it('should render correctly', () => {
    render(<MyComponent />);
    expect(screen.getByText('Hello')).toBeInTheDocument();
  });

  it('should handle click event', async () => {
    const handleClick = vi.fn();
    render(<MyComponent onClick={handleClick} />);
    
    fireEvent.click(screen.getByRole('button'));
    expect(handleClick).toHaveBeenCalledTimes(1);
  });
});
```

## MSW (Mock Service Worker)

### 添加新的API处理器

在 `src/test/mocks/handlers.ts` 中添加新的处理器：

```typescript
http.get('/api/endpoint', () => {
  return HttpResponse.json({
    code: 200,
    message: 'success',
    data: mockData,
  });
});
```

### 模拟错误响应

```typescript
http.get('/api/endpoint', () => {
  return HttpResponse.json({
    code: 500,
    message: '服务器错误',
    data: null,
  }, { status: 500 });
});
```

## 测试覆盖率

运行测试覆盖率报告：

```bash
npm run test:coverage
```

覆盖率报告将生成在 `coverage/` 目录下。

## 最佳实践

1. **使用MSW模拟API**: 隔离前端测试，不依赖后端
2. **测试用户行为**: 关注用户如何与应用交互，而不是实现细节
3. **使用语义化查询**: `getByRole`, `getByText` 等
4. **测试边界情况**: 空状态、错误状态、加载状态
5. **保持测试独立**: 每个测试应该能够独立运行
6. **使用描述性测试名称**: 清楚表达测试目的

## 常见问题

### 测试失败：找不到模块

确保已安装所有依赖：
```bash
npm install
```

### TypeScript类型错误

检查类型定义文件是否正确导入。

### MSW不拦截请求

确保在测试文件中正确设置了 `beforeAll` 和 `afterAll` 钩子。

## 扩展阅读

- [Vitest 文档](https://vitest.dev/)
- [React Testing Library 文档](https://testing-library.com/docs/react-testing-library/intro/)
- [MSW 文档](https://mswjs.io/)
