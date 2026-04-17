<template>
  <div
    class="polar-bear-mascot"
    :class="[
      `mode-${mode}`,
      { 'is-reduced-motion': reducedMotion }
    ]"
  >
    <div class="scene">
      <div
        class="bear-layer"
        :class="{ 'is-clickable': mode === 'idle' }"
        @click="handleIdleInteract"
        @dblclick.stop="handleBearDoubleClick"
      >
        <div
          v-if="mode === 'success'"
          class="success-sparkles"
          aria-hidden="true"
        >
          <span class="success-star star-a"></span>
          <span class="success-star star-b"></span>
          <span class="success-star star-c"></span>
          <span class="success-star star-d"></span>
          <span class="success-star star-e"></span>
        </div>
        <div
          v-if="mode === 'intro'"
          class="intro-sequence-wrap"
          :style="introMotionStyle"
        >
          <img
            :src="currentModeAsset"
            alt="polar bear intro animation frame"
            class="bear-asset sequence-asset"
          />
        </div>
        <div
          v-else-if="activeModeKey === 'idle'"
          class="idle-static-wrap"
        >
          <img
            :src="idleOpenAsset"
            :alt="currentModeAlt"
            class="bear-asset sequence-asset idle-base-asset"
          />
          <img
            v-show="idleBlinkVisible"
            :src="idleBlinkOverlayAsset"
            alt=""
            aria-hidden="true"
            class="bear-asset sequence-asset idle-blink-overlay"
          />
        </div>
        <div
          v-else
          class="sequence-frame-wrap"
        >
          <img
            :src="currentModeAsset"
            :alt="currentModeAlt"
            class="bear-asset sequence-asset"
          />
          <svg
            v-if="activeModeKey === 'password' && currentSequenceFrameIndex >= PASSWORD_TRANSITION_END_FRAME"
            class="password-foot-arc"
            viewBox="0 0 570 592"
            aria-hidden="true"
          >
            <path d="M 160 516 C 175 528 205 528 226 514" />
          </svg>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import coverPeekAsset from '@/assets/bear/generated-cv/transparent-svg/cover-peek.svg';
import errorSadAsset from '@/assets/bear/generated-cv/transparent-svg/error-sad.svg';
import idleBlinkOverlayAsset from '@/assets/bear/generated-cv/transparent-svg/idle-eye-blink-overlay.svg';
import idleOpenAsset from '@/assets/bear/generated-cv/transparent-svg/idle-open.svg';
import successLiftAsset from '@/assets/bear/generated-cv/transparent-svg/success-lift.svg';
import sequenceManifest from '@/assets/bear/generated-cv/video-sequences/manifest.json';

function loadSequence(modules) {
  return Object.entries(modules)
    .sort(([leftKey], [rightKey]) => leftKey.localeCompare(rightKey, undefined, { numeric: true }))
    .map(([, value]) => value);
}

const introFrameSources = loadSequence(
  import.meta.glob('../assets/bear/generated-cv/video-sequences/intro/frame-*.svg', { eager: true, import: 'default' })
);
const idleInteractFrameSources = loadSequence(
  import.meta.glob('../assets/bear/generated-cv/video-sequences/idleInteract/frame-*.svg', { eager: true, import: 'default' })
);
const passwordFrameSources = loadSequence(
  import.meta.glob('../assets/bear/generated-cv/video-sequences/password/frame-*.svg', { eager: true, import: 'default' })
);
const errorFrameSources = loadSequence(
  import.meta.glob('../assets/bear/generated-cv/video-sequences/error/frame-*.svg', { eager: true, import: 'default' })
);
const successFrameSources = loadSequence(
  import.meta.glob('../assets/bear/generated-cv/video-sequences/success/frame-*.svg', { eager: true, import: 'default' })
);

const sequenceSourcesByMode = {
  intro: introFrameSources,
  idleInteract: idleInteractFrameSources,
  password: passwordFrameSources,
  error: errorFrameSources,
  success: successFrameSources
};

const fallbackAssetByMode = {
  intro: introFrameSources[introFrameSources.length - 1] || idleOpenAsset,
  idle: idleOpenAsset,
  password: passwordFrameSources[0] || coverPeekAsset,
  error: errorSadAsset,
  success: successLiftAsset
};

const props = defineProps({
  mode: {
    type: String,
    default: 'idle'
  },
  pointer: {
    type: Object,
    default: null
  },
  reducedMotion: {
    type: Boolean,
    default: false
  },
  idleInteractTick: {
    type: Number,
    default: 0
  },
  interruptTick: {
    type: Number,
    default: 0
  }
});

