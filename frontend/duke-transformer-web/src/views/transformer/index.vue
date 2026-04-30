<template>
  <div class="transformer-page">
    <!-- 主容器：三个顶级Tab -->
    <el-tabs v-model="activeTab" class="main-tabs-container">
      <!-- Tab 1: 支持词汇表 -->
      <el-tab-pane label="📋 支持词汇表" name="vocab">
        <el-card class="vocab-card">
          <div class="vocab-section">
            <div class="section-title">支持词汇 ({{ vocabList.length }})</div>

            <!-- 中文字符 -->
            <div v-if="categorizedVocab.chinese.length > 0" class="vocab-subsection">
              <div class="subsection-title">中文 ({{ categorizedVocab.chinese.length }})</div>
              <div class="vocab-list">
                <span v-for="c in categorizedVocab.chinese" :key="c" class="vocab-item">{{ c }}</span>
              </div>
            </div>

            <!-- 英文字符 -->
            <div v-if="categorizedVocab.english.length > 0" class="vocab-subsection">
              <div class="subsection-title">英文 ({{ categorizedVocab.english.length }})</div>
              <div class="vocab-list">
                <span v-for="c in categorizedVocab.english" :key="c" class="vocab-item">{{ c }}</span>
              </div>
            </div>

            <!-- 数字 -->
            <div v-if="categorizedVocab.digits.length > 0" class="vocab-subsection">
              <div class="subsection-title">数字 ({{ categorizedVocab.digits.length }})</div>
              <div class="vocab-list">
                <span v-for="c in categorizedVocab.digits" :key="c" class="vocab-item">{{ c }}</span>
              </div>
            </div>

            <!-- 特殊符号 -->
            <div v-if="categorizedVocab.special.length > 0" class="vocab-subsection">
              <div class="subsection-title">特殊 ({{ categorizedVocab.special.length }})</div>
              <div class="vocab-list">
                <span v-for="c in categorizedVocab.special" :key="c" class="vocab-item">{{ c }}</span>
              </div>
            </div>
          </div>
          <el-alert title="输入提示" type="info" :closable="false" class="tips">
            <p>系统采用词级分词，支持中文词汇、英文字母和数字。</p>
            <p>推荐输入：hello、用电电鳗鱼、会不会</p>
          </el-alert>
        </el-card>
      </el-tab-pane>

      <!-- Tab 2: 交互演示 -->
      <el-tab-pane label="⚙️ 交互演示" name="demo">
        <div class="demo-content">
          <!-- 步骤导航 -->
          <div class="steps-nav">
            <el-steps :active="activeMainStep" finish-status="success" align-center simple>
              <el-step title="配置参数" @click="activeMainStep = 0"/>
              <el-step title="分词结果" @click="activeMainStep = 1"/>
              <el-step title="Encoder" @click="activeMainStep = 2"/>
              <el-step title="Decoder" @click="activeMainStep = 3"/>
            </el-steps>
          </div>

          <!-- 步骤内容 -->
          <div class="steps-content">
            <!-- Step 0: 配置参数 -->
            <div v-if="activeMainStep === 0" class="step-content-panel">
              <el-form :model="demoConfig" label-width="100px" size="default">
                <el-form-item label="输入文本">
                  <el-input
                      v-model="demoConfig.text"
                      placeholder="输入英文或中文文本 (最多8个token)"
                      clearable
                  />
                </el-form-item>
                <el-form-item label="嵌入维度">
                  <el-select v-model.number="demoConfig.embeddingDim">
                    <el-option label="4" :value="4"/>
                    <el-option label="8" :value="8"/>
                    <el-option label="16" :value="16"/>
                    <el-option label="32" :value="32"/>
                  </el-select>
                </el-form-item>
                <el-form-item label="注意力头数">
                  <el-select v-model.number="demoConfig.numHeads">
                    <el-option label="1" :value="1"/>
                    <el-option label="2" :value="2"/>
                    <el-option label="4" :value="4"/>
                  </el-select>
                </el-form-item>
                <el-form-item label="Encoder层数">
                  <el-select v-model.number="demoConfig.numLayers">
                    <el-option label="1" :value="1"/>
                    <el-option label="2" :value="2"/>
                  </el-select>
                </el-form-item>
                <el-form-item>
                  <el-button
                      type="primary"
                      size="large"
                      :loading="running"
                      @click="handleRun"
                      :disabled="!demoConfig.text.trim()"
                  >
                    {{ running ? '运行中 ⏳' : '▶ 运行 Transformer' }}
                  </el-button>
                  <el-button v-if="running" @click="handleStop" type="danger">
                    ⏹ 停止
                  </el-button>
                </el-form-item>
              </el-form>
            </div>

            <!-- Step 1: 分词结果 -->
            <div v-if="activeMainStep === 1" class="step-content-panel">
              <el-card class="tokenize-display">
                <div class="tokenize-section">
                  <h4>源序列分词结果 (Encoder 输入)</h4>
                  <div class="tokenize-text">
                    <strong>文本：</strong>{{ demoConfig.text }}
                  </div>
                  <div class="tokenize-tokens">
                    <div v-for="(token, idx) in srcTokenizeResult?.tokens || []" :key="`src-${idx}`" class="token-with-id">
                      <span class="token-text">{{ token }}</span>
                      <span class="token-id">[ID:{{ srcTokenizeResult?.tokenIds[idx] }}]</span>
                      <span v-if="srcTokenizeResult?.unknownTokens.includes(token)" class="token-unknown">UNK</span>
                    </div>
                  </div>
                  <div class="tokenize-info">词数：{{ srcTokenizeResult?.tokens.length || 0 }} / {{ maxSeqLen }}</div>
                </div>

                <div v-if="(srcTokenizeResult?.unknownTokens.length || 0) > 0" class="unknown-warning">
                  <el-alert
                    :title="`以下词不在词表中，将映射为 &lt;UNK&gt;：${(srcTokenizeResult?.unknownTokens || []).join('、')}`"
                    type="warning"
                    :closable="false"
                  />
                </div>
              </el-card>
            </div>

            <!-- Step 2: Encoder 计算步骤 -->
            <div v-if="activeMainStep === 2" class="step-content-panel transformer-layout">
              <!-- 左右两栏布局 -->
              <div v-if="encoderSteps.length > 0" class="transformer-container">
                <!-- 左侧：步骤列表 -->
                <div class="steps-sidebar">
                  <div class="steps-menu">
                    <div
                        v-for="(step, idx) in encoderSteps"
                        :key="`encoder-step-menu-${idx}`"
                        :class="['step-menu-item', { active: selectedEncoderStepIndex === idx }]"
                        @click="selectedEncoderStepIndex = idx"
                    >
                      <div class="step-number">{{ idx + 1 }}</div>
                      <div class="step-info">
                        <div class="step-type">{{ step.type }}</div>
                        <div class="step-title-short">{{ step.title }}</div>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 右侧：步骤详情 -->
                <div class="step-detail">
                  <ArchDiagram :activeStepType="encoderSteps[selectedEncoderStepIndex]?.type || ''" />
                  <TransformerStepCard
                      style="margin:0"
                      v-if="encoderSteps[selectedEncoderStepIndex]"
                      :step="encoderSteps[selectedEncoderStepIndex]"
                      :isNew="selectedEncoderStepIndex === encoderSteps.length - 1 && running"
                  />
                </div>
              </div>
            </div>

            <!-- Step 3: 自回归生成 + Decoder计算步骤 -->
            <div v-if="activeMainStep === 3" class="step-content-panel autoregressive-panel">
              <!-- 生成序列展示 - 固定顶部 -->
              <el-card class="generation-sequence-card">
                <h4 style="margin: 0 0 12px 0;">📊 生成序列</h4>
                <div class="sequence-tokens">
                  <el-tag type="success" class="token-tag">&lt;BOS&gt;</el-tag>
                  <el-tag
                      v-for="(step, idx) in generatedTokens"
                      :key="`gen-token-${idx}`"
                      :class="['token-tag', { 'fade-in': idx === generatedTokens.length - 1 && running }]"
                  >
                    {{ step.token }}
                  </el-tag>
                  <el-tag v-if="completed" type="success" class="token-tag">&lt;EOS&gt;</el-tag>
                </div>
              </el-card>

              <!-- Decoder步骤展示区 -->
              <el-tabs v-if="decoderSteps.length > 0" class="decoder-tabs-container">
                <!-- Tab 1: 生成摘要 -->
                <el-tab-pane label="📈 生成摘要">
                  <div class="generation-steps">
                    <div
                        v-for="(step, idx) in generatedTokens"
                        :key="`gen-step-${idx}`"
                        class="generation-step-card"
                    >
                      <div class="step-header">
                        <span class="step-num">第 {{ idx + 1 }} 步</span>
                        <span class="selected-token">选中：<strong>{{ step.token }}</strong></span>
                        <span class="token-prob">概率：{{ (step.topk && step.topk[0] ? Math.round(step.topk[0].prob * 1000) / 10 : 0) }}%</span>
                      </div>
                      <div v-if="step.topk" class="top5-bars">
                        <div v-for="(item, i) in step.topk" :key="`top5-${idx}-${i}`" class="prob-bar">
                          <div class="bar-label">{{ item.token }}</div>
                          <div class="bar-container">
                            <div class="bar-fill" :style="{ width: item.prob * 100 + '%' }"></div>
                          </div>
                          <div class="bar-value">{{ Math.round(item.prob * 1000) / 10 }}%</div>
                        </div>
                      </div>
                    </div>
                  </div>
                </el-tab-pane>

                <!-- Tab 2: Decoder计算步骤 -->
                <el-tab-pane label="⚙️ Decoder 详细步骤" class="decoder-steps-tab">
                  <div class="decoder-layout">
                    <!-- 左侧：步骤列表 -->
                    <div class="decoder-sidebar">
                      <div class="steps-menu">
                        <div
                            v-for="(step, idx) in decoderSteps"
                            :key="`decoder-step-menu-${idx}`"
                            :class="['step-menu-item', { active: selectedDecoderStepIndex === idx }]"
                            @click="selectedDecoderStepIndex = idx"
                        >
                          <div class="step-number">{{ idx + 1 }}</div>
                          <div class="step-info">
                            <div class="step-type">{{ step.type }}</div>
                            <div class="step-title-short">{{ step.title }}</div>
                          </div>
                        </div>
                      </div>
                    </div>

                    <!-- 右侧：步骤详情 -->
                    <div class="decoder-detail">
                      <ArchDiagram :activeStepType="decoderSteps[selectedDecoderStepIndex]?.type || ''" />
                      <TransformerStepCard
                          v-if="decoderSteps[selectedDecoderStepIndex]"
                          :step="decoderSteps[selectedDecoderStepIndex]"
                          :isNew="selectedDecoderStepIndex === decoderSteps.length - 1 && running"
                      />
                    </div>
                  </div>
                </el-tab-pane>
              </el-tabs>

              <div v-else-if="!running && !completed" class="autoregressive-empty">
                <el-empty description="点击运行开始生成" />
              </div>

              <!-- 完成提示 -->
              <el-alert
                v-if="completed"
                class="completion-alert"
                title="✓ 生成完成"
                :description="`成功生成 ${generatedTokens.length} 个 token，总共 ${generatedTokens.length + 2} 个 token（包括 BOS 和 EOS）。共展示 ${decoderSteps.length} 个 Decoder 计算步骤`"
                type="success"
                :closable="false"
              />
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- Tab 3: 学习文档 -->
      <el-tab-pane label="📚 学习文档" name="guide">
        <div class="guide-content">
          <el-skeleton v-if="loadingGuide" :rows="10" animated/>
          <MarkdownViewer v-else-if="guideContent" :content="guideContent"/>
          <el-empty v-else description="文档加载失败，请重试"/>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import {ref, reactive, onMounted, computed} from 'vue';
