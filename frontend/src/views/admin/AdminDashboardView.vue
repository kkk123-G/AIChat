<template>
  <div v-loading="loading" class="dashboard-container">
    <el-row :gutter="16">
      <el-col :xs="24" :sm="12" :md="6" v-for="(card, index) in cardData" :key="index" class="card-col">
        <el-card shadow="never" class="data-card">
          <div class="card-header">
            <span class="title">{{ card.title }}</span>
            <el-icon :color="card.color" :size="20">
              <component :is="card.icon" />
            </el-icon>
          </div>
          <div class="card-body">
            <span class="value">{{ card.value }}</span>
            <div class="sub-text" v-if="card.subText">
              <span>{{ card.subText }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :md="8" class="chart-col">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-header">状态分布</div>
          </template>
          <div ref="pieChartRef" class="chart-container"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="16" class="chart-col">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-header">调用次数趋势</div>
          </template>
          <div ref="lineChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :span="24">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-header">最近使用 (Top 5)</div>
          </template>
          <div ref="curveChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, markRaw } from 'vue'
import * as echarts from 'echarts'
import { User, DataLine, Check, Close } from '@element-plus/icons-vue'
import { adminDashboardApi, type AdminDashboard } from '@/utils/api'

const loading = ref(false)
const dashboard = ref<AdminDashboard | null>(null)
const cardData = computed(() => {
  const data = dashboard.value
  return [
    { title: '用户数量', value: formatCount(data?.userCount), icon: markRaw(User), color: '#409EFF', subText: `今日新增: +${formatCount(data?.newUsersToday)}` },
    { title: '今日请求', value: formatCount(data?.todayRequests), icon: markRaw(DataLine), color: '#E6A23C', subText: `总计: ${formatCount(data?.totalRequests)}` },
    { title: '今日成功', value: formatCount(data?.todaySuccesses), icon: markRaw(Check), color: '#67C23A', subText: `今日成功率: ${formatRate(data?.todaySuccessRate)} | 总成功率: ${formatRate(data?.totalSuccessRate)} | 总成功数: ${formatCount(data?.totalSuccesses)}` },
    { title: '今日失败', value: formatCount(data?.todayFailures), icon: markRaw(Close), color: '#F56C6C', subText: `今日失败率: ${formatRate(data?.todayFailureRate)} | 总失败率: ${formatRate(data?.totalFailureRate)} | 总失败数: ${formatCount(data?.totalFailures)}` },
  ]
})

const pieChartRef = ref<HTMLElement | null>(null)
const lineChartRef = ref<HTMLElement | null>(null)
const curveChartRef = ref<HTMLElement | null>(null)

let pieChartInstance: echarts.ECharts | null = null
let lineChartInstance: echarts.ECharts | null = null
let curveChartInstance: echarts.ECharts | null = null

function themeColor(name: string, fallback: string) {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback
}

function initPieChart(data: AdminDashboard) {
  if (!pieChartRef.value) return
  pieChartInstance?.dispose()
  pieChartInstance = echarts.init(pieChartRef.value)
  const option = {
    tooltip: { trigger: 'item' },
    legend: {
      top: '5%',
      left: 'center',
      icon: 'circle'
    },
    title: [
      {
        text: '今日状态',
        left: '25%',
        textAlign: 'center',
        bottom: '8%',
        textStyle: { fontSize: 13, color: themeColor('--app-text-muted', '#606266'), fontWeight: 'normal' }
      },
      {
        text: '总计状态',
        left: '75%',
        textAlign: 'center',
        bottom: '8%',
        textStyle: { fontSize: 13, color: themeColor('--app-text-muted', '#606266'), fontWeight: 'normal' }
      }
    ],
    color: ['#67C23A', '#F56C6C'],
    series: [
      {
        name: '今日状态',
        type: 'pie',
        radius: ['35%', '55%'],
        center: ['25%', '48%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 6,
          borderColor: themeColor('--app-surface', '#ffffff'),
          borderWidth: 2
        },
        label: { show: false },
        data: [
          { value: data.todaySuccesses, name: '成功' },
          { value: data.todayFailures, name: '失败' }
        ]
      },
      {
        name: '总计状态',
        type: 'pie',
        radius: ['35%', '55%'],
        center: ['75%', '48%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 6,
          borderColor: themeColor('--app-surface', '#ffffff'),
          borderWidth: 2
        },
        label: { show: false },
        data: [
          { value: data.totalSuccesses, name: '成功' },
          { value: data.totalFailures, name: '失败' }
        ]
      }
    ]
  }
  pieChartInstance.setOption(option)
}

function initLineChart(data: AdminDashboard) {
  if (!lineChartRef.value) return
  lineChartInstance?.dispose()
  lineChartInstance = echarts.init(lineChartRef.value)
  const option = {
    textStyle: { color: themeColor('--app-text-regular', '#606266') },
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '5%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.requestTrend.labels,
    },
    yAxis: {
      type: 'value',
      minInterval: 1
    },
    series: [
      {
        name: '调用次数',
        type: 'line',
        data: data.requestTrend.values,
        itemStyle: { color: themeColor('--app-primary-strong', '#409EFF') },
        lineStyle: { color: themeColor('--app-primary-strong', '#409EFF'), width: 3 },
        showSymbol: false,
      }
    ]
  }
  lineChartInstance.setOption(option)
}