const emit = defineEmits(['intro-complete', 'success-cycle-complete', 'error-cycle-complete', 'double-click']);

const currentSequenceFrameIndex = ref(0);
const idleInteractActive = ref(false);
const idleBlinkVisible = ref(false);

const activeModeKey = computed(() => {
  if (props.mode === 'idle' && idleInteractActive.value) {
    return 'idleInteract';
  }

  return props.mode;
});

const currentSequenceSources = computed(() => sequenceSourcesByMode[activeModeKey.value] || []);

const currentModeAsset = computed(() => {
  const sources = currentSequenceSources.value;
  if (sources.length) {
    const clampedIndex = Math.min(currentSequenceFrameIndex.value, sources.length - 1);
    return sources[clampedIndex];
  }

  return fallbackAssetByMode[activeModeKey.value] || fallbackAssetByMode[props.mode] || idleOpenAsset;
});

const introMotionStyle = computed(() => ({
  '--intro-duration': `${Math.max(currentSequenceDurationMs.value, 160)}ms`
}));

const currentModeAlt = computed(() => {
  if (props.mode === 'idle') return 'polar bear idle animation frame';
  if (activeModeKey.value === 'idleInteract') return 'polar bear idle interaction animation frame';
  if (props.mode === 'password') return 'polar bear password animation frame';
  if (props.mode === 'error') return 'polar bear error animation frame';
  if (props.mode === 'success') return 'polar bear success animation frame';
  return 'polar bear animation frame';
});

const currentSequenceDurationMs = computed(() => {
  const meta = sequenceManifest.sequences?.[activeModeKey.value];
  const frameCount = Number(meta?.frameCount || currentSequenceSources.value.length || 0);
  const fps = Number(meta?.fps || 24);
  const duration = frameCount > 0 ? (frameCount / Math.max(fps, 1)) * 1000 : 0;
  return activeModeKey.value === 'intro'
    ? duration / INTRO_SPEED_MULTIPLIER
    : duration;
});

let sequenceTimer = 0;
let completeTimer = 0;
let idlePauseTimer = 0;

const INTRO_SPEED_MULTIPLIER = 1.35;
const PASSWORD_TRANSITION_END_FRAME = 18;
const PASSWORD_LOOP_TAIL_LENGTH = 10;

function clearTimer(timerId) {
  if (timerId) {
    window.clearTimeout(timerId);
  }
}

function clearAllTimers() {
  clearTimer(sequenceTimer);
  clearTimer(completeTimer);
  clearTimer(idlePauseTimer);
}

function nextIdleBlinkDelay() {
  return 2800 + Math.random() * 1400;
}

function playIdleSequence() {
  clearAllTimers();
  idleInteractActive.value = false;
  idleBlinkVisible.value = false;
  currentSequenceFrameIndex.value = 0;

  if (props.reducedMotion) {
    return;
  }
  const blinkDuration = 120;

  const scheduleNextBlink = () => {
    idlePauseTimer = window.setTimeout(() => {
      if (props.mode !== 'idle' || idleInteractActive.value) {
        return;
      }

      idleBlinkVisible.value = true;
      sequenceTimer = window.setTimeout(() => {
        idleBlinkVisible.value = false;

        if (props.mode === 'idle' && !idleInteractActive.value) {
          scheduleNextBlink();
        }
      }, blinkDuration);
    }, nextIdleBlinkDelay());
  };

  scheduleNextBlink();
}

function playIdleInteractSequence() {
  clearAllTimers();
  idleInteractActive.value = true;
  idleBlinkVisible.value = false;
  currentSequenceFrameIndex.value = 0;

  const sources = sequenceSourcesByMode.idleInteract;
  if (!sources?.length) {
    idleInteractActive.value = false;
    playIdleSequence();
    return;
  }

  if (props.reducedMotion) {
    idleInteractActive.value = false;
    currentSequenceFrameIndex.value = 0;
    return;
  }

  const fps = Number(sequenceManifest.sequences?.idleInteract?.fps || 24);
  const frameDuration = 1000 / Math.max(fps, 1);

  const advanceFrame = () => {
    if (props.mode !== 'idle' || !idleInteractActive.value) {
      return;
    }

    if (currentSequenceFrameIndex.value >= sources.length - 1) {
      idleInteractActive.value = false;
      playIdleSequence();
      return;
    }

    currentSequenceFrameIndex.value += 1;
    sequenceTimer = window.setTimeout(advanceFrame, frameDuration);
  };

  sequenceTimer = window.setTimeout(advanceFrame, frameDuration);
}