import {ElMessage} from 'element-plus';
import TransformerStepCard from '@/components/TransformerStepCard.vue';
import MarkdownViewer from '@/components/MarkdownViewer.vue';
import ArchDiagram from '@/components/ArchDiagram.vue';
import {
  getGuide,
  getVocabList,
  createEncoderSSE,
  createAutoRegressiveSSE,
  tokenize,
  type TransformerStep,
  type TokenizeResult,
  type EncoderRunParams,
  type AutoRegressiveParams
} from '@/api/transformer';

// 状态管理
const activeTab = ref<'demo' | 'guide'>('demo');
const activeMainStep = ref(0);
const selectedEncoderStepIndex = ref(0);
const selectedDecoderStepIndex = ref(0);
const running = ref(false);
const completed = ref(false);
const loadingGuide = ref(true);
const guideContent = ref('');
const vocabList = ref<string[]>([]);
let encoderEventSource: EventSource | null = null;
let decoderEventSource: EventSource | null = null;

// 配置表单
const demoConfig = reactive({
  text: '用电电鳗鱼鳗鱼会不会被电死',
  embeddingDim: 4,
  numHeads: 1,
  numLayers: 1
});

// Encoder 步骤列表
const encoderSteps = ref<TransformerStep[]>([]);
// Decoder 步骤列表（用于兼容，不再使用）
const decoderSteps = ref<TransformerStep[]>([]);

