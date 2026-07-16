<template>
  <div class="user-manage-container">
    <section class="toolbar-section">
      <div class="top-bar">
        <div class="search-wrapper">
          <el-input v-model="searchQuery" placeholder="账号名" clearable class="search-input" @clear="handleSearch"
            @keyup.enter="handleSearch">
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
          <el-table-column label="账号" min-width="170">
            <template #default="scope">
              <div class="account-cell">
                <el-avatar :size="28" class="user-avatar">
                  {{ scope.row.account.slice(0, 2).toUpperCase() }}
                </el-avatar>
                <span class="account-text">{{ scope.row.account }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="角色" min-width="86">
            <template #default="scope">
              <span :class="['role-tag', scope.row.roleType]">
                {{ scope.row.role }}
              </span>
            </template>
          </el-table-column>

          <el-table-column label="余额" min-width="130">
            <template #default="scope">
              <div class="balance-cell">
                <span class="balance-text">￥{{ scope.row.balance }}</span>
                <button type="button" class="balance-type recharge-button"
                  @click="openRechargeDialog(scope.row)">充值</button>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="状态" min-width="86">
            <template #default="scope">
              <div class="status-cell">
                <span class="status-dot" :class="{ 'is-disabled': scope.row.status === '禁用' }"></span>
                <span>{{ scope.row.status }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="最后活跃时间" min-width="164">
            <template #default="scope">{{ scope.row.lastActive }}</template>
          </el-table-column>
          <el-table-column label="最后使用时间" min-width="164">
            <template #default="scope">{{ scope.row.lastUsed }}</template>
          </el-table-column>
          <el-table-column label="创建时间" min-width="164">
            <template #default="scope">{{ scope.row.createTime }}</template>
          </el-table-column>
          <el-table-column label="操作" min-width="124" fixed="right">
            <template #default="scope">
              <div class="opera-actions">
                <el-link v-if="canChangeUserStatus(scope.row)" :type="scope.row.status === '启用' ? 'danger' : 'primary'"
                  :underlined="false" @click="toggleUserStatus(scope.row)">
                  <el-icon>
                    <component :is="scope.row.status === '启用' ? CircleClose : CircleCheck" />
                  </el-icon>
                  {{ scope.row.status === '启用' ? '禁用' : '启用' }}
                </el-link>
                <el-dropdown trigger="click" placement="bottom-end" popper-class="more-action-dropdown"
                  @command="handleMoreAction(scope.row, $event)">
                  <el-link type="info" :underlined="false">
                    <el-icon>
                      <MoreFilled />
                    </el-icon>
                    更多
                  </el-link>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="recharge"><el-icon>
                          <Coin />
                        </el-icon>充值</el-dropdown-item>
                      <el-dropdown-item command="refund"><el-icon>
                          <RefreshLeft />
                        </el-icon>退款</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="card-wrapper hidden-sm-and-up">
        <div v-for="item in tableData" :key="item.id" class="mobile-card">
          <div class="card-header">
            <div class="account-cell">
              <el-avatar :size="28" class="user-avatar">
                {{ item.account.slice(0, 2).toUpperCase() }}
              </el-avatar>
              <span class="account-text">{{ item.account }}</span>
            </div>
            <span :class="['role-tag', item.roleType]">{{ item.role }}</span>
          </div>
          <div class="card-body">
            <div class="info-row">
              <span class="label">余额:</span>
              <span class="balance-cell">
                <span class="balance-text">￥{{ item.balance }}</span>
                <button type="button" class="balance-type recharge-button" @click="openRechargeDialog(item)">充值</button>
              </span>
            </div>
            <div class="info-row">
              <span class="label">状态:</span>
              <div class="status-cell">
                <span class="status-dot" :class="{ 'is-disabled': item.status === '禁用' }"></span>
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
            <el-button v-if="canChangeUserStatus(item)" link :type="item.status === '启用' ? 'danger' : 'primary'"
              @click="toggleUserStatus(item)">
              <el-icon>
                <component :is="item.status === '启用' ? CircleClose : CircleCheck" />
              </el-icon>
              {{ item.status === '启用' ? '禁用' : '启用' }}
            </el-button>
            <el-dropdown trigger="click" placement="bottom-end" popper-class="more-action-dropdown"
              @command="handleMoreAction(item, $event)">
              <el-button link type="info"><el-icon>
                  <MoreFilled />
                </el-icon>更多</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="recharge"><el-icon>
                      <Coin />
                    </el-icon>充值</el-dropdown-item>
                  <el-dropdown-item command="refund"><el-icon>
                      <RefreshLeft />
                    </el-icon>退款</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </div>
    </section>

    <section class="pagination-section">
      <div class="pagination-container">
        <div class="pagination-summary">
          <div class="total-text hidden-xs-only">
            显示 {{ pageStart }} 至 {{ pageEnd }}，共 {{ total }} 条记录
          </div>
          <el-pagination v-model:page-size="pageSize" :page-sizes="[10, 20, 50]" layout="sizes" :total="total"
            @size-change="handleSizeChange" />
        </div>
        <el-pagination v-model:current-page="currentPage" :page-size="pageSize" layout="prev, pager, next"
          :total="total" @current-change="handleCurrentChange" />
      </div>
    </section>
  </div>

  <el-dialog v-model="rechargeDialogVisible" title="充值" width="384px" class="recharge-custom-dialog" align-center
    @closed="resetRechargeForm">
    <div class="recharge-content">
      <div class="user-info-card">
        <el-avatar :size="42" class="user-avatar">
          {{ currentRechargeUser?.account?.slice(0, 2).toUpperCase() || 'U' }}
        </el-avatar>
        <div class="info-text">
          <div class="account">{{ currentRechargeUser?.account || '未知账户' }}</div>
          <div class="balance">当前余额: ￥{{ currentRechargeUser?.balance || '0.00' }}</div>
        </div>
      </div>

      <el-form label-position="top" @submit.prevent>
        <el-form-item label="充值金额">
          <el-input v-model="rechargeForm.amount" placeholder="0" inputmode="decimal" class="custom-input"
            @input="sanitizeRechargeAmount">
            <template #prefix><span class="currency-prefix">￥</span></template>
          </el-input>
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="rechargeForm.remark" type="textarea" :rows="3" resize="none" maxlength="200"
            class="custom-textarea" />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <span class="dialog-footer">
        <el-button class="btn-cancel" @click="rechargeDialogVisible = false">取消</el-button>
        <el-button class="btn-confirm" type="primary" :loading="rechargeSubmitting"
          @click="submitRecharge">确认</el-button>
      </span>
    </template>
  </el-dialog>

  <el-dialog v-model="refundDialogVisible" title="退款" width="384px" class="recharge-custom-dialog" align-center
    @closed="resetRefundForm">
    <div class="recharge-content">
      <div class="user-info-card">
        <el-avatar :size="42" class="user-avatar">
          {{ currentRefundUser?.account?.slice(0, 2).toUpperCase() || 'U' }}
        </el-avatar>
        <div class="info-text">
          <div class="account">{{ currentRefundUser?.account || '未知账户' }}</div>
          <div class="balance">当前余额: ￥{{ currentRefundUser?.balance || '0.00' }}</div>
        </div>
      </div>

      <el-form label-position="top" @submit.prevent>
        <el-form-item label="退款金额">
          <el-input v-model="refundForm.amount" placeholder="0" inputmode="decimal" class="custom-input"
            @input="sanitizeRefundAmount">
            <template #prefix><span class="currency-prefix">￥</span></template>
            <template #suffix><el-button text class="refund-all-button"
                @click="fillRefundAmount">全部</el-button></template>
          </el-input>
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="refundForm.remark" type="textarea" :rows="3" resize="none" maxlength="200"
            class="custom-textarea" />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <span class="dialog-footer">
        <el-button class="btn-cancel" @click="refundDialogVisible = false">取消</el-button>
        <el-button class="btn-confirm" type="primary" :loading="refundSubmitting" @click="submitRefund">确认</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, CircleClose, Coin, MoreFilled, Refresh, RefreshLeft, Search } from '@element-plus/icons-vue'
import { adminUserApi, authApi, type AdminUser } from '@/utils/api'
import { useAccountStore } from '@/stores/account'

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
  status: '启用' | '禁用'
  lastActive: string
  lastUsed: string
  createTime: string
}