function handleIdleInteract() {
  if (props.mode !== 'idle' || idleInteractActive.value || props.reducedMotion) {
    return;
  }

  if (!sequenceSourcesByMode.idleInteract?.length) {
    return;
  }

  playIdleInteractSequence();
}

function handleBearDoubleClick() {
  emit('double-click');
}

function playPasswordSequence() {
  clearAllTimers();
  idleBlinkVisible.value = false;
  currentSequenceFrameIndex.value = 0;

  const sources = sequenceSourcesByMode.password;
  if (!sources?.length) {
    return;
  }

  const transitionEnd = Math.min(PASSWORD_TRANSITION_END_FRAME, sources.length - 1);
  const loopStart = Math.min(
    Math.max(transitionEnd, sources.length - PASSWORD_LOOP_TAIL_LENGTH),
    sources.length - 1
  );
  const loopEnd = sources.length - 1;

  if (props.reducedMotion) {
    currentSequenceFrameIndex.value = loopStart;
    return;
  }

  const fps = Number(sequenceManifest.sequences?.password?.fps || 24);
  const frameDuration = 1000 / Math.max(fps, 1);

  const advanceFrame = () => {
    if (props.mode !== 'password') {
      return;
    }

    if (currentSequenceFrameIndex.value < transitionEnd) {
      currentSequenceFrameIndex.value += 1;
      sequenceTimer = window.setTimeout(advanceFrame, frameDuration);
      return;
    }

    if (loopStart >= loopEnd) {
      currentSequenceFrameIndex.value = loopStart;
      sequenceTimer = window.setTimeout(advanceFrame, frameDuration);
      return;
    }

    if (currentSequenceFrameIndex.value < loopStart || currentSequenceFrameIndex.value >= loopEnd) {
      currentSequenceFrameIndex.value = loopStart;
    } else {
      currentSequenceFrameIndex.value += 1;
    }

    sequenceTimer = window.setTimeout(advanceFrame, frameDuration);
  };

  sequenceTimer = window.setTimeout(advanceFrame, frameDuration);
}

function emitSequenceComplete(mode) {
  if (mode === 'intro') {
    emit('intro-complete');
  }
  if (mode === 'success') {
    emit('success-cycle-complete');
  }
  if (mode === 'error') {
    emit('error-cycle-complete');
  }
}

function playSequence(mode) {
  clearAllTimers();
  idleBlinkVisible.value = false;
  currentSequenceFrameIndex.value = 0;

  const sources = sequenceSourcesByMode[mode];
  if (!sources?.length) {
    if (mode === 'intro' || mode === 'success' || mode === 'error') {
      completeTimer = window.setTimeout(() => emitSequenceComplete(mode), 120);
    }
    return;
  }

  if (props.reducedMotion) {
    currentSequenceFrameIndex.value = mode === 'idle' ? 0 : sources.length - 1;
    if (mode === 'intro' || mode === 'success' || mode === 'error') {
      completeTimer = window.setTimeout(() => emitSequenceComplete(mode), 160);
    }
    return;
  }

  const fps = Number(sequenceManifest.sequences?.[mode]?.fps || 24);
  const playbackRate = mode === 'intro' ? INTRO_SPEED_MULTIPLIER : 1;
  const frameDuration = (1000 / Math.max(fps, 1)) / playbackRate;

  const advanceFrame = () => {
    if (props.mode !== mode) {
      return;
    }

    if (currentSequenceFrameIndex.value >= sources.length - 1) {
      emitSequenceComplete(mode);
      return;
    }

    currentSequenceFrameIndex.value += 1;
    sequenceTimer = window.setTimeout(advanceFrame, frameDuration);
  };

  sequenceTimer = window.setTimeout(advanceFrame, frameDuration);
}

watch(
  () => props.idleInteractTick,
  (tick, previousTick) => {
    if (tick === previousTick) {
      return;
    }

    handleIdleInteract();
  }
);

watch(
  [() => props.mode, () => props.interruptTick],
  ([mode]) => {
    clearAllTimers();
    idleInteractActive.value = false;
    idleBlinkVisible.value = false;

    if (mode === 'password') {
      playPasswordSequence();
      return;
    }

    if (mode === 'idle') {
      playIdleSequence();
      return;
    }

    playSequence(mode);
  },
  { immediate: true }
);

onBeforeUnmount(() => {
  clearAllTimers();
});
</script>