// 自回归生成的 token 列表
interface GeneratedTokenInfo {
  token: string;
  step: number;
  topk?: Array<{ token: string; id: number; prob: number }>;
}
const generatedTokens = ref<GeneratedTokenInfo[]>([]);

// 分词结果
const srcTokenizeResult = ref<TokenizeResult | null>(null);
const tgtTokenizeResult = ref<TokenizeResult | null>(null);
const maxSeqLen = 8;

// 用于在 Encoder 完成时存储的数据
let storedEncoderOutputJson = '';
let storedSrcTokenIdsJson = '';

// 分类词汇表
const categorizedVocab = computed(() => {
  const chinese: string[] = [];
  const english: string[] = [];
  const digits: string[] = [];
  const special: string[] = [];

  vocabList.value.forEach(char => {
    if (char === ' ') {
      special.push('␣');
    } else if (char === '<' || char === '?') {
      special.push(char);
    } else if (/^[一-鿿]/.test(char)) {
      chinese.push(char);
    } else if (/^[a-zA-Z]/.test(char)) {
      english.push(char);
    } else if (/^\d/.test(char)) {
      digits.push(char);
    } else {
      special.push(char);
    }
  });

  return {chinese, english, digits, special};
});

/**
 * 运行 Transformer（4 阶段流程）
 */
