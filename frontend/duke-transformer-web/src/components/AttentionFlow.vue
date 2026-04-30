<template>
  <div class="attn-viz">
    <div class="viz-header">
      <span class="viz-label">{{ displayMode === 'cross' ? '交叉注意力' : '注意力连线图' }}</span>
      <span class="viz-hint">{{ displayMode === 'cross' ? 'Decoder 关注 Encoder 的程度' : '点击上方词语 → 线越粗 = 关注越强' }}</span>
    </div>

    <div class="svg-scroll">
      <svg :width="svgW" :height="svgH" :viewBox="`0 0 ${svgW} ${svgH}`">
        <!-- 角色标签 -->
        <text :x="LP - 6" :y="QY + 5" text-anchor="end" font-size="10" fill="#94a3b8" font-family="system-ui">{{ displayMode === 'cross' ? 'Dec' : 'Q' }}</text>
        <text :x="LP - 6" :y="KY + 5" text-anchor="end" font-size="10" fill="#94a3b8" font-family="system-ui">{{ displayMode === 'cross' ? 'Enc' : 'K' }}</text>

        <!-- 曲线连接（在 token 盒子下面渲染，避免遮住文字） -->
        <g v-if="sel !== null">
          <path
            v-for="j in keyTokens.length"
            :key="`p${j}`"
            :d="curve(sel, j - 1)"
            fill="none"
            :stroke="lineColor(values[sel][j - 1])"
            :stroke-width="lineW(values[sel][j - 1])"
            :stroke-opacity="Math.max(values[sel][j - 1] * 1.8, 0.12)"
            stroke-linecap="round"
          />
        </g>

        <!-- Key 行 token（下方） -->
        <g v-for="(tok, j) in keyTokens" :key="`k${j}`">
          <rect
            :x="cx(j) - TW / 2" :y="KY - TH / 2"
            :width="TW" :height="TH" rx="6"
            :fill="kFill(j)" :stroke="kStroke(j)" stroke-width="1.5"
          />
          <text :x="cx(j)" :y="KY + 5" text-anchor="middle" font-size="12"
            :fill="sel !== null && j === sel ? '#fff' : '#1e293b'"
            font-family="system-ui, sans-serif">{{ tok }}</text>
          <!-- 百分比标签 -->
          <text v-if="sel !== null"
            :x="cx(j)" :y="KY + TH / 2 + 14"
            text-anchor="middle" font-size="10" font-weight="600"
            :fill="pctColor(values[sel][j])"
            font-family="system-ui, sans-serif">
            {{ pct(values[sel][j]) }}
          </text>
        </g>

        <!-- Query 行 token（上方，可点击） -->
        <g v-for="(tok, i) in queryTokens" :key="`q${i}`"
          style="cursor:pointer" @click="sel = i">
          <rect
            :x="cx(i) - TW / 2" :y="QY - TH / 2"
            :width="TW" :height="TH" rx="6"
            :fill="i === sel ? '#4f46e5' : '#f8fafc'"
            :stroke="i === sel ? '#4f46e5' : '#cbd5e1'"
            stroke-width="2"
          />
          <text :x="cx(i)" :y="QY + 5" text-anchor="middle" font-size="12"
            :fill="i === sel ? '#fff' : '#1e293b'"
            font-family="system-ui, sans-serif">{{ tok }}</text>
        </g>
      </svg>
    </div>

    <!-- 图例 -->
    <div class="legend">
      <span class="legend-item" style="color:#dc2626">━━ 强关注 (&gt;35%)</span>
      <span class="legend-item" style="color:#f59e0b">━━ 中等 (15-35%)</span>
      <span class="legend-item" style="color:#93c5fd">━ 弱 (&lt;15%)</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';

const props = defineProps<{
  values: number[][];
  tokens?: string[];        // self-attention 用（行列相同）
  queryTokens?: string[];   // cross-attention 用（Decoder tokens，上方）
  keyTokens?: string[];     // cross-attention 用（Encoder tokens，下方）
  mode?: 'self' | 'cross';
}>();

const sel = ref<number>(0);

// 布局常量
const TW = 66;   // token 盒宽
const TH = 26;   // token 盒高
const COL = 80;  // 列宽
const LP = 28;   // 左侧边距（给 Q/K 标签留位）
const QY = 26;   // Query 行中心 Y
const KY = 120;  // Key 行中心 Y

const displayMode = computed(() => props.mode || 'self');
const queryTokens = computed(() => props.queryTokens || props.tokens || []);
const keyTokens = computed(() => props.keyTokens || props.tokens || []);

const svgW = computed(() => LP + Math.max(queryTokens.value.length, keyTokens.value.length) * COL + 16);
const svgH = computed(() => KY + TH / 2 + 22);

function cx(i: number) { return LP + i * COL + COL / 2; }

function curve(qi: number, ki: number): string {
  const x1 = cx(qi), y1 = QY + TH / 2 + 1;
  const x2 = cx(ki), y2 = KY - TH / 2 - 1;
  const mid = (y1 + y2) / 2;
  return `M ${x1} ${y1} C ${x1} ${mid} ${x2} ${mid} ${x2} ${y2}`;
}

function lineW(w: number): number { return Math.max(w * 10, 0.4); }

function lineColor(w: number): string {
  if (w > 0.35) return '#dc2626';
  if (w > 0.15) return '#f59e0b';
  return '#93c5fd';
}

function pct(w: number): string { return (w * 100).toFixed(1) + '%'; }

function pctColor(w: number): string {
  if (w > 0.35) return '#dc2626';
  if (w > 0.15) return '#d97706';
  return '#94a3b8';
}

function kFill(j: number): string {
  if (sel.value === null) return '#f8fafc';
  if (j === sel.value) return '#4f46e5';
  const w = props.values[sel.value][j];
  if (w > 0.35) return '#fee2e2';
  if (w > 0.15) return '#fef3c7';
  return '#f8fafc';
}

function kStroke(j: number): string {
  if (sel.value === null) return '#cbd5e1';
  if (j === sel.value) return '#4f46e5';
  const w = props.values[sel.value][j];
  if (w > 0.35) return '#fca5a5';
  if (w > 0.15) return '#fcd34d';
  return '#e2e8f0';
}

onMounted(() => { sel.value = 0; });
</script>

<style scoped lang="scss">
.attn-viz {
  border: 1px solid #e0e7ff;
  border-radius: 10px;
  padding: 10px 12px 8px;
  background: #fafbff;
  margin-bottom: 12px;
}

.viz-header {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 6px;
}

.viz-label {
  font-size: 12px;
  font-weight: 700;
  color: #4f46e5;
  text-transform: uppercase;
  letter-spacing: 0.4px;
}

.viz-hint {
  font-size: 11px;
  color: #94a3b8;
}

.svg-scroll {
  overflow-x: auto;
}

.legend {
  display: flex;
  gap: 14px;
  margin-top: 4px;
  padding-left: 28px;
}

.legend-item {
  font-size: 11px;
  font-weight: 600;
}
</style>