<style scoped>
.polar-bear-mascot {
  --success-star-core: rgba(20, 20, 20, 0.96);
  --success-star-halo: rgba(20, 20, 20, 0.26);
  width: min(680px, 100%);
  height: min(680px, 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.scene {
  position: relative;
  width: min(620px, 100%);
  height: min(620px, 100%);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  transform: translateY(-46px);
}

.bear-layer {
  position: relative;
  width: 100%;
  height: 100%;
  padding-bottom: 52px;
  box-sizing: border-box;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.bear-layer.is-clickable {
  cursor: pointer;
}

.success-sparkles {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.success-star {
  position: absolute;
  width: 18px;
  height: 18px;
  background: radial-gradient(circle, var(--success-star-core) 0 22%, var(--success-star-core) 23% 52%, rgba(255, 255, 255, 0) 53%);
  clip-path: polygon(50% 0%, 61% 37%, 100% 50%, 61% 63%, 50% 100%, 39% 63%, 0% 50%, 39% 37%);
  filter: drop-shadow(0 0 12px var(--success-star-halo));
  opacity: 0;
  transform: scale(0.45) rotate(0deg);
  animation: success-star-pop 1.18s ease-in-out infinite;
}

.star-a {
  left: 24%;
  top: 18%;
  animation-delay: 0s;
}

.star-b {
  left: 34%;
  top: 10%;
  width: 13px;
  height: 13px;
  animation-delay: 0.22s;
}

.star-c {
  right: 24%;
  top: 13%;
  width: 20px;
  height: 20px;
  animation-delay: 0.4s;
}

.star-d {
  right: 18%;
  top: 29%;
  width: 14px;
  height: 14px;
  animation-delay: 0.68s;
}

.star-e {
  left: 28%;
  top: 36%;
  width: 12px;
  height: 12px;
  animation-delay: 0.9s;
}

.bear-asset {
  user-select: none;
  -webkit-user-drag: none;
  pointer-events: none;
  filter: drop-shadow(0 28px 34px rgba(17, 17, 17, 0.14));
  transform-origin: center bottom;
}

.intro-sequence-wrap {
  width: 100%;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  animation: intro-run-in var(--intro-duration, 5000ms) cubic-bezier(0.2, 0.78, 0.22, 1) both;
  will-change: transform;
}

.idle-static-wrap {
  position: relative;
  width: min(470px, 82%);
  flex: 0 0 auto;
  line-height: 0;
}

.idle-static-wrap .bear-asset {
  width: 100%;
}

.idle-blink-overlay {
  position: absolute;
  inset: 0;
  filter: none;
}

.sequence-asset {
  width: min(470px, 82%);
}

.sequence-frame-wrap {
  position: relative;
  width: min(470px, 82%);
  flex: 0 0 auto;
  line-height: 0;
}

.sequence-frame-wrap .sequence-asset {
  width: 100%;
}

.password-foot-arc {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  overflow: visible;
}

.password-foot-arc path {
  fill: none;
  stroke: #010101;
  stroke-width: 1.35;
  stroke-linecap: round;
  stroke-linejoin: round;
  opacity: 0.96;
}

.cover-asset {
  width: min(470px, 82%);
}

.is-reduced-motion .bear-layer,
.is-reduced-motion .intro-sequence-wrap,
.is-reduced-motion .cover-asset,
.is-reduced-motion .sequence-asset {
  animation: none !important;
}

.is-reduced-motion .success-star {
  animation: none !important;
  opacity: 0.88;
  transform: scale(0.78);
}

@media (prefers-color-scheme: dark) {
  .polar-bear-mascot {
    --success-star-core: rgba(255, 255, 255, 0.98);
    --success-star-halo: rgba(255, 255, 255, 0.28);
  }
}

@keyframes intro-run-in {
  0% {
    transform: translateX(clamp(300px, 28vw, 460px));
  }
  76% {
    transform: translateX(-14px);
  }
  100% {
    transform: translateX(0);
  }
}

@keyframes success-star-pop {
  0% {
    opacity: 0;
    transform: scale(0.35) rotate(0deg);
  }
  14% {
    opacity: 1;
    transform: scale(1) rotate(18deg);
  }
  34% {
    opacity: 0.98;
    transform: scale(0.82) rotate(32deg) translateY(-2px);
  }
  62% {
    opacity: 0.46;
    transform: scale(1.12) rotate(52deg) translateY(-6px);
  }
  100% {
    opacity: 0;
    transform: scale(0.5) rotate(86deg) translateY(-10px);
  }
}
</style>
