<template>
  <div class="markdown-viewer">
    <div class="md-body" v-html="rendered"></div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { marked } from 'marked';
import hljs from 'highlight.js';
import 'highlight.js/styles/github.css';

interface Props {
  content: string;
}

const props = defineProps<Props>();

marked.setOptions({
  breaks: true,
  gfm: true
});

const renderer = new marked.Renderer();
renderer.code = ({ text, lang }) => {
  const highlighted = lang ? hljs.highlight(text, { language: lang, ignoreIllegals: true }).value : hljs.highlightAuto(text).value;
  return `<pre><code class="hljs ${lang}">${highlighted}</code></pre>`;
};

marked.setOptions({ renderer });

const rendered = computed(() => {
  try {
    return marked(props.content) as string;
  } catch (e) {
    console.error('Markdown 渲染失败', e);
    return '<p>文档渲染失败</p>';
  }
});
</script>

<style scoped lang="scss">
.markdown-viewer {
  width: 100%;
  padding: 20px;
  background: white;
}

.md-body {
  font-size: 14px;
  line-height: 1.6;
  color: #333;

  h1, h2, h3, h4, h5, h6 {
    margin: 24px 0 16px 0;
    font-weight: 600;
    color: #1f2937;
  }

  h1 {
    font-size: 32px;
    border-bottom: 2px solid #3b82f6;
    padding-bottom: 12px;
  }

  h2 {
    font-size: 24px;
    border-left: 4px solid #3b82f6;
    padding-left: 12px;
  }

  h3 {
    font-size: 20px;
  }

  p {
    margin: 16px 0;
  }

  code {
    background: #f3f4f6;
    padding: 2px 6px;
    border-radius: 3px;
    font-family: 'Courier New', monospace;
    font-size: 13px;
    color: #d97706;
  }

  pre {
    background: #1f2937;
    color: #e5e7eb;
    padding: 16px;
    border-radius: 6px;
    overflow-x: auto;
    margin: 16px 0;
    line-height: 1.4;

    code {
      background: none;
      padding: 0;
      color: inherit;
      font-size: 12px;
    }
  }

  blockquote {
    border-left: 4px solid #d1d5db;
    padding-left: 16px;
    margin: 16px 0;
    color: #6b7280;
    font-style: italic;
  }

  a {
    color: #3b82f6;
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }

  ul, ol {
    margin: 16px 0;
    padding-left: 32px;

    li {
      margin: 8px 0;
    }
  }

  table {
    border-collapse: collapse;
    width: 100%;
    margin: 16px 0;

    th, td {
      border: 1px solid #d1d5db;
      padding: 12px;
      text-align: left;
    }

    th {
      background: #f3f4f6;
      font-weight: 600;
    }
  }

  strong {
    font-weight: 600;
    color: #1f2937;
  }

  em {
    font-style: italic;
    color: #6b7280;
  }
}

:deep(.hljs) {
  background: #1f2937 !important;
  color: #e5e7eb !important;
  padding: 16px !important;
  border-radius: 6px;
}
</style>
