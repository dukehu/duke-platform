<template>
  <el-card class="step-card" :class="{ 'step-card--new': isNew, 'step-card--collapsed': !expanded }">
    <template #header>
      <div class="step-header">
        <el-tag :type="getTagType(step.type)" size="small">{{ step.type }}</el-tag>
        <span class="step-title">{{ step.title }}</span>
        <el-button link @click="expanded = !expanded" class="toggle-btn">
          {{ expanded ? '折叠' : '展开' }}
        </el-button>
      </div>
    </template>

    <el-collapse-transition>
      <div v-show="expanded" class="step-content">
        <!-- 通俗解释 -->
        <div v-if="step.analogy" class="step-analogy">
          <span class="analogy-icon">💡</span>
          <p>{{ step.analogy }}</p>
        </div>

        <!-- 数据流向 -->
        <div v-if="step.dataFlow" class="step-dataflow">
          <span class="dataflow-label">数据流向</span>
          <pre class="dataflow-text">{{ step.dataFlow }}</pre>
        </div>

        <!-- 描述部分 -->
        <div v-if="step.description" class="step-description">
          <p>{{ step.description }}</p>
        </div>

        <!-- 元数据展示（如词表映射、token IDs） -->
        <div v-if="step.metadata && Object.keys(step.metadata).length > 0" class="step-metadata">
          <div v-if="step.metadata.input_text" class="metadata-item">
            <strong>输入文本：</strong>
            <code>{{ step.metadata.input_text }}</code>
          </div>
          <div v-if="step.metadata.token_ids" class="metadata-item">
            <strong>Token IDs：</strong>
            <code>{{ JSON.stringify(step.metadata.token_ids) }}</code>
          </div>
          <div v-if="step.metadata.char_mappings" class="metadata-item">
            <strong>字符映射：</strong>
            <div class="char-map">
              <span v-for="(mapping, idx) in step.metadata.char_mappings" :key="idx" class="char-map-item">
                {{ mapping }}
              </span>
            </div>
          </div>
        </div>

        <!-- 注意力流向（仅 ATTENTION_SOFTMAX 和 DECODER_MASKED_SOFTMAX 和 DECODER_CROSS_ATTN_SOFTMAX 步骤显示） -->
        <AttentionFlow
          v-if="attentionMatrix"
          :values="attentionMatrix.values"
          :tokens="attentionMatrix.tokens"
          :queryTokens="attentionMatrix.queryTokens"
          :keyTokens="attentionMatrix.keyTokens"
          :mode="attentionMatrix.mode"
        />

        <!-- 矩阵展示区 -->
        <div v-if="step.matrices && step.matrices.length > 0" class="matrices-container">
          <MatrixTable
            v-for="matrix in step.matrices"
            :key="matrix.label"
            :matrixData="matrix"
          />
        </div>

        <!-- 如果没有矩阵数据但有其他信息 -->
        <div v-else-if="!step.matrices || step.matrices.length === 0" class="no-data">
          <p>此步骤无矩阵数据</p>
        </div>
      </div>
    </el-collapse-transition>
  </el-card>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import MatrixTable from './MatrixTable.vue';
import AttentionFlow from './AttentionFlow.vue';

interface MatrixData {
  label: string;
  values: number[][];
  minVal: number;
  maxVal: number;
  rowLabels: string[];
  colLabels: string[];
  colorScheme: string;
}

interface TransformerStep {
  type: string;
  title: string;
  description?: string;
  analogy?: string;
  dataFlow?: string;
  headIndex?: string;
  matrices?: MatrixData[];
  metadata?: Record<string, any>;
  timestamp: number;
}

const props = defineProps<{
  step: TransformerStep;
  isNew?: boolean;
}>();

const attentionMatrix = computed((): {
  values: number[][];
  tokens?: string[];
  queryTokens?: string[];
  keyTokens?: string[];
  mode: 'self' | 'cross';
} | null => {
  const crossAttnTypes = ['DECODER_CROSS_ATTN_SOFTMAX'];
  const selfAttnTypes = ['ATTENTION_SOFTMAX', 'DECODER_MASKED_SOFTMAX'];

  if (selfAttnTypes.includes(props.step.type)) {
    const m = props.step.matrices?.[0];
    if (!m || !m.values || !m.rowLabels) return null;
    return {
      values: m.values,
      tokens: m.rowLabels,
      mode: 'self'
    };
  }

  if (crossAttnTypes.includes(props.step.type)) {
    const m = props.step.matrices?.[0];
    if (!m || !m.values || !m.rowLabels || !m.colLabels) return null;
    return {
      values: m.values,
      queryTokens: m.rowLabels,
      keyTokens: m.colLabels,
      mode: 'cross'
    };
  }
  return null;
});

