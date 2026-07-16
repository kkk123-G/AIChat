<template>
  <div class="recharge-records-container">
    <section v-loading="loading" class="list-section">
      <div class="table-wrapper hidden-xs-only">
        <el-table :data="pageRecords" class="recharge-table" style="width: 100%">
          <el-table-column label="用户" min-width="180">
            <template #default="scope">
              <div class="user-cell">
                <el-avatar :size="30" class="user-avatar">{{ scope.row.username.slice(0, 2).toUpperCase() }}</el-avatar>
                <div class="user-info">
                  <span class="username">{{ scope.row.username }}</span>
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="充值单号" min-width="178">
            <template #default="scope">
              <span class="record-no">{{ scope.row.rechargeNo }}</span>
            </template>
          </el-table-column>

          <el-table-column label="充值金额" min-width="130">
            <template #default="scope">
              <span class="amount">{{ formatAmount(scope.row.amount) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="备注" min-width="120" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.remark || '-' }}</template>
          </el-table-column>

          <el-table-column label="操作管理员" min-width="120">
            <template #default="scope">
              <span class="operator">{{ scope.row.operatorUsername }}</span>
            </template>
          </el-table-column>

          <el-table-column label="充值时间" min-width="124">
            <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
          </el-table-column>
        </el-table>
      </div>

      <div class="card-wrapper hidden-sm-and-up">
        <article v-for="record in pageRecords" :key="record.id" class="recharge-card">
          <div class="card-header">
            <div class="user-cell">
              <el-avatar :size="32" class="user-avatar">{{ record.username.slice(0, 2).toUpperCase() }}</el-avatar>
              <div class="user-info">
                <span class="username">{{ record.username }}</span>
              </div>
            </div>
            <span class="amount">{{ formatAmount(record.amount) }}</span>
          </div>

          <div class="card-body">
            <div class="info-row"><span class="label">充值单号</span><span class="value record-no">{{ record.rechargeNo
                }}</span>
            </div>
            <div class="info-row"><span class="label">备注</span><span class="value">{{ record.remark || '-' }}</span>
            </div>
            <div class="info-row"><span class="label">操作管理员</span><span class="value">{{ record.operatorUsername }}</span></div>
            <div class="info-row"><span class="label">充值时间</span><span class="value">{{ formatDateTime(record.createdAt) }}</span></div>
          </div>
        </article>
      </div>
    </section>

    <section class="pagination-section">
      <div class="pagination-container">
        <div class="pagination-summary">
          <span class="total-text hidden-xs-only">显示 {{ pageStart }} 至 {{ pageEnd }}，共 {{ total }} 条记录</span>
          <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :page-sizes="[10, 20, 50]"
            :total="total" layout="sizes" @size-change="handleSizeChange" />
        </div>
        <el-pagination v-model:current-page="currentPage" :page-size="pageSize" :total="total" layout="prev, pager, next"
          @current-change="handleCurrentChange" />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { adminRechargeRecordApi, type RechargeRecord } from '@/utils/api'

const currentPage = ref(1)
const pageSize = ref<10 | 20 | 50>(10)
const total = ref(0)
const pageRecords = ref<RechargeRecord[]>([])
const loading = ref(false)
const pageStart = computed(() => (total.value === 0 ? 0 : (currentPage.value - 1) * pageSize.value + 1))
const pageEnd = computed(() => Math.min(currentPage.value * pageSize.value, total.value))

function formatAmount(amount: number) {
  return `￥${amount.toFixed(2)}`
}

function formatDateTime(value: string) {
  return value.replace('T', ' ').slice(0, 19)
}

function handleSizeChange(size: number) {
  pageSize.value = size as 10 | 20 | 50
  currentPage.value = 1
  void loadRechargeRecords()
}

function handleCurrentChange(page: number) {
  currentPage.value = page
  void loadRechargeRecords()
}

async function loadRechargeRecords() {
  loading.value = true
  try {
    const page = await adminRechargeRecordApi.list({
      page: currentPage.value,
      size: pageSize.value,
    })
    pageRecords.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadRechargeRecords()
})
</script>

<style scoped lang="scss">
$primary-color: var(--app-primary);
$bg-color: var(--app-bg);
$text-main: var(--app-text);
$text-muted: var(--app-text-muted);
$border-color: var(--app-border-muted);

.recharge-records-container {
  display: flex;
  min-width: 0;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  gap: 20px;
}

.list-section,
.pagination-section {
  border: 1px solid $border-color;
  border-radius: 8px;
  background-color: var(--app-surface);
}

.list-section {
  display: flex;
  min-width: 0;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  overflow: hidden;
}

.pagination-section {
  padding: 14px 20px;
}

.table-wrapper {
  min-height: 0;
  flex: 1;
  overflow: auto;
}

.recharge-table {
  color: $text-main;
  font-size: 14px;

  :deep(th.el-table__cell) {
    border-bottom: 1px solid $border-color;
    background-color: var(--app-surface-subtle);
    color: var(--app-text-regular);
    font-weight: 600;
  }

  :deep(td.el-table__cell) {
    border-bottom: 1px solid $border-color;
    padding: 12px 0;
  }

  :deep(.el-table__cell .cell) {
    padding: 0 12px;
  }
}

.user-cell {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  flex: 0 0 auto;
  background-color: var(--app-primary-soft);
  color: var(--app-primary-strong);
  font-weight: 600;
}

.user-info {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.username {
  overflow: hidden;
  color: $text-main;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}


.label {
  color: $text-muted;
  font-size: 12px;
}

.record-no {
  color: var(--app-text-regular);
  font-family: Consolas, monospace;
  font-size: 13px;
}

.amount {
  color: #0f766e;
  font-weight: 600;
}

.operator {
  display: inline-block;
  border-radius: 4px;
  padding: 2px 6px;
  background-color: #ecfdf5;
  color: #047857;
  font-size: 12px;
}

.pagination-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #606266;
  font-size: 13px;
}

.pagination-summary {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 16px;
}

:deep(.el-pager li) {
  margin: 0 3px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background-color: var(--app-surface);
  font-weight: 400;

  &.is-active {
    border-color: $primary-color;
    background-color: #e6f7f6;
    color: $primary-color;
    font-weight: 700;
  }
}

.card-wrapper {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
  padding: 12px;
}

.recharge-card {
  border: 1px solid $border-color;
  border-radius: 8px;
  padding: 14px;
  background-color: var(--app-surface);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid $border-color;
  padding-bottom: 12px;
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-top: 12px;
}

.info-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.value {
  min-width: 0;
  color: $text-main;
  text-align: right;
  word-break: break-word;
}

@media (max-width: 768px) {
  .hidden-xs-only {
    display: none !important;
  }

  .recharge-records-container {
    gap: 12px;
  }

  .pagination-section {
    padding: 12px;
  }

  .pagination-container {
    justify-content: space-between;
    gap: 12px;
    overflow-x: auto;
  }

  .pagination-summary {
    gap: 12px;
  }
}

@media (min-width: 769px) {
  .hidden-sm-and-up {
    display: none !important;
  }
}
</style>