async function handleRun() {
  if (!demoConfig.text.trim()) {
    ElMessage.warning('请输入源文本');
    return;
  }

  // 重置状态
  encoderSteps.value = [];
  decoderSteps.value = [];
  generatedTokens.value = [];
  srcTokenizeResult.value = null;
  tgtTokenizeResult.value = null;
  completed.value = false;
  running.value = true;
  activeMainStep.value = 0;
  selectedEncoderStepIndex.value = 0;
  selectedDecoderStepIndex.value = 0;

  try {
    // ========== Phase 1: 分词 ==========
    activeMainStep.value = 1;
    ElMessage.info('第 1 步：分词中...');

    try {
      const srcResult = await tokenize(demoConfig.text);
      srcTokenizeResult.value = srcResult;
      await new Promise(resolve => setTimeout(resolve, 500)); // 展示分词结果
    } catch (error) {
      ElMessage.error('分词失败');
      running.value = false;
      return;
    }

    // ========== Phase 2: Encoder SSE ==========
    activeMainStep.value = 2;
    ElMessage.info('第 2 步：运行 Encoder...');
    encoderSteps.value = [];

    await new Promise<void>((resolve, reject) => {
      const encoderParams: EncoderRunParams = {
        srcText: demoConfig.text,
        embeddingDim: demoConfig.embeddingDim,
        numHeads: demoConfig.numHeads,
        numLayers: demoConfig.numLayers
      };

      encoderEventSource = createEncoderSSE(
        encoderParams,
        (step: TransformerStep) => {
          encoderSteps.value.push(step);
        },
        (encoderOutputJson: string, srcTokenIdsJson: string) => {
          storedEncoderOutputJson = encoderOutputJson;
          storedSrcTokenIdsJson = srcTokenIdsJson;
          resolve();
        },
        (error: Event) => {
          ElMessage.error('Encoder 运行失败');
          reject(error);
        }
      );
    });

    // ========== Phase 3: 自回归生成 SSE ==========
    activeMainStep.value = 3;
    ElMessage.info('第 3 步：自回归生成...');
    generatedTokens.value = [];

    await new Promise<void>((resolve, reject) => {
      const autoRegParams: AutoRegressiveParams = {
        encoderOutputJson: storedEncoderOutputJson,
        srcTokenIdsJson: storedSrcTokenIdsJson,
        embeddingDim: demoConfig.embeddingDim,
        numHeads: demoConfig.numHeads,
        numLayers: demoConfig.numLayers
      };

      decoderEventSource = createAutoRegressiveSSE(
        autoRegParams,
        (step: TransformerStep) => {
          // 收集所有Decoder步骤
          decoderSteps.value.push(step);

          // 同时处理TOKEN_GENERATED步骤以提取生成的词
          if (step.type === 'TOKEN_GENERATED') {
            const metadata = step.metadata as any;
            const tokenInfo: GeneratedTokenInfo = {
              token: metadata.token,
              step: metadata.step,
              topk: metadata.top5
            };
            generatedTokens.value.push(tokenInfo);
          }
        },
        () => {
          resolve();
        },
        (error: Event) => {
          ElMessage.error('自回归生成失败');
          reject(error);
        }
      );
    });

    // ========== 完成 ==========
    running.value = false;
    completed.value = true;
    ElMessage.success(`生成完成！生成了 ${generatedTokens.value.length} 个 token`);

  } catch (error) {
    running.value = false;
    ElMessage.error('Transformer 运行失败');
    console.error(error);
  }
}