const expanded = ref(true);

const typeTagMap: Record<string, 'success' | 'info' | 'warning' | 'danger'> = {
  TOKENIZATION: 'info',
  TOKEN_EMBEDDING: 'success',
  POSITIONAL_ENCODING: 'success',
  QKV_PROJECTION: 'warning',
  ATTENTION_SCORES: 'warning',
  ATTENTION_SCALE: 'warning',
  ATTENTION_SOFTMAX: 'warning',
  ATTENTION_WEIGHTED_SUM: 'warning',
  MULTI_HEAD_CONCAT: 'warning',
  ADD_NORM_1: 'info',
  FFN_LINEAR1: 'info',
  FFN_RELU: 'info',
  FFN_LINEAR2: 'info',
  ADD_NORM_2: 'info',
  OUTPUT_PROJECTION: 'success',
  DECODER_EMBEDDING: 'danger',
  DECODER_MASKED_SELF_ATTN: 'danger',
  DECODER_MASKED_SOFTMAX: 'danger',
  DECODER_MASKED_WEIGHTED_SUM: 'danger',
  DECODER_ADD_NORM_1: 'danger',
  DECODER_CROSS_ATTN_SCORES: 'danger',
  DECODER_CROSS_ATTN_SOFTMAX: 'danger',
  DECODER_CROSS_ATTN_WEIGHTED: 'danger',
  DECODER_ADD_NORM_2: 'danger',
  DECODER_FFN: 'danger',
  DECODER_ADD_NORM_3: 'danger',
  DECODER_OUTPUT_LOGITS: 'danger',
  DECODER_SOFTMAX_PROBS: 'danger',
  COMPLETE: 'success'
};

function getTagType(type: string): 'success' | 'info' | 'warning' | 'danger' {
  return typeTagMap[type] || 'info';
}
</script>

<style scoped lang="scss">
.step-card {
  margin: 12px 0;
  animation: slideIn 0.4s ease-out forwards;

  &.step-card--new {
    animation: slideIn 0.4s ease-out;
  }

  &.step-card--collapsed {
    :deep(.el-card__body) {
      padding: 0;
    }
  }
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.step-header {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;

  :deep(.el-tag) {
    flex-shrink: 0;
  }

  .step-title {
    flex: 1;
    font-weight: 600;
    color: #1f2937;
    font-size: 14px;
  }

  .toggle-btn {
    flex-shrink: 0;
    color: #6b7280;

    &:hover {
      color: #3b82f6;
    }
  }
}

.step-content {
  padding: 16px 0;
}

.step-description {
  padding: 0 16px 12px 16px;
  border-bottom: 1px solid #e5e7eb;
  margin-bottom: 12px;

  p {
    margin: 0;
    font-size: 13px;
    color: #4b5563;
    line-height: 1.5;
    white-space: pre-wrap;
    word-break: break-word;
  }
}

.step-metadata {
  padding: 0 16px 12px 16px;
  background: #f9fafb;
  border-radius: 6px;
  margin-bottom: 12px;

  .metadata-item {
    margin: 8px 0;
    font-size: 12px;

    strong {
      color: #374151;
      margin-right: 8px;
    }

    code {
      background: white;
      padding: 2px 6px;
      border-radius: 4px;
      color: #d97706;
      font-family: 'Courier New', monospace;
    }
  }

  .char-map {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 8px;

    .char-map-item {
      background: white;
      padding: 4px 8px;
      border-radius: 4px;
      border: 1px solid #e5e7eb;
      font-family: 'Courier New', monospace;
      font-size: 11px;
      color: #4b5563;
    }
  }
}

.matrices-container {
  padding: 0 16px 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.no-data {
  padding: 24px 16px;
  text-align: center;
  color: #9ca3af;
  font-size: 13px;
}

.step-analogy {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  padding: 10px 16px;
  background: #fffbeb;
  border-left: 3px solid #f59e0b;
  margin-bottom: 8px;

  .analogy-icon {
    font-size: 16px;
    flex-shrink: 0;
    margin-top: 1px;
  }

  p {
    margin: 0;
    font-size: 13px;
    color: #78350f;
    line-height: 1.6;
  }
}

.step-dataflow {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  padding: 8px 16px;
  background: #f0f9ff;
  border-left: 3px solid #38bdf8;
  margin-bottom: 8px;

  .dataflow-label {
    font-size: 11px;
    font-weight: 700;
    color: #0369a1;
    white-space: nowrap;
    padding-top: 1px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  .dataflow-text {
    margin: 0;
    font-size: 12px;
    color: #0c4a6e;
    font-family: 'Courier New', monospace;
    white-space: pre-wrap;
    line-height: 1.6;
    background: transparent;
  }
}
</style>
