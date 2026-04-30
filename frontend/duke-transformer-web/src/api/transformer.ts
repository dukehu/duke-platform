import request from '@/utils/request';

/**
 * Transformer API 接口定义和 SSE 客户端
 */

export interface MatrixData {
  label: string;
  values: number[][];
  minVal: number;
  maxVal: number;
  rowLabels: string[];
  colLabels: string[];
  colorScheme: string;
}

export interface TransformerStep {
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

export interface TokenizeResult {
  tokens: string[];
  tokenIds: number[];
  unknownTokens: string[];
}

export interface TransformerRunParams {
  text: string;
  embeddingDim?: number;
  numHeads?: number;
  numLayers?: number;
}

/**
 * 获取学习文档
 */
export function getGuide(): Promise<string> {
  return request.get('/transformer/guide');
}

/**
 * 获取默认配置
 */
export function getConfig(): Promise<any> {
  return request.get('/transformer/config');
}

/**
 * 获取词表列表
 */
export function getVocabList(): Promise<string[]> {
  return request.get('/transformer/vocab-list');
}

/**
 * 获取词表
 */
export function getVocab(): Promise<any> {
  return request.get('/transformer/vocab');
}

/**
 * 分词演示：返回词、token ID、未知词
 */
export function tokenize(text: string): Promise<TokenizeResult> {
  return request.get('/transformer/tokenize', { params: { text } });
}

/**
 * 创建 SSE 连接，流式接收 Transformer 计算步骤
 *
 * 使用原生 EventSource API（无法自定义请求头，所以不需要传 token）
 * SSE 接口已在网关白名单中，无需认证
 */
export interface TransformerRunWithTokensParams {
  tokens: string[];
  embeddingDim?: number;
  numHeads?: number;
  numLayers?: number;
}

export interface Seq2SeqRunParams {
  srcText: string;  // 源文本
  tgtText: string;  // 目标文本
  embeddingDim?: number;
  numHeads?: number;
  numLayers?: number;
}

export interface EncoderRunParams {
  srcText: string;
  embeddingDim?: number;
  numHeads?: number;
  numLayers?: number;
}

export interface DecoderRunParams {
  tgtText: string;
  encoderOutputJson: string;   // encoder_complete 事件返回的 encoderOutput JSON
  srcTokenIdsJson: string;     // encoder_complete 事件返回的 srcTokenIds JSON
  embeddingDim?: number;
  numHeads?: number;
  numLayers?: number;
}

export interface AutoRegressiveParams {
  encoderOutputJson: string;
  srcTokenIdsJson: string;
  embeddingDim?: number;
  numHeads?: number;
  numLayers?: number;
}

export function createTransformerSSE(
  params: TransformerRunWithTokensParams,
  onStep: (step: TransformerStep) => void,
  onComplete: () => void,
  onError: (error: Event) => void
): EventSource {
  // 构建查询参数：传递 token 数组
  const queryParams = new URLSearchParams({
    tokens: JSON.stringify(params.tokens),
    embeddingDim: String(params.embeddingDim ?? 16),
    numHeads: String(params.numHeads ?? 2),
    numLayers: String(params.numLayers ?? 1)
  });

  // 直连后端 SSE 接口（绕过网关，因为 EventSource 无法正确处理网关的流式转发）
  const url = `http://localhost:8082/transformer/run?${queryParams}`;

  // 创建 EventSource
  const eventSource = new EventSource(url);

  // 监听 step 事件
  eventSource.addEventListener('step', (event: MessageEvent) => {
    try {
      const step = JSON.parse(event.data) as TransformerStep;
      onStep(step);
    } catch (e) {
      console.error('解析步骤数据失败', e);
    }
  });

  // 监听 complete 事件
  eventSource.addEventListener('complete', () => {
    eventSource.close();
    onComplete();
  });

  // 错误处理
  eventSource.onerror = (error: Event) => {
    eventSource.close();
    onError(error);
  };

  return eventSource;
}

export function createSeq2SeqSSE(
  params: Seq2SeqRunParams,
  onStep: (step: TransformerStep) => void,
  onComplete: () => void,
  onError: (error: Event) => void
): EventSource {
  // 临时使用 srcTokensJson/tgtTokensJson 参数格式，但内容是文本而不是token数组
  // 后端会识别这是文本并使用 encode() 方法分词
  const queryParams = new URLSearchParams({
    srcTokensJson: JSON.stringify([params.srcText]),
    tgtTokensJson: JSON.stringify([params.tgtText]),
    embeddingDim: String(params.embeddingDim ?? 16),
    numHeads: String(params.numHeads ?? 2),
    numLayers: String(params.numLayers ?? 1),
    useTextEncoding: 'true'  // 标记后端使用文本编码
  });

  const url = `http://localhost:8082/transformer/run-seq2seq?${queryParams}`;
  const eventSource = new EventSource(url);

  eventSource.addEventListener('step', (event: MessageEvent) => {
    try {
      const step = JSON.parse(event.data) as TransformerStep;
      onStep(step);
    } catch (e) {
      console.error('解析步骤数据失败', e);
    }
  });

  eventSource.addEventListener('complete', () => {
    eventSource.close();
    onComplete();
  });

  eventSource.onerror = (error: Event) => {
    eventSource.close();
    onError(error);
  };

  return eventSource;
}

export function createEncoderSSE(
  params: EncoderRunParams,
  onStep: (step: TransformerStep) => void,
  onEncoderComplete: (encoderOutputJson: string, srcTokenIdsJson: string) => void,
  onError: (error: Event) => void
): EventSource {
  const queryParams = new URLSearchParams({
    srcText: params.srcText,
    embeddingDim: String(params.embeddingDim ?? 16),
    numHeads: String(params.numHeads ?? 2),
    numLayers: String(params.numLayers ?? 1)
  });

  const url = `http://localhost:8082/transformer/run-encoder?${queryParams}`;
  const eventSource = new EventSource(url);

  eventSource.addEventListener('step', (event: MessageEvent) => {
    try {
      const step = JSON.parse(event.data) as TransformerStep;
      onStep(step);
    } catch (e) {
      console.error('解析 Encoder 步骤数据失败', e);
    }
  });

  eventSource.addEventListener('encoder_complete', (event: MessageEvent) => {
    try {
      const data = JSON.parse(event.data);
      const encoderOutputJson = JSON.stringify(data.encoderOutput);
      const srcTokenIdsJson = JSON.stringify(data.srcTokenIds);
      onEncoderComplete(encoderOutputJson, srcTokenIdsJson);
    } catch (e) {
      console.error('解析 encoder_complete 事件失败', e);
    }
    eventSource.close();
  });

  eventSource.onerror = (error: Event) => {
    eventSource.close();
    onError(error);
  };

  return eventSource;
}

export function createDecoderSSE(
  params: DecoderRunParams,
  onStep: (step: TransformerStep) => void,
  onComplete: () => void,
  onError: (error: Event) => void
): EventSource {
  const queryParams = new URLSearchParams({
    tgtText: params.tgtText,
    encoderOutputJson: params.encoderOutputJson,
    srcTokenIdsJson: params.srcTokenIdsJson,
    embeddingDim: String(params.embeddingDim ?? 16),
    numHeads: String(params.numHeads ?? 2),
    numLayers: String(params.numLayers ?? 1)
  });

  const url = `http://localhost:8082/transformer/run-decoder?${queryParams}`;
  const eventSource = new EventSource(url);

  eventSource.addEventListener('step', (event: MessageEvent) => {
    try {
      const step = JSON.parse(event.data) as TransformerStep;
      onStep(step);
    } catch (e) {
      console.error('解析 Decoder 步骤数据失败', e);
    }
  });

  eventSource.addEventListener('complete', () => {
    eventSource.close();
    onComplete();
  });

  eventSource.onerror = (error: Event) => {
    eventSource.close();
    onError(error);
  };

  return eventSource;
}

export function createAutoRegressiveSSE(
  params: AutoRegressiveParams,
  onStep: (step: TransformerStep) => void,
  onComplete: () => void,
  onError: (error: Event) => void
): EventSource {
  const queryParams = new URLSearchParams({
    encoderOutputJson: params.encoderOutputJson,
    srcTokenIdsJson: params.srcTokenIdsJson,
    embeddingDim: String(params.embeddingDim ?? 16),
    numHeads: String(params.numHeads ?? 2),
    numLayers: String(params.numLayers ?? 1)
  });

  const url = `http://localhost:8082/transformer/run-autoregressive?${queryParams}`;
  const eventSource = new EventSource(url);

  eventSource.addEventListener('step', (event: MessageEvent) => {
    try {
      const step = JSON.parse(event.data) as TransformerStep;
      onStep(step);
    } catch (e) {
      console.error('解析自回归步骤数据失败', e);
    }
  });

  eventSource.addEventListener('complete', () => {
    eventSource.close();
    onComplete();
  });

  eventSource.onerror = (error: Event) => {
    eventSource.close();
    onError(error);
  };

  return eventSource;
}
