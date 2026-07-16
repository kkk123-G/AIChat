<template>
  <div class="balance-changes-container">
    <section v-loading="loading" class="list-section">
      <div class="table-wrapper hidden-xs-only">
        <el-table :data="pageRecords" class="balance-change-table" style="width: 100%">
          <el-table-column label="变动金额" min-width="150">
            <template #default="scope">
              <span class="amount" :class="amountClass(scope.row.amount)">{{ formatAmount(scope.row.amount) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="变动状态" min-width="130">
            <template #default="scope">
              <span class="change-status" :class="amountClass(scope.row.amount)">{{ changeStatus(scope.row.amount) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="时间" min-width="180">
            <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
          </el-table-column>

          <el-table-column label="类型" min-width="150">
            <template #default="scope">
              <el-tag :class="typeClass(scope.row.type)" effect="light" size="small">{{ scope.row.type }}</el-tag>
            </template>
          </el-table-column>

          <el-table-column label="备注" min-width="104" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.remark || '-' }}</template>
          </el-table-column>
        </el-table>
      </div>

      <div class="card-wrapper hidden-sm-and-up">
        <article v-for="record in pageRecords" :key="record.id" class="balance-change-card">
          <div class="card-header">
            <el-tag :class="typeClass(record.type)" effect="light" size="small">{{ record.type }}</el-tag>
            <span class="amount" :class="amountClass(record.amount)">{{ formatAmount(record.amount) }}</span>
          </div>

          <div class="card-body">
            <div class="info-row"><span class="label">变动状态</span><span class="change-status" :class="amountClass(record.amount)">{{ changeStatus(record.amount) }}</span></div>
            <div class="info-row"><span class="label">时间</span><span class="value">{{ formatDateTime(record.createdAt) }}</span></div>
            <div class="info-row"><span class="label">备注</span><span class="value">{{ record.remark || '-' }}</span></div>
          </div>
        </article>
      </div>
    </section>

    <section class="pagination-section">
      <div class="pagination-container">
        <div class="pagination-summary">
          <span class="total-text hidden-xs-only">显示 {{ pageStart }} 至 {{ pageEnd }}，共 {{ total }} 条记录</span>
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50]"
            :total="total"
            layout="sizes"
            @size-change="handleSizeChange"
          />
        </div>
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="handleCurrentChange"
        />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { userAccountApi, type BalanceChangeRecord } from '@/utils/api'

const currentPage = ref(1)
const pageSize = ref<10 | 20 | 50>(10)
const total = ref(0)
const pageRecords = ref<BalanceChangeRecord[]>([])
const loading = ref(false)
const pageStart = computed(() => (total.value === 0 ? 0 : (currentPage.value - 1) * pageSize.value + 1))
const pageEnd = computed(() => Math.min(currentPage.value * pageSize.value, total.value))

function formatAmount(amount: number) {
  return `￥${Math.abs(amount).toFixed(2)}`
}

function amountClass(amount: number) {
  return amount > 0 ? 'is-income' : 'is-expense'
}

function changeStatus(amount: number) {
  return amount > 0 ? '增加' : '减少'
}

function formatDateTime(value: string) {
  return value.replace('T', ' ').slice(0, 19)
}

function typeClass(type: string) {
  return {
    'is-recharge': type === '管理员充值',
    'is-check-in': type === '签到奖励',
    'is-consumption': type === 'AI聊天消耗',
  }
}

function handleSizeChange(size: number) {
  pageSize.value = size as 10 | 20 | 50
  currentPage.value = 1
  void loadBalanceChanges()
}

function handleCurrentChange(page: number) {
  currentPage.value = page
  void loadBalanceChanges()
}

async function loadBalanceChanges() {
  loading.value = true
  try {
    const page = await userAccountApi.balanceChanges({
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
  void loadBalanceChanges()
})
</script>

<style scoped lang="scss">
$primary-color: var(--app-primary);
$text-main: var(--app-text);
$text-muted: var(--app-text-muted);
$border-color: var(--app-border-muted);

.balance-changes-container {
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

.balance-change-table {
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

.amount {
  font-weight: 600;

  &.is-income {
    color: #16a34a;
  }

  &.is-expense {
    color: #ef4444;
  }
}

.change-status {
  font-weight: 500;
  color: var(--app-text);
}

:deep(.el-tag) {
  border-radius: 4px;
  font-weight: 500;

  &.is-recharge {
    border-color: #a7f3d0;
    background-color: #ecfdf5;
    color: #047857;
  }

  &.is-check-in {
    border-color: #bfdbfe;
    background-color: #eff6ff;
    color: #2563eb;
  }

  &.is-consumption {
    border-color: #fed7aa;
    background-color: #fff7ed;
    color: #c2410c;
  }
}

.pagination-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--app-text-regular);
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

.balance-change-card {
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

.label {
  flex: 0 0 auto;
  color: $text-muted;
  font-size: 12px;
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

  .balance-changes-container {
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
