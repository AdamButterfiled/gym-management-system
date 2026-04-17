<template>
  <span class="windows-hello-indicator" aria-hidden="true">
    <img
      v-if="!reducedMotion"
      class="hello-image hello-animated"
      :class="{ 'is-hidden': showStaticSmile }"
      :src="helloSequenceUrl"
      alt=""
      @load="handleAnimatedLoad"
    />
    <img
      class="hello-image"
      :class="['hello-static', { 'is-visible': reducedMotion || showStaticSmile }]"
      :src="helloStaticUrl"
      alt=""
    />
  </span>
</template>

<script setup>
import { onBeforeUnmount, ref, watch } from 'vue';
import helloSequenceUrl from '@/assets/windows-hello/hello-sequence.webp';
import helloStaticUrl from '@/assets/windows-hello/hello-static.png';

const FREEZE_ON_SMILE_MS = 1960;

const props = defineProps({
  reducedMotion: {
    type: Boolean,
    default: false
  }
});

const showStaticSmile = ref(props.reducedMotion);
let freezeTimer = 0;

function clearFreezeTimer() {
  if (freezeTimer) {
    window.clearTimeout(freezeTimer);
    freezeTimer = 0;
  }
}

function freezeOnSmileFrame() {
  clearFreezeTimer();
  showStaticSmile.value = true;
}

function restartAnimation(useReducedMotion) {
  clearFreezeTimer();
  showStaticSmile.value = useReducedMotion;
}

function handleAnimatedLoad() {
  if (props.reducedMotion) {
    return;
  }

  clearFreezeTimer();
  showStaticSmile.value = false;
  freezeTimer = window.setTimeout(freezeOnSmileFrame, FREEZE_ON_SMILE_MS);
}

watch(
  () => props.reducedMotion,
  (value) => {
    restartAnimation(value);
  }
);

onBeforeUnmount(() => {
  clearFreezeTimer();
});
</script>

<style scoped>
.windows-hello-indicator {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 22px;
  height: 22px;
}

.hello-image {
  position: absolute;
  inset: 0;
  width: 22px;
  height: 22px;
  display: block;
  user-select: none;
  pointer-events: none;
  image-rendering: auto;
}

.hello-animated,
.hello-static {
  transition: opacity 0.08s linear;
}

.hello-animated {
  opacity: 1;
}

.hello-animated.is-hidden {
  opacity: 0;
}

.hello-static {
  opacity: 0;
}

.hello-static.is-visible {
  opacity: 1;
}
</style>