const tableData = ref<UserItem[]>([])
const loading = ref(false)
const currentUserId = ref<string | null>(null)
const accountStore = useAccountStore()

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
  void loadCurrentUser()
})

async function loadCurrentUser() {
  try {
    currentUserId.value = String((await authApi.currentUser()).id)
  } catch {
    currentUserId.value = null
  }
}

function canChangeUserStatus(user: UserItem) {
  return user.id !== currentUserId.value
}

async function toggleUserStatus(user: UserItem) {
  const enabled = user.status === '禁用'
  const action = enabled ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确定要${action}账号“${user.account}”吗？`, `${action}账号`, {
      confirmButtonText: action,
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  try {
    await adminUserApi.updateStatus(user.id, { enabled })
    ElMessage.success(`账号已${action}`)
    await loadUsers()
  } catch {
    // The shared request handler displays the server error message.
  }
}

function handleMoreAction(user: UserItem, command: unknown) {
  if (command === 'recharge') {
    openRechargeDialog(user)
  } else if (command === 'refund') {
    openRefundDialog(user)
  }
}

const rechargeDialogVisible = ref(false)
const currentRechargeUser = ref<UserItem | null>(null)
const rechargeSubmitting = ref(false)

const rechargeForm = reactive({
  amount: '',
  remark: ''
})

function openRechargeDialog(user: UserItem) {
  currentRechargeUser.value = user
  resetRechargeForm()
  rechargeDialogVisible.value = true
}

function sanitizeRechargeAmount(value: string) {
  const normalized = value.replace(/[^\d.]/g, '')
  const [integerPart = '', ...decimalParts] = normalized.split('.')
  const decimalPart = decimalParts.join('').slice(0, 2)
  const integer = integerPart || (normalized.startsWith('.') ? '0' : '')
  rechargeForm.amount = decimalParts.length > 0 ? `${integer}.${decimalPart}` : integer
}

function resetRechargeForm() {
  rechargeForm.amount = ''
  rechargeForm.remark = ''
}

async function submitRecharge() {
  const user = currentRechargeUser.value
  const amount = rechargeForm.amount
  if (!user || rechargeSubmitting.value) return
  if (!/^(?:0|[1-9]\d*)(?:\.\d{1,2})?$/.test(amount) || Number(amount) <= 0) {
    ElMessage.warning('请输入大于 0 的有效充值金额')
    return
  }

  rechargeSubmitting.value = true
  try {
    const result = await adminUserApi.recharge(user.id, {
      amount,
      remark: rechargeForm.remark.trim() || undefined,
    })
    user.balance = Number(result.balance).toFixed(2)
    ElMessage.success('充值成功')
    rechargeDialogVisible.value = false
    await accountStore.refreshBalance()
    await loadUsers()
  } finally {
    rechargeSubmitting.value = false
  }
}

const refundDialogVisible = ref(false)
const currentRefundUser = ref<UserItem | null>(null)
const refundSubmitting = ref(false)
const refundForm = reactive({
  amount: '',
  remark: ''
})

function openRefundDialog(user: UserItem) {
  currentRefundUser.value = user
  resetRefundForm()
  refundDialogVisible.value = true
}

function sanitizeRefundAmount(value: string) {
  const normalized = value.replace(/[^\d.]/g, '')
  const [integerPart = '', ...decimalParts] = normalized.split('.')
  const decimalPart = decimalParts.join('').slice(0, 2)
  const integer = integerPart || (normalized.startsWith('.') ? '0' : '')
  refundForm.amount = decimalParts.length > 0 ? `${integer}.${decimalPart}` : integer
}

function fillRefundAmount() {
  refundForm.amount = currentRefundUser.value?.balance || ''
}

function resetRefundForm() {
  refundForm.amount = ''
  refundForm.remark = ''
}

async function submitRefund() {
  const user = currentRefundUser.value
  const amount = refundForm.amount
  if (!user || refundSubmitting.value) return
  if (!/^(?:0|[1-9]\d*)(?:\.\d{1,2})?$/.test(amount) || Number(amount) <= 0) {
    ElMessage.warning('请输入大于 0 的有效退款金额')
    return
  }
  if (Number(amount) > Number(user.balance)) {
    ElMessage.warning('退款金额不能超过当前余额')
    return
  }

  refundSubmitting.value = true
  try {
    const result = await adminUserApi.refund(user.id, {
      amount,
      remark: refundForm.remark.trim() || undefined,
    })
    user.balance = Number(result.balance).toFixed(2)
    ElMessage.success('退款成功')
    refundDialogVisible.value = false
    await accountStore.refreshBalance()
    await loadUsers()
  } finally {
    refundSubmitting.value = false
  }
}
</script>

<style scoped lang="scss">
$primary-color: var(--app-primary);
$bg-color: var(--app-bg);
$text-main: var(--app-text);
$text-muted: var(--app-text-muted);
$border-color: var(--app-border-muted);

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
    background-color: var(--app-surface);
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

  .top-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;

    .search-input {
      width: 240px;

      :deep(.el-input__wrapper) {
        border-radius: 20px; 
        background-color: var(--app-surface-muted);
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

  .table-wrapper {
    flex: 1;
    min-height: 0;
    overflow: auto;

    .custom-table {
      font-size: 14px;
      color: $text-main;

      :deep(th.el-table__cell) {
        background-color: var(--app-surface-subtle);
        color: var(--app-text-regular);
        font-weight: 600;
        border-bottom: 1px solid $border-color;
      }

      :deep(td.el-table__cell) {
        padding: 12px 0;
        border-bottom: 1px solid $border-color;
      }

      :deep(.el-table__cell .cell) {
        padding: 0 12px;
      }
    }
  }

  .account-cell {
    display: flex;
    align-items: center;
    gap: 10px;

    .account-text {
      font-weight: 500;
      color: var(--app-text);
    }

    .user-avatar {
      background-color: var(--app-primary-soft);
      color: var(--app-primary-strong);
      font-weight: 600;
    }
  }

  .role-tag {
    display: inline-block;
    padding: 1px 6px;
    border-radius: 999px;
    font-size: 11px;
    line-height: 1.4;

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
    line-height: 1;
  }

  .balance-cell {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    vertical-align: middle;
  }

  .balance-type {
    font-size: 11px;
    line-height: 1.2;
    padding: 1px 4px;
    border-radius: 3px;
    color: #10b981;
    background-color: #ecfdf5;
  }

  .recharge-button {
    border: 0;
    cursor: pointer;
    font-family: inherit;

    &:hover {
      background-color: #d1fae5;
    }
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
      background-color: #10b981;

      &.is-disabled {
        background-color: #ef4444;
      }
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

  .card-wrapper {
    display: flex;
    flex: 1;
    flex-direction: column;
    gap: 16px;
    min-height: 0;
    overflow-y: auto;

    .mobile-card {
      background: var(--app-surface);
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

  .pagination-container {
    display: flex;
    justify-content: space-between;
    align-items: center;
    color: #606266;
    font-size: 13px;

    .pagination-summary {
      display: flex;
      min-width: 0;
      align-items: center;
      gap: 16px;
    }

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
      width: 100%; 
    }

    .pagination-container {
      justify-content: space-between;
      gap: 12px;
      overflow-x: auto;

      .pagination-summary {
        gap: 12px;
      }
    }
  }
}

@media (min-width: 769px) {
  .hidden-sm-and-up {
    display: none !important;
  }
}
</style>


<style lang="scss">
.more-action-dropdown {
  min-width: 116px;
  padding: 4px;

  .el-dropdown-menu {
    padding: 0;
  }

  .el-dropdown-menu__item {
    display: flex;
    min-width: 108px;
    align-items: center;
    gap: 8px;
    border-radius: 4px;
    color: #334155;

    .el-icon {
      color: #0f766e;
      font-size: 16px;
    }

    &:hover,
    &:focus {
      background: #f0fdf4;
      color: #0f766e;
    }
  }
}

.recharge-custom-dialog {
  border-radius: 16px !important;
  overflow: hidden;

  .el-dialog__header {
    padding: 18px 24px 16px;
    margin-right: 0;
    border-bottom: 1px solid #f0f2f5;

    .el-dialog__title {
      font-size: 20px;
      font-weight: bold;
      color: #111827;
    }

    .el-dialog__headerbtn {
      top: 18px;
      right: 20px;
      font-size: 18px;
    }
  }

  .el-dialog__body {
    padding: 16px 24px 18px;
  }

  .user-info-card {
    display: flex;
    align-items: center;
    background-color: #f8fafc;
    padding: 12px 14px;
    border-radius: 12px;
    margin-bottom: 16px;

    .user-avatar {
      background-color: #e0f2f1;
      color: #00695c;
      font-size: 18px;
      font-weight: 600;
      margin-right: 12px;
    }

    .info-text {
      .account {
        font-size: 16px;
        color: #1a1a1a;
        font-weight: 500;
        margin-bottom: 2px;
      }

      .balance {
        font-size: 13px;
        color: #6b7280;
      }
    }
  }

  .el-form-item {
    margin-bottom: 16px;

    .el-form-item__label {
      font-size: 14px;
      color: #374151;
      padding-bottom: 6px;
    }
  }

  .custom-input,
  .custom-textarea {

    .el-input__wrapper,
    .el-textarea__inner {
      box-shadow: 0 0 0 1px #e5e7eb inset !important;
      border-radius: 12px !important;
      background-color: #ffffff;

      &:hover,
      &.is-focus,
      &:focus {
        box-shadow: 0 0 0 1px #82cca9 inset !important;
      }
    }

    .el-input__prefix {
      color: #6b7280;
      margin-right: 6px;
    }
  }

  .custom-input .el-input__wrapper {
    min-height: 42px;
    padding: 0 14px;
    align-items: center;

    .el-input__inner {
      font-size: 16px;
      line-height: 20px;
    }
  }

  .custom-input .el-input__prefix,
  .custom-input .el-input__prefix-inner {
    display: flex;
    align-items: center;
    line-height: 20px;
  }

  .custom-input .el-input__prefix {
    margin-right: -3px;
  }

  .refund-all-button {
    min-height: 24px;
    padding: 0 2px;
    color: #0f766e;
    font-size: 13px;
    font-weight: 600;

    &:hover,
    &:focus-visible {
      color: #047857;
    }
  }

  .custom-input .currency-prefix {
    position: relative;
    top: -2px;
    display: inline-flex;
    align-items: center;
    height: 20px;
    color: #6b7280;
    font-size: 16px;
    line-height: 20px;
  }

  .custom-textarea .el-textarea__inner {
    min-height: 64px !important;
    padding: 10px 14px;
    font-size: 14px;
  }

  .el-dialog__footer {
    padding: 12px 24px 16px;

    .dialog-footer {
      display: flex;
      justify-content: flex-end;
      gap: 12px;

      .el-button {
        min-width: 76px;
        height: 38px;
        border-radius: 10px;
        font-size: 14px;
        padding: 0 16px;
      }

      .btn-cancel {
        border: 1px solid #e5e7eb;
        color: #4b5563;
        background: #fff;

        &:hover {
          background-color: #f9fafb;
        }
      }

      .btn-confirm.el-button--primary {
        background-color: #0f766e;
        border-color: #0f766e;
        color: #fff;

        &:hover {
          background-color: #115e59;
          border-color: #115e59;
        }
      }
    }
  }
}

@media (max-width: 768px) {
  .recharge-custom-dialog {
    width: 92% !important;

    .el-dialog__header {
      padding: 20px;
    }

    .el-dialog__body {
      padding: 16px 20px;
    }

    .el-dialog__footer {
      padding: 16px 20px;
    }
  }
}
</style>
