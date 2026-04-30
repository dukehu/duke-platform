<template>
  <div class="arch-wrap">
    <svg :width="SVG_W" :height="SVG_H" :viewBox="`0 0 ${SVG_W} ${SVG_H}`">

      <!-- ===== ENCODER 列（左侧）===== -->
      <!-- ① Encoder 主链箭头 -->
      <line v-for="seg in encoderArrows" :key="seg.id"
        :x1="seg.x" :y1="seg.y1" :x2="seg.x" :y2="seg.y2"
        stroke="#94a3b8" stroke-width="1.5" marker-end="url(#arr)" />

      <!-- ② Encoder 块 -->
      <g v-for="blk in encoderBlocks" :key="blk.id"
        :transform="`translate(${blk.x}, ${blk.y})`">
        <rect :width="blk.w" :height="BH" rx="6"
          :fill="blk.id === activeBlockId ? '#fef9c3' : blk.fill"
          :stroke="blk.id === activeBlockId ? '#f59e0b' : blk.stroke"
          :stroke-width="blk.id === activeBlockId ? 2.5 : 1.5" />
        <text :x="blk.w / 2" :y="BH / 2 + 4" text-anchor="middle"
          font-size="10" font-weight="600"
          :fill="blk.id === activeBlockId ? '#92400e' : blk.textColor"
          font-family="system-ui, sans-serif">{{ blk.label }}</text>
        <circle v-if="blk.id === activeBlockId"
          :cx="blk.w - 8" cy="8" r="4" fill="#f59e0b">
          <animate attributeName="opacity" values="1;0.3;1" dur="1.2s" repeatCount="indefinite" />
        </circle>
      </g>

      <!-- ===== DECODER 列（右侧）===== -->
      <!-- ③ Cross-Attention 连接线（从 Encoder→Decoder） -->
      <path v-if="showCrossAttentionArrow"
        d="M 190 160 L 240 160 L 240 120 L 290 120"
        fill="none" stroke="#f97316" stroke-width="2" marker-end="url(#arrCross)" />
      <text x="215" y="140" font-size="9" fill="#f97316" font-weight="600">K,V</text>

      <!-- ④ Decoder 主链箭头 -->
      <line v-for="seg in decoderArrows" :key="seg.id"
        :x1="seg.x" :y1="seg.y1" :x2="seg.x" :y2="seg.y2"
        stroke="#94a3b8" stroke-width="1.5" marker-end="url(#arr)" />

      <!-- ⑤ Decoder 块 -->
      <g v-for="blk in decoderBlocks" :key="blk.id"
        :transform="`translate(${blk.x}, ${blk.y})`">
        <rect :width="blk.w" :height="BH" rx="6"
          :fill="blk.id === activeBlockId ? '#fef9c3' : blk.fill"
          :stroke="blk.id === activeBlockId ? '#f59e0b' : blk.stroke"
          :stroke-width="blk.id === activeBlockId ? 2.5 : 1.5" />
        <text :x="blk.w / 2" :y="BH / 2 + 4" text-anchor="middle"
          font-size="10" font-weight="600"
          :fill="blk.id === activeBlockId ? '#92400e' : blk.textColor"
          font-family="system-ui, sans-serif">{{ blk.label }}</text>
        <circle v-if="blk.id === activeBlockId"
          :cx="blk.w - 8" cy="8" r="4" fill="#f59e0b">
          <animate attributeName="opacity" values="1;0.3;1" dur="1.2s" repeatCount="indefinite" />
        </circle>
      </g>

      <!-- 箭头 marker 定义 -->
      <defs>
        <marker id="arr" markerWidth="6" markerHeight="6"
          refX="5" refY="3" orient="auto">
          <path d="M0,0 L6,3 L0,6 Z" fill="#94a3b8" />
        </marker>
        <marker id="arrCross" markerWidth="6" markerHeight="6"
          refX="5" refY="3" orient="auto">
          <path d="M0,0 L6,3 L0,6 Z" fill="#f97316" />
        </marker>
      </defs>
    </svg>

    <!-- 当前步骤说明 -->
    <div v-if="activeBlock" class="arch-caption">
      <span class="cap-dot" />
      <span>当前：<strong>{{ activeBlock.label }}</strong> — {{ activeBlock.tip }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{ activeStepType: string }>();

// ── 布局常量 ──────────────────────────────────────────
const SVG_W = 680;
const BW = 140;   // 块宽
const BH = 30;    // 块高
const ENC_X = 20; // Encoder 列 X
const DEC_X = 360; // Decoder 列 X

// Encoder 块 Y 坐标
const ENC_TOPS = [0, 48, 96, 150, 200, 250, 300];
// Decoder 块 Y 坐标（与 Encoder 对齐）
const DEC_TOPS = [0, 48, 96, 150, 200, 250, 300];
const SVG_H = 350;

// ── Encoder 块定义 ───────────────────────────────────
const encoderBlocks = computed(() => [
  {
    id: 'enc-embed', label: 'Embedding', x: ENC_X, y: ENC_TOPS[0], w: BW,
    fill: '#f0fdf4', stroke: '#86efac', textColor: '#166534',
    tip: '源序列 token 嵌入',
    types: ['TOKEN_EMBEDDING'],
  },
  {
    id: 'enc-pos', label: '位置编码', x: ENC_X, y: ENC_TOPS[1], w: BW,
    fill: '#f0fdf4', stroke: '#86efac', textColor: '#166534',
    tip: '注入位置信息',
    types: ['POSITIONAL_ENCODING'],
  },
  {
    id: 'enc-mha', label: 'Self-Attn', x: ENC_X, y: ENC_TOPS[2], w: BW,
    fill: '#fff7ed', stroke: '#fdba74', textColor: '#9a3412',
    tip: '编码器自注意力',
    types: ['QKV_PROJECTION', 'ATTENTION_SCORES', 'ATTENTION_SCALE',
            'ATTENTION_SOFTMAX', 'ATTENTION_WEIGHTED_SUM', 'MULTI_HEAD_CONCAT'],
  },
  {
    id: 'enc-norm1', label: 'Add & Norm ①', x: ENC_X, y: ENC_TOPS[3], w: BW,
    fill: '#eff6ff', stroke: '#93c5fd', textColor: '#1e40af',
    tip: '残差 + 层归一化',
    types: ['ADD_NORM_1'],
  },
  {
    id: 'enc-ffn', label: 'FFN', x: ENC_X, y: ENC_TOPS[4], w: BW,
    fill: '#faf5ff', stroke: '#c4b5fd', textColor: '#6b21a8',
    tip: '前馈网络',
    types: ['FFN_LINEAR1', 'FFN_RELU', 'FFN_LINEAR2'],
  },
  {
    id: 'enc-norm2', label: 'Add & Norm ②', x: ENC_X, y: ENC_TOPS[5], w: BW,
    fill: '#eff6ff', stroke: '#93c5fd', textColor: '#1e40af',
    tip: 'Encoder 输出',
    types: ['ADD_NORM_2'],
  },
]);

// ── Decoder 块定义 ───────────────────────────────────
const decoderBlocks = computed(() => [
  {
    id: 'dec-embed', label: 'Embedding', x: DEC_X, y: DEC_TOPS[0], w: BW,
    fill: '#f0fdf4', stroke: '#86efac', textColor: '#166534',
    tip: '目标序列 token 嵌入',
    types: ['DECODER_EMBEDDING'],
  },
  {
    id: 'dec-masked', label: 'Masked SA', x: DEC_X, y: DEC_TOPS[1], w: BW,
    fill: '#fff7ed', stroke: '#fdba74', textColor: '#9a3412',
    tip: 'Masked 自注意力（禁止看未来）',
    types: ['DECODER_MASKED_SELF_ATTN', 'DECODER_MASKED_SOFTMAX', 'DECODER_MASKED_WEIGHTED_SUM'],
  },
  {
    id: 'dec-norm1', label: 'Add & Norm ①', x: DEC_X, y: DEC_TOPS[2], w: BW,
    fill: '#eff6ff', stroke: '#93c5fd', textColor: '#1e40af',
    tip: '残差 + 层归一化',
    types: ['DECODER_ADD_NORM_1'],
  },
  {
    id: 'dec-cross', label: 'Cross-Attn', x: DEC_X, y: DEC_TOPS[3], w: BW,
    fill: '#fed7aa', stroke: '#f97316', textColor: '#92400e',
    tip: '交叉注意力（Decoder→Encoder）',
    types: ['DECODER_CROSS_ATTN_SCORES', 'DECODER_CROSS_ATTN_SOFTMAX', 'DECODER_CROSS_ATTN_WEIGHTED'],
  },
  {
    id: 'dec-norm2', label: 'Add & Norm ②', x: DEC_X, y: DEC_TOPS[4], w: BW,
    fill: '#eff6ff', stroke: '#93c5fd', textColor: '#1e40af',
    tip: '残差 + 层归一化',
    types: ['DECODER_ADD_NORM_2'],
  },
  {
    id: 'dec-ffn', label: 'FFN', x: DEC_X, y: DEC_TOPS[5], w: BW,
    fill: '#faf5ff', stroke: '#c4b5fd', textColor: '#6b21a8',
    tip: '前馈网络',
    types: ['DECODER_FFN'],
  },
  {
    id: 'dec-norm3', label: 'Add & Norm ③', x: DEC_X, y: DEC_TOPS[6], w: BW,
    fill: '#eff6ff', stroke: '#93c5fd', textColor: '#1e40af',
    tip: '残差 + 层归一化',
    types: ['DECODER_ADD_NORM_3', 'DECODER_OUTPUT_LOGITS', 'DECODER_SOFTMAX_PROBS'],
  },
]);

// 合并所有块用于找当前活跃块
const allBlocks = computed(() => [...encoderBlocks.value, ...decoderBlocks.value]);

const activeBlockId = computed(() =>
  allBlocks.value.find(b => b.types.includes(props.activeStepType))?.id ?? ''
);

const activeBlock = computed(() =>
  allBlocks.value.find(b => b.id === activeBlockId.value) ?? null
);

// ── Encoder 箭头 ─────────────────────────────────────
const encoderArrows = computed(() =>
  encoderBlocks.value.slice(0, -1).map((b, i) => ({
    id: `enc-arr${i}`,
    x: ENC_X + BW / 2,
    y1: b.y + BH,
    y2: encoderBlocks.value[i + 1].y - 2,
  }))
);

// ── Decoder 箭头 ─────────────────────────────────────
const decoderArrows = computed(() =>
  decoderBlocks.value.slice(0, -1).map((b, i) => ({
    id: `dec-arr${i}`,
    x: DEC_X + BW / 2,
    y1: b.y + BH,
    y2: decoderBlocks.value[i + 1].y - 2,
  }))
);

// ── Cross-Attention 连接线显示条件 ─────────────────
const showCrossAttentionArrow = computed(() =>
  activeBlockId.value.startsWith('dec-cross')
);
</script>

<style scoped lang="scss">
.arch-wrap {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 10px 12px 8px;
  margin-bottom: 10px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.arch-caption {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #64748b;

  strong { color: #1e293b; }
}

.cap-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f59e0b;
  flex-shrink: 0;
  animation: pulse 1.2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.5); opacity: 0.6; }
}
</style>
