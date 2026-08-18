<script setup>
import { computed } from 'vue'
import { teamColor } from '../lib/team'

const props = defineProps({
  name: { type: String, required: true },
  size: { type: String, default: 'md' },   // sm | md | lg
  dim: { type: Boolean, default: false },
})
const c = computed(() => teamColor(props.name))
</script>

<template>
  <span class="tm" :class="[size, { dim }]"
        :style="{ background: c.bg, color: c.fg }">{{ name }}</span>
</template>

<style scoped>
.tm {
  display: inline-flex; align-items: center; justify-content: center;
  border-radius: 7px; font-weight: 800; letter-spacing: -0.02em;
  white-space: nowrap; transition: opacity .15s;
}
.sm { padding: 2px 6px; font-size: 11px; border-radius: 6px; }
.md { padding: 4px 9px; font-size: 13px; }
.lg { padding: 6px 12px; font-size: 15px; }
.dim { opacity: .38; }
</style>
