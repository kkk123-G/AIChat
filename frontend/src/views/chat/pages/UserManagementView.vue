<template>
  <div class="user-manage-container">
    <section class="toolbar-section">
      <div class="top-bar">
        <div class="search-wrapper">
          <el-input v-model="searchQuery" placeholder="账号名" clearable class="search-input" @clear="handleSearch" @keyup.enter="handleSearch">
            <template #prefix>
              <el-icon>
                <Search />
              </el-icon>
            </template>
          </el-input>
        </div>
        <div class="action-wrapper">
          <el-button @click="handleRefresh" class="refresh-btn">
            <el-icon>
              <Refresh />
            </el-icon>
          </el-button>
        </div>
      </div>
    </section>

    <section class="list-section">
      <div class="table-wrapper hidden-xs-only">
        <el-table v-loading="loading" :data="tableData" style="width: 100%" class="custom-table">
          <th class="hidden"></th>
          <el-table-column label="账号" min-width="180">
            <template #default="scope">
              <div class="account-cell">
                <el-avatar :size="28" :class="'avatar-' + scope.row.id">
                  {{ scope.row.account.charAt(0).toUpperCase() }}
                </el-avatar>
                <span class="account-text">{{ scope.row.account }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="角色" width="100">
            <template #default="scope">
              <span :class="['role-tag', scope.row.roleType]">
                {{ scope.row.role }}
              </span>
            </template>
          </el-table-column>

          <el-table-column label="余额" width="140">
            <template #default="scope">
              <span class="balance-text">￥{{ scope.row.balance }}</span>
              <span class="balance-type">充值</span>
            </template>
          </el-table-column>

          <el-table-column label="状态" width="100">
            <template #default="scope">
              <div class="status-cell">
                <span class="status-dot"></span>
                <span>{{ scope.row.status }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="最后活跃时间" min-width="160">
            <template #default="scope">{{ scope.row.lastActive }}</template>
          </el-table-column>
          <el-table-column label="最后使用时间" min-width="160">
            <template #default="scope">{{ scope.row.lastUsed }}</template>
          </el-table-column>
          <el-table-column label="创建时间" min-width="160">
            <template #default="scope">{{ scope.row.createTime }}</template>
          </el-table-column>
          <el-table-column label="操作" width="216" fixed="right">
            <template #default="scope">
              <div class="opera-actions">
                <el-link type="primary" :underlined="false">
                  <el-icon><EditPen /></el-icon>
                  编辑
                </el-link>
                <el-link v-if="scope.row.id !== '1'" type="danger" :underlined="false">
                  <el-icon><CircleClose /></el-icon>
                  禁用
                </el-link>
                <el-link type="info" :underlined="false">
                  <el-icon><MoreFilled /></el-icon>
                  更多
                </el-link>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="card-wrapper hidden-sm-and-up">
        <div v-for="item in tableData" :key="item.id" class="mobile-card">
          <div class="card-header">
            <div class="account-cell">
              <el-avatar :size="28" :class="'avatar-' + item.id">
                {{ item.account.charAt(0).toUpperCase() }}
              </el-avatar>
              <span class="account-text">{{ item.account }}</span>
            </div>
            <span :class="['role-tag', item.roleType]">{{ item.role }}</span>
          </div>
          <div class="card-body">
            <div class="info-row">
              <span class="label">余额:</span>
              <span>
                <span class="balance-text">￥{{ item.balance }}</span>
                <span class="balance-type">充值</span>
              </span>
            </div>
            <div class="info-row">
              <span class="label">状态:</span>
              <div class="status-cell">
                <span class="status-dot"></span>
                <span>{{ item.status }}</span>
              </div>
            </div>
            <div class="info-row">
              <span class="label">最后活跃:</span><span>{{ item.lastActive || '-' }}</span>
            </div>
            <div class="info-row">
              <span class="label">最后使用:</span><span>{{ item.lastUsed || '-' }}</span>
            </div>
            <div class="info-row">
              <span class="label">创建时间:</span><span>{{ item.createTime }}</span>
            </div>
          </div>
          <div class="card-footer">
            <el-button link type="primary"><el-icon><EditPen /></el-icon>编辑</el-button>
            <el-button v-if="item.id !== '1'" link type="danger"><el-icon><CircleClose /></el-icon>禁用</el-button>
            <el-button link type="info"><el-icon><MoreFilled /></el-icon>更多</el-button>
          </div>
        </div>
      </div>
    </section>

    <section class="pagination-section">
      <div class="pagination-container">
        <div class="total-text hidden-xs-only">
          显示 {{ pageStart }} 至 {{ pageEnd }} 共 {{ total }} 条结果 每页: {{ pageSize }}
        </div>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          layout="prev, pager, next, sizes"
          :total="total"
          @current-change="handleCurrentChange"
          @size-change="handleSizeChange"
        />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { CircleClose, EditPen, MoreFilled, Refresh, Search } from '@element-plus/icons-vue'
import { adminUserApi, type AdminUser } from '@/utils/api'

const searchQuery = ref('')
const currentPage = ref(1)
const pageSize = ref<10 | 20 | 50>(10)
const total = ref(0)
interface UserItem {
  id: string
  account: string
  role: string
  roleType: 'user' | 'admin'
  balance: string
  status: string
  lastActive: string
  lastUsed: string
  createTime: string
}

const tableData = ref<UserItem[]>([])
const loading = ref(false)

const pageStart = computed(() => (total.value === 0 ? 0 : (currentPage.value - 1) * pageSize.value + 1))
const pageEnd = computed(() => Math.min(currentPage.value * pageSize.value, total.value))

async function loadUsers() {
  loading.value = true
  try {
    const page = await adminUserApi.list({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchQuery.value.trim() || undefined,
    })
    tableData.value = page.records.map(toUserItem)
    total.value = page.total
  } finally {
    loading.value = false
  }
}

function handleRefresh() {
  void loadUsers()
}

function handleSearch() {
  currentPage.value = 1
  void loadUsers()
}

function handleCurrentChange(page: number) {
  currentPage.value = page
  void loadUsers()
}

function handleSizeChange(size: number) {
  pageSize.value = size as 10 | 20 | 50
  currentPage.value = 1
  void loadUsers()
}

function toUserItem(user: AdminUser): UserItem {
  return {
    id: user.id,
    account: user.username,
    role: user.role === 'ADMIN' ? '管理员' : '用户',
    roleType: user.role === 'ADMIN' ? 'admin' : 'user',
    balance: Number(user.balance).toFixed(2),
    status: user.status === 'ENABLED' ? '启用' : '禁用',
    lastActive: formatDateTime(user.lastActiveAt),
    lastUsed: formatDateTime(user.lastUsedAt),
    createTime: formatDateTime(user.createdAt),
  }
}

function formatDateTime(value: string | null) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

onMounted(() => {
  void loadUsers()
})
</script>

<style scoped lang="scss">
// 基础变量与高保真主色调
$primary-color: #00b5ad;
$bg-color: #f8fafc;
$text-main: #333333;
$text-muted: #909399;
$border-color: #f0f2f5;

.user-manage-container {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 20px;
  min-height: 0;
  min-width: 0;

  .toolbar-section,
  .list-section,
  .pagination-section {
    background-color: #ffffff;
    border: 1px solid $border-color;
    border-radius: 8px;
  }

  .toolbar-section {
    padding: 16px 20px;
  }

  .list-section {
    display: flex;
    flex: 1;
    flex-direction: column;
    min-height: 0;
    min-width: 0;
    overflow: hidden;
  }

  .pagination-section {
    padding: 14px 20px;
  }

  // 顶部操作栏高保真还原
  .top-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;

    .search-input {
      width: 240px;

      :deep(.el-input__wrapper) {
        border-radius: 20px; // 圆角搜索框
        background-color: #f5f7fa;
        border: none;
        box-shadow: none;

        &.is-focus {
          box-shadow: 0 0 0 1px $primary-color inset;
        }
      }
    }

    .refresh-btn {
      border-radius: 50%;
      width: 36px;
      height: 36px;
      padding: 0;
      border: 1px solid #e4e7ed;
      color: $text-muted;

      &:hover {
        color: $primary-color;
        border-color: $primary-color;
        background-color: rgba(0, 181, 173, 0.05);
      }
    }
  }

  // 网页端表格样式定制
  .table-wrapper {
    flex: 1;
    min-height: 0;
    overflow: auto;

    .custom-table {
      font-size: 14px;
      color: $text-main;

      :deep(th.el-table__cell) {
        background-color: #fafbfd;
        color: #606266;
        font-weight: 600;
        border-bottom: 1px solid $border-color;
      }

      :deep(td.el-table__cell) {
        padding: 12px 0;
        border-bottom: 1px solid $border-color;
      }

      :deep(.el-table__cell .cell) {
        padding: 0 14px;
      }
    }
  }

  // 账号列头像与布局
  .account-cell {
    display: flex;
    align-items: center;
    gap: 10px;

    .account-text {
      font-weight: 500;
      color: #303133;
    }

    // 依照原图生成不同颜色的圆形弱化头像背景
    .avatar-3 {
      background-color: #e1f5fe;
      color: #0288d1;
    }

    .avatar-2 {
      background-color: #e8f5e9;
      color: #388e3c;
    }

    .avatar-1 {
      background-color: #e0f2f1;
      color: #004d40;
    }
  }

  // 角色和状态标签还原
  .role-tag {
    display: inline-block;
    padding: 2px 8px;
    border-radius: 999px;
    font-size: 12px;

    &.user {
      background-color: #f4f4f5;
      color: #909399;
    }

    &.admin {
      background-color: #f3e8ff;
      color: #a855f7;
    }
  }

  .balance-text {
    font-weight: 600;
    margin-right: 4px;
  }

  .balance-type {
    font-size: 11px;
    padding: 1px 4px;
    border-radius: 3px;
    color: #10b981;
    background-color: #ecfdf5;
  }

  .status-cell {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;

    .status-dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background-color: #10b981; // 绿色的启用状态点

    }
  }

  .opera-actions {
    display: flex;
    gap: 16px;
    font-size: 13px;

    .el-link {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      font-size: 13px;
    }
  }

  // H5 响应式卡片流布局 (仅在移动端尺寸下显示)
  .card-wrapper {
    display: flex;
    flex: 1;
    flex-direction: column;
    gap: 16px;
    min-height: 0;
    overflow-y: auto;

    .mobile-card {
      background: #ffffff;
      border: 1px solid $border-color;
      border-radius: 8px;
      padding: 14px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);

      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        border-bottom: 1px solid $border-color;
        padding-bottom: 10px;
        margin-bottom: 10px;
      }

      .card-body {
        display: flex;
        flex-direction: column;
        gap: 8px;
        font-size: 13px;

        .info-row {
          display: flex;
          justify-content: space-between;

          .label {
            color: $text-muted;
          }
        }
      }

      .card-footer {
        display: flex;
        justify-content: flex-end;
        gap: 16px;
        border-top: 1px solid $border-color;
        margin-top: 10px;
        padding-top: 8px;

        :deep(.el-button) {
          display: inline-flex;
          align-items: center;
          gap: 4px;
        }
      }
    }
  }

  // 底部底栏与分页区
  .pagination-container {
    display: flex;
    justify-content: space-between;
    align-items: center;
    color: #606266;
    font-size: 13px;

    :deep(.el-pager li) {
      border: 1px solid #dcdfe6;
      background-color: #fff;
      border-radius: 4px;
      margin: 0 3px;
      font-weight: normal;

      &.is-active {
        background-color: #e6f7f6;
        border-color: $primary-color;
        color: $primary-color;
        font-weight: bold;
      }
    }
  }
}

// 针对移动端极端小屏幕的显示切换 (使用 Element 官方自带响应式断点类名)
@media (max-width: 768px) {
  .hidden-xs-only {
    display: none !important;
  }

  .user-manage-container {
    gap: 12px;

    .toolbar-section {
      padding: 12px;
    }

    .pagination-section {
      padding: 12px;
    }

    .top-bar .search-input {
      width: 100%; // 移动端搜索框撑满
    }

    .pagination-container {
      justify-content: center; // 移动端分页居中
    }
  }
}

@media (min-width: 769px) {
  .hidden-sm-and-up {
    display: none !important;
  }
}
</style>
