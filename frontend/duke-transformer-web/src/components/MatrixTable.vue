<template>
  <div class="matrix-table-wrapper">
    <div v-if="matrixData" class="matrix-container">
      <div class="matrix-label">{{ matrixData.label }}</div>
      <div class="matrix-scroll">
        <table class="matrix-table">
          <thead v-if="matrixData.colLabels && matrixData.colLabels.length > 0">
            <tr>
              <th class="row-header-cell"></th>
              <th v-for="(col, idx) in matrixData.colLabels" :key="idx" class="col-header">
                {{ col }}
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, rowIdx) in matrixData.values" :key="rowIdx">
              <td v-if="matrixData.rowLabels" class="row-header">
                {{ matrixData.rowLabels[rowIdx] }}
              </td>
              <td
                v-for="(val, colIdx) in row"
                :key="colIdx"
                class="matrix-cell"
                :style="getCellStyle(val)"
                :title="`[${rowIdx}, ${colIdx}] = ${val.toFixed(4)}`"
              >
                <span class="cell-value">{{ showValues ? val.toFixed(3) : '' }}</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="matrix-stats">
        <span>最小值: {{ matrixData.minVal.toFixed(4) }}</span>
        <span>最大值: {{ matrixData.maxVal.toFixed(4) }}</span>
      </div>
    </div>
    <div v-else class="no-matrix">无矩阵数据</div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useHeatmap } from '@/composables/useHeatmap';

interface MatrixData {
  label: string;
  values: number[][];
  minVal: number;
  maxVal: number;
  rowLabels: string[];
  colLabels: string[];
  colorScheme: string;
}

const props = defineProps<{
  matrixData: MatrixData;
}>();

const showValues = ref(true);
const { getColor } = useHeatmap();

function getCellStyle(value: number) {
  const colorScheme = props.matrixData.colorScheme || 'blue';
  const minVal = props.matrixData.minVal;
  const maxVal = props.matrixData.maxVal;

  const { bgColor, textColor } = getColor(value, minVal, maxVal, colorScheme);

  return {
    backgroundColor: bgColor,
    color: textColor
  };
}
</script>

<style scoped lang="scss">
.matrix-table-wrapper {
  margin: 12px 0;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  overflow: hidden;
}

.matrix-container {
  padding: 12px;
}

.matrix-label {
  font-weight: 600;
  font-size: 13px;
  color: #1f2937;
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.matrix-scroll {
  overflow-x: auto;
  overflow-y: auto;
  max-height: 400px;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  margin-bottom: 8px;
}

.matrix-table {
  border-collapse: collapse;
  width: 100%;
  min-width: 300px;
  font-size: 11px;

  thead {
    background: #f9fafb;
    position: sticky;
    top: 0;
    z-index: 10;
  }

  th {
    padding: 6px 8px;
    text-align: center;
    font-weight: 600;
    color: #374151;
    border: 1px solid #e5e7eb;
    background: #f9fafb;
    min-width: 36px;
  }

  td {
    padding: 6px 8px;
    text-align: center;
    border: 1px solid #e5e7eb;
    min-width: 36px;
    height: 32px;
    position: relative;
    cursor: help;
    transition: all 0.2s;

    &:hover {
      transform: scale(1.05);
      z-index: 5;
      box-shadow: 0 0 8px rgba(0, 0, 0, 0.15);
    }
  }

  .row-header-cell {
    background: #f9fafb;
    font-weight: 600;
    color: #374151;
  }

  .row-header {
    background: #f9fafb;
    font-weight: 600;
    color: #374151;
    text-align: center;
    padding: 6px 8px;
  }

  .matrix-cell {
    display: table-cell;
    vertical-align: middle;
  }
}

.cell-value {
  display: inline-block;
  font-family: 'Courier New', monospace;
  font-weight: 500;
  font-size: 10px;
}

.matrix-stats {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #6b7280;
  padding: 8px;
  background: #f9fafb;
  border-radius: 4px;

  span {
    font-family: 'Courier New', monospace;
  }
}

.no-matrix {
  padding: 24px;
  text-align: center;
  color: #9ca3af;
  font-size: 12px;
}
</style>