/**
 * 停止运行
 */
function handleStop() {
  if (encoderEventSource) {
    encoderEventSource.close();
    encoderEventSource = null;
  }
  if (decoderEventSource) {
    decoderEventSource.close();
    decoderEventSource = null;
  }
  running.value = false;
  ElMessage.info('已停止');
}

/**
 * 滚动到指定步骤
 */
/**
 * 加载词汇表
 */
async function loadVocab() {
  try {
    const res = await getVocabList();
    if (Array.isArray(res)) {
      vocabList.value = res;
    }
  } catch (error) {
    console.error('加载词汇表失败', error);
  }
}

/**
 * 加载学习文档
 */
async function loadGuide() {
  try {
    loadingGuide.value = true;
    const res = await getGuide();
    const content = typeof res === 'string' ? res : (res as any);
    if (content && content.length > 0) {
      guideContent.value = content;
    } else {
      console.error('Unexpected response format:', res);
      ElMessage.error('文档格式错误');
    }
  } catch (error) {
    console.error('加载文档失败', error);
    ElMessage.error('无法加载文档，请检查网络连接');
  } finally {
    loadingGuide.value = false;
  }
}

// 初始化
onMounted(() => {
  loadVocab();
  loadGuide();
});
</script>

<style scoped lang="scss">
.transformer-page {
  height: 96vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  padding: 5px;
  overflow: hidden;
}

.main-tabs-container {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;

  :deep(.el-tabs__header) {
    background: #f9fafb;
    border-bottom: 2px solid #e5e7eb;
    border-radius: 12px 12px 0 0;
  }

  :deep(.el-tabs__content) {
    flex: 1;
    overflow-y: auto;
  }
}

.vocab-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  height: calc(100vh - 89px);

  :deep(.el-card__header) {
    padding: 16px;
    border-bottom: 1px solid #f0f0f0;
  }
}

