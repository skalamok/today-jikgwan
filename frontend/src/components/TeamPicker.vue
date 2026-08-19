<script setup>
import { ref, onMounted } from 'vue'
import client from '../api/client'
import TeamMark from './TeamMark.vue'

const props = defineProps({
  modelValue: { type: [Number, String, null], default: null },
  clearable: { type: Boolean, default: true },
})
const emit = defineEmits(['update:modelValue'])

const teams = ref([])

onMounted(async () => {
  const { data } = await client.get('/teams')
  teams.value = data
})

function pick(id) {
  emit('update:modelValue', props.clearable && props.modelValue === id ? null : id)
}
</script>

<template>
  <div class="picker">
    <button v-for="t in teams" :key="t.id" type="button" class="pick"
            :class="{ on: modelValue === t.id }" @click="pick(t.id)">
      <TeamMark :name="t.shortName" size="md" :dim="modelValue !== null && modelValue !== t.id" />
      <span>{{ t.shortName }}</span>
    </button>
  </div>
</template>

<style scoped>
.picker { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 8px; }
.pick {
  display: flex; flex-direction: column; align-items: center; gap: 6px;
  padding: 10px 4px; border: 1px solid var(--line); border-radius: 10px;
  background: var(--card); font-size: 12px; color: var(--muted); cursor: pointer;
  transition: border-color .15s ease, color .15s ease;
}
.pick.on { border-color: var(--accent); color: var(--text); font-weight: 700; }
@media (max-width: 380px) { .picker { grid-template-columns: repeat(4, minmax(0, 1fr)); } }
</style>
