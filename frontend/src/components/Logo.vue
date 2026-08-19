<script setup>
// 색은 currentColor 를 따르므로 부모에서 color 만 지정하면 된다.
// 실밥만 --logo-seam 으로 따로 둔다.
defineProps({ size: { type: Number, default: 28 }, withText: { type: Boolean, default: false } })
</script>

<template>
  <svg v-if="!withText" :width="size" :height="size" viewBox="0 0 64 64" class="logo" aria-label="오늘의직관">
    <path fill="currentColor" fill-rule="evenodd" d="M8 14h48a4 4 0 0 1 4 4v7.5a6.5 6.5 0 0 0 0 13V46a4 4 0 0 1-4 4H8a4 4 0 0 1-4-4v-7.5a6.5 6.5 0 0 0 0-13V18a4 4 0 0 1 4-4Z M23.4 21.6a2 2 0 0 0-2.8 2.8c3.9 3.9 3.9 11.2 0 15.2a2 2 0 1 0 2.8 2.8c5.4-5.4 5.4-15.4 0-20.8Z M43.4 21.6c-5.4 5.4-5.4 15.4 0 20.8a2 2 0 0 0 2.8-2.8c-3.9-4-3.9-11.3 0-15.2a2 2 0 0 0-2.8-2.8Z"/>
  </svg>

  <svg v-else :height="size" :width="size * 3.9" viewBox="0 0 250 64" class="logo" aria-label="오늘의직관">
    <path fill="currentColor" fill-rule="evenodd" d="M4 14h52a4 4 0 0 1 4 4v7.5a6.5 6.5 0 0 0 0 13V46a4 4 0 0 1-4 4H4a4 4 0 0 1-4-4v-7.5a6.5 6.5 0 0 0 0-13V18a4 4 0 0 1 4-4Z M21.4 21.6a2 2 0 0 0-2.8 2.8c3.9 3.9 3.9 11.2 0 15.2a2 2 0 1 0 2.8 2.8c5.4-5.4 5.4-15.4 0-20.8Z M41.4 21.6c-5.4 5.4-5.4 15.4 0 20.8a2 2 0 0 0 2.8-2.8c-3.9-4-3.9-11.3 0-15.2a2 2 0 0 0-2.8-2.8Z"/>
    <text x="72" y="41" font-size="26" font-weight="800" letter-spacing="-0.5" fill="currentColor">오늘의직관</text>
  </svg>
</template>

<style scoped>
.logo { display: block; flex: none; }
text { font-family: inherit; }
</style>