.vocab-section {
  padding: 12px;
  overflow-y: auto;
  flex: 1;

  .section-title {
    font-weight: 600;
    font-size: 11px;
    color: #333;
    margin-bottom: 8px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  .vocab-subsection {
    margin-bottom: 10px;

    .subsection-title {
      font-weight: 600;
      font-size: 10px;
      color: #666;
      margin-bottom: 6px;
      padding: 0 4px;
      text-transform: uppercase;
    }

    .vocab-list {
      display: flex;
      flex-wrap: wrap;
      gap: 4px;

      .vocab-item {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        min-width: 24px;
        height: 24px;
        padding: 0 4px;
        background: #f5f7fa;
        border: 1px solid #ddd;
        border-radius: 3px;
        font-size: 11px;
        font-weight: 500;
        font-family: monospace;
        transition: all 0.2s;
        flex-shrink: 0;

        &:hover {
          background: #e3f2fd;
          border-color: #2196f3;
          transform: scale(1.08);
        }
      }
    }
  }
}

.tips {
  flex-shrink: 0;

  p {
    margin: 4px 0;
    font-size: 12px;
    line-height: 1.4;
  }
}

.demo-content {
  width: 100%;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.steps-nav {
  background: white;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;

  :deep(.el-steps) {
    .el-step {
      cursor: pointer;

      &:hover .el-step__head {
        color: #667eea;
      }
    }
  }
}

.steps-content {
  flex: 1;
  background: #f9fafb;
}

.step-content-panel {
  background: white;
  border-radius: 12px;
  padding: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  height: 81vh;
  display: flex;
  flex-direction: column;

  h3 {
    margin: 0 0 20px 0;
    font-size: 18px;
    font-weight: 600;
    color: #1f2937;
    padding-bottom: 12px;
    border-bottom: 2px solid #e5e7eb;
  }
}

// Auto-regressive 面板特殊样式
.autoregressive-panel {
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0;

  .generation-sequence-card {
    flex-shrink: 0;
    margin: 8px 8px 0 8px;
    border-radius: 12px;

    :deep(.el-card__body) {
      padding: 12px;
    }
  }

  .decoder-tabs-container {
    flex: 1;
    overflow: hidden;
    margin: 8px;
    border-radius: 12px;
    min-height: 400px;

    :deep(.el-tabs__header) {
      margin-bottom: 0;
    }

    :deep(.el-tabs__content) {
      height: calc(100% - 40px);
      overflow: hidden;
    }
  }
}

.generation-sequence-card {
  :deep(.el-card__body) {
    padding: 16px;
  }
}

.step-content-panel {
  :deep(.el-form-item) {
    margin-bottom: 16px;
  }

  :deep(.el-form-item__label) {
    font-weight: 600;
    color: #374151;
  }

  .tokenize-display {
    .tokenize-text {
      margin-bottom: 16px;
      padding: 12px;
      background: #f0f9ff;
      border-left: 4px solid #3b82f6;
      border-radius: 4px;
      font-size: 14px;

      strong {
        color: #1e40af;
        margin-right: 8px;
      }
    }

    .tokenize-tokens {
      margin-bottom: 16px;
      padding: 12px;
      background: white;
      border: 1px solid #e5e7eb;
      border-radius: 4px;
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      align-items: center;
      font-size: 14px;

      strong {
        color: #1e40af;
        margin-right: 8px;
        flex-basis: 100%;
      }

      .token-item {
        display: inline-block;
        padding: 6px 12px;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        border-radius: 18px;
        font-size: 13px;
        font-weight: 500;
      }
    }

    .tokenize-info {
      font-size: 13px;
      color: #6b7280;
      text-align: right;
    }
  }

  .transformer-steps-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
    margin-bottom: 24px;
  }

  .transformer-container {
    display: flex;
    gap: 20px;
    height: calc(100vh - 142px);
    min-height: 500px;
  }

  .steps-sidebar {
    width: 280px;
    flex-shrink: 0;
    background: #f9fafb;
    border-radius: 8px;
    overflow: hidden;
    display: flex;
    flex-direction: column;

    .sidebar-header {
      padding: 16px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      font-weight: 600;
      font-size: 14px;
      text-align: center;
    }

    .steps-menu {
      flex: 1;
      overflow-y: auto;
      padding: 8px;

      // 隐藏滚动条但保持滚动功能
      scrollbar-width: thin;
      scrollbar-color: rgba(102, 126, 234, 0.3) transparent;

      &::-webkit-scrollbar {
        width: 6px;
      }

      &::-webkit-scrollbar-track {
        background: transparent;
      }

      &::-webkit-scrollbar-thumb {
        background: rgba(102, 126, 234, 0.3);
        border-radius: 3px;

        &:hover {
          background: rgba(102, 126, 234, 0.5);
        }
      }
    }

    .step-menu-item {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 6px;
      background: white;
      border: 2px solid transparent;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.2s;
      padding: 8px;

      &:hover {
        border-color: #667eea;
        transform: translateX(4px);
      }

      &.active {
        border-color: #667eea;
        background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
        box-shadow: 0 2px 8px rgba(102, 126, 234, 0.2);
      }

      .step-number {
        width: 32px;
        height: 32px;
        flex-shrink: 0;
        display: flex;
        align-items: center;
        justify-content: center;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        border-radius: 50%;
        font-weight: 600;
        font-size: 13px;
      }

      .step-info {
        flex: 1;
        min-width: 0;

        .step-type {
          font-size: 11px;
          color: #667eea;
          font-weight: 600;
          text-transform: uppercase;
          letter-spacing: 0.5px;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .step-title-short {
          font-size: 12px;
          color: #4b5563;
          margin-top: 2px;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
      }
    }
  }

  .step-detail {
    flex: 1;
    overflow-y: auto;
    background: white;
    border-radius: 8px;
    padding: 5px;
    border: 1px solid #e5e7eb;

    // 隐藏滚动条但保持滚动功能
    scrollbar-width: thin;
    scrollbar-color: rgba(102, 126, 234, 0.3) transparent;

    &::-webkit-scrollbar {
      width: 6px;
    }

    &::-webkit-scrollbar-track {
      background: transparent;
    }

    &::-webkit-scrollbar-thumb {
      background: rgba(102, 126, 234, 0.3);
      border-radius: 3px;

      &:hover {
        background: rgba(102, 126, 234, 0.5);
      }
    }
  }

  .completion-banner {
    margin-top: 20px;
  }
}

.input-hint {
  font-size: 12px;
  color: #6b7280;
  margin-top: 6px;
}

.run-button {
  padding: 10px 32px;
  font-size: 15px;
}

.main-tabs {
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  padding: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  :deep(.el-tabs__header) {
    background: #f9fafb;
    border-bottom: 2px solid #e5e7eb;
    border-radius: 12px 12px 0 0;
    flex-shrink: 0;

    .el-tabs__nav-wrap {
      &::after {
        height: 0;
      }
    }

    .el-tabs__item {
      color: #6b7280;
      font-weight: 600;
      font-size: 14px;
      padding: 0 24px;
      height: 50px;
      line-height: 50px;
      border-bottom: 3px solid transparent;
      transition: all 0.3s;

      &:hover {
        color: #3b82f6;
      }

      &.is-active {
        color: #3b82f6;
        border-bottom-color: #3b82f6;
      }
    }
  }

  :deep(.el-tabs__content) {
    padding: 0;
    flex: 1;
    overflow: hidden;

    .el-tab-pane {
      height: 100%;
      display: flex;
      flex-direction: column;
    }
  }
}

.el-tab-pane {
  height: 89vh;
}

.demo-content {
  padding: 0;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow-y: auto;
}

.guide-content {
  padding: 20px;
  background: white;
  border-radius: 8px;
}

// 自回归生成样式
.generation-sequence {
  margin-bottom: 16px;

  h4 {
    margin: 0 0 12px 0;
    font-size: 14px;
    font-weight: 600;
    color: #333;
  }

  .sequence-tokens {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-items: center;

    .token-tag {
      padding: 6px 12px;
      border-radius: 4px;
      font-size: 13px;
      font-weight: 500;
      transition: all 0.3s;

      &.fade-in {
        animation: fadeIn 0.4s ease-in;
      }
    }
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.generation-steps {
  height: 100%;
  overflow-y: auto;
  padding: 0;

  // 隐藏滚动条但保持滚动功能
  scrollbar-width: thin;
  scrollbar-color: rgba(102, 126, 234, 0.3) transparent;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(102, 126, 234, 0.3);
    border-radius: 3px;

    &:hover {
      background: rgba(102, 126, 234, 0.5);
    }
  }

  h4 {
    margin: 0 0 16px 0;
    font-size: 14px;
    font-weight: 600;
    color: #333;
    padding: 12px 12px 0 12px;
  }

  .generation-step-card {
    background: #f9fafb;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 12px;
    margin: 12px;
    margin-top: 0;

    &:first-of-type {
      margin-top: 0;
    }

    .step-header {
      display: flex;
      gap: 16px;
      align-items: center;
      margin-bottom: 12px;
      flex-wrap: wrap;

      .step-num {
        font-weight: 600;
        color: #667eea;
        font-size: 13px;
      }

      .selected-token {
        font-size: 13px;
        color: #4b5563;

        strong {
          color: #667eea;
          font-weight: 600;
        }
      }

      .token-prob {
        font-size: 13px;
        color: #4b5563;
      }
    }

    .top5-bars {
      display: flex;
      flex-direction: column;
      gap: 8px;

      .prob-bar {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 12px;

        .bar-label {
          width: 60px;
          text-align: right;
          font-weight: 500;
          color: #4b5563;
          white-space: nowrap;
        }

        .bar-container {
          flex: 1;
          height: 24px;
          background: #e5e7eb;
          border-radius: 4px;
          overflow: hidden;
          position: relative;

          .bar-fill {
            height: 100%;
            background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
            transition: width 0.3s ease;
          }
        }

        .bar-value {
          width: 50px;
          text-align: right;
          font-weight: 500;
          color: #667eea;
          white-space: nowrap;
        }
      }
    }
  }

  .empty-state {
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 40px 20px;
  }
}

.completion-message {
  margin-top: 16px;
}

.autoregressive-empty {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 8px;
}

.completion-alert {
  flex-shrink: 0;
  margin: 0 8px 8px 8px;
  border-radius: 8px;

  :deep(.el-alert__content) {
    font-size: 12px;
  }

  :deep(.el-alert__title) {
    font-size: 13px;
  }
}

// 统一Decoder和Encoder的容器样式
.decoder-layout {
  display: flex;
  gap: 20px;
  height: calc(100vh - 270px);
  min-height: 500px;
}

.decoder-sidebar {
  width: 280px;
  flex-shrink: 0;
  background: #f9fafb;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;

  .steps-menu {
    flex: 1;
    overflow-y: auto;
    padding: 8px;

    // 隐藏滚动条但保持滚动功能
    scrollbar-width: thin;
    scrollbar-color: rgba(102, 126, 234, 0.3) transparent;

    &::-webkit-scrollbar {
      width: 6px;
    }

    &::-webkit-scrollbar-track {
      background: transparent;
    }

    &::-webkit-scrollbar-thumb {
      background: rgba(102, 126, 234, 0.3);
      border-radius: 3px;

      &:hover {
        background: rgba(102, 126, 234, 0.5);
      }
    }
  }

  .step-menu-item {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 6px;
    background: white;
    border: 2px solid transparent;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.2s;
    padding: 8px;

    &:hover {
      border-color: #667eea;
      transform: translateX(4px);
    }

    &.active {
      border-color: #667eea;
      background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
      box-shadow: 0 2px 8px rgba(102, 126, 234, 0.2);
    }

    .step-number {
      width: 32px;
      height: 32px;
      flex-shrink: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      border-radius: 50%;
      font-weight: 600;
      font-size: 13px;
    }

    .step-info {
      flex: 1;
      min-width: 0;

      .step-type {
        font-size: 11px;
        color: #667eea;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.5px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .step-title-short {
        font-size: 12px;
        color: #4b5563;
        margin-top: 2px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
    }
  }
}

.decoder-detail {
  flex: 1;
  overflow-y: auto;
  background: white;
  border-radius: 8px;
  padding: 5px;
  border: 1px solid #e5e7eb;

  // 隐藏滚动条但保持滚动功能
  scrollbar-width: thin;
  scrollbar-color: rgba(102, 126, 234, 0.3) transparent;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(102, 126, 234, 0.3);
    border-radius: 3px;

    &:hover {
      background: rgba(102, 126, 234, 0.5);
    }
  }
}

.decoder-steps-tab {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  :deep(.el-tabs__content) {
    height: calc(100% - 40px);
    overflow: hidden;
  }

  :deep(.el-tab-pane) {
    height: 100%;
    overflow: hidden;
  }
}

// 响应式设计

@media (max-width: 768px) {
  .transformer-page {
    padding: 12px;
  }

  .guide-content {
    padding: 16px;
  }

  .decoder-layout {
    flex-direction: column;
    height: auto;
  }

  .decoder-sidebar {
    width: 100%;
    max-height: 200px;
  }
}
</style>