function initCurveChart(data: AdminDashboard) {
  if (!curveChartRef.value) return
  curveChartInstance?.dispose()
  curveChartInstance = echarts.init(curveChartRef.value)
  const usernames = data.topUsers.map((item) => item.username)
  const option = {
    textStyle: { color: themeColor('--app-text-regular', '#606266') },
    tooltip: { trigger: 'axis' },
    legend: {
      top: 0,
      type: 'scroll',
      data: usernames,
    },
    grid: { top: 48, left: '4%', right: '4%', bottom: '5%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.requestTrend.labels,
    },
    yAxis: {
      type: 'value',
      name: '调用次数',
      minInterval: 1,
      nameTextStyle: {
        align: 'right',
        color: themeColor('--app-text-muted', '#909399'),
        fontWeight: 'bold',
        padding: [0, 8, 0, 0]
      }
    },
    series: data.topUsers.map((item) => ({
      name: item.username,
      type: 'line',
      smooth: true,
      showSymbol: false,
      lineStyle: { width: 2 },
      data: item.values,
    })),
  }
  curveChartInstance.setOption(option)
}

function formatCount(value: number | undefined) {
  return value === undefined ? '--' : new Intl.NumberFormat('zh-CN').format(value)
}

function formatRate(value: number | undefined) {
  return value === undefined ? '--' : `${Number(value).toFixed(1)}%`
}

async function loadDashboard() {
  loading.value = true
  try {
    dashboard.value = await adminDashboardApi.overview()
    await nextTick()
    initPieChart(dashboard.value)
    initLineChart(dashboard.value)
    initCurveChart(dashboard.value)
  } catch {
    dashboard.value = null
  } finally {
    loading.value = false
  }
}

function handleResize() {
  pieChartInstance?.resize()
  lineChartInstance?.resize()
  curveChartInstance?.resize()
}

function handleThemeChange() {
  if (!dashboard.value) return
  initPieChart(dashboard.value)
  initLineChart(dashboard.value)
  initCurveChart(dashboard.value)
}

onMounted(() => {
  void loadDashboard()
  window.addEventListener('resize', handleResize)
  window.addEventListener('themechange', handleThemeChange)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('themechange', handleThemeChange)
  pieChartInstance?.dispose()
  lineChartInstance?.dispose()
  curveChartInstance?.dispose()
})
</script>

<style scoped lang="scss">
.dashboard-container {
  padding: 20px;
  min-height: 100vh;
  box-sizing: border-box;

  .card-col {
    margin-bottom: 16px;
  }

  :deep(.el-card) {
    background-color: var(--app-surface) !important;
    border: 1px solid var(--app-border) !important;
    border-radius: 8px !important;
    box-shadow: none !important;
  }

  :deep(.el-card__header) {
    border-bottom: 1px solid var(--app-border-muted) !important;
    padding: 14px 16px !important;
  }

  :deep(.el-card__body) {
    padding: 16px !important;
  }

  .data-card {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      color: var(--app-text-muted);
      font-size: 14px;
    }

    .card-body {
      .value {
        font-size: 28px;
        font-weight: 600;
        color: var(--app-text);
        line-height: 1.2;
      }

      .sub-text {
        margin-top: 8px;
        font-size: 12px;
        color: var(--app-text-muted);
      }
    }
  }

  .chart-row {
    margin-bottom: 16px;
  }

  .chart-card {
    .chart-header {
      font-weight: 600;
      color: var(--app-text);
      font-size: 15px;
    }

    .chart-container {
      height: 320px;
      width: 100%;
    }
  }
}

@media screen and (max-width: 768px) {
  .dashboard-container {
    padding: 10px;

    .data-card {
      .card-body .value {
        font-size: 22px;
      }
    }

    .chart-card {
      margin-bottom: 16px;

      .chart-container {
        height: 260px;
      }
    }
  }
}
</style>
