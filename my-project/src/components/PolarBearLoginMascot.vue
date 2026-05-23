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
            :key="animationAssetKey"
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
            :key="animationAssetKey"
            :src="currentModeAsset"
            :alt="currentModeAlt"
            class="bear-asset sequence-asset"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import coverPeekAsset from '@/assets/bear/generated-cv/transparent-svg/cover-peek.svg';
import errorSadAsset from '@/assets/bear/generated-cv/transparent-svg/error-sad.svg';
import idleBlinkOverlayAsset from '@/assets/bear/generated-cv/transparent-svg/idle-eye-blink-overlay.svg';
import idleOpenAsset from '@/assets/bear/generated-cv/transparent-svg/idle-open.svg';
import successLiftAsset from '@/assets/bear/generated-cv/transparent-svg/success-lift.svg';
import sequenceManifest from '@/assets/bear/generated-cv/video-sequences/manifest.json';
import errorAnimationAsset from '@/assets/bear/generated-cv/video-sequences/animated/error.webp';
import idleInteractAnimationAsset from '@/assets/bear/generated-cv/video-sequences/animated/idleInteract.webp';
import introAnimationAsset from '@/assets/bear/generated-cv/video-sequences/animated/intro.webp';
import successAnimationAsset from '@/assets/bear/generated-cv/video-sequences/animated/success.webp';

function loadSequence(modules) {
  return Object.entries(modules)
    .sort(([leftKey], [rightKey]) => leftKey.localeCompare(rightKey, undefined, { numeric: true }))
    .map(([, value]) => value);
}

const passwordFrameSources = loadSequence(
  import.meta.glob('../assets/bear/generated-cv/video-sequences/password/frame-*.svg', { eager: true, import: 'default' })
);

const animationAssetByMode = {
  intro: introAnimationAsset,
  idleInteract: idleInteractAnimationAsset,
  error: errorAnimationAsset,
  success: successAnimationAsset
};

const fallbackAssetByMode = {
  intro: idleOpenAsset,
  idle: idleOpenAsset,
  password: passwordFrameSources[passwordFrameSources.length - 1] || coverPeekAsset,
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

const animationPlaybackKey = ref(0);
const idleInteractActive = ref(false);
const idleBlinkVisible = ref(false);
const passwordFrameIndex = ref(0);

const activeModeKey = computed(() => {
  if (props.mode === 'idle' && idleInteractActive.value) {
    return 'idleInteract';
  }

  return props.mode;
});

const currentModeAsset = computed(() => {
  if (activeModeKey.value === 'password') {
    return passwordFrameSources[Math.min(passwordFrameIndex.value, passwordFrameSources.length - 1)]
      || fallbackAssetByMode.password;
  }

  if (!props.reducedMotion) {
    const animationAsset = animationAssetByMode[activeModeKey.value];
    if (animationAsset) {
      return animationAsset;
    }
  }

  return fallbackAssetByMode[activeModeKey.value] || fallbackAssetByMode[props.mode] || idleOpenAsset;
});

const introMotionStyle = computed(() => ({
  '--intro-duration': `${Math.max(currentSequenceDurationMs.value, 160)}ms`,
  '--intro-steps': String(Math.max(getSequenceFrameCount('intro'), 1))
}));

const animationAssetKey = computed(() => `${activeModeKey.value}-${animationPlaybackKey.value}`);

const currentModeAlt = computed(() => {
  if (props.mode === 'idle') return 'polar bear idle animation frame';
  if (activeModeKey.value === 'idleInteract') return 'polar bear idle interaction animation frame';
  if (props.mode === 'password') return 'polar bear password animation frame';
  if (props.mode === 'error') return 'polar bear error animation frame';
  if (props.mode === 'success') return 'polar bear success animation frame';
  return 'polar bear animation frame';
});

const currentSequenceDurationMs = computed(() => {
  return getSequenceDurationMs(activeModeKey.value);
});

let sequenceTimer = 0;
let completeTimer = 0;
let idlePauseTimer = 0;

const INTRO_SPEED_MULTIPLIER = 1.35;

function getSequenceMeta(mode) {
  return sequenceManifest.sequences?.[mode] || {};
}

function getSequenceFrameCount(mode) {
  return Number(getSequenceMeta(mode).frameCount || 0);
}

function getSequenceFps(mode) {
  return Math.max(Number(getSequenceMeta(mode).fps || 24), 1);
}

function getSequenceDurationMs(mode) {
  const frameCount = getSequenceFrameCount(mode);
  const duration = frameCount > 0 ? (frameCount / getSequenceFps(mode)) * 1000 : 0;
  return mode === 'intro'
    ? duration / INTRO_SPEED_MULTIPLIER
    : duration;
}

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

function restartAnimatedAsset() {
  animationPlaybackKey.value += 1;
}

const preloadedPasswordImages = [];

function preloadPasswordFrames() {
  if (typeof window === 'undefined') {
    return;
  }

  passwordFrameSources.forEach((source) => {
    const image = new Image();
    image.decoding = 'async';
    image.src = source;
    preloadedPasswordImages.push(image);
  });
}

function nextIdleBlinkDelay(immediate = false) {
  if (immediate) {
    return 520 + Math.random() * 260;
  }

  return 2800 + Math.random() * 1400;
}

function playIdleSequence() {
  clearAllTimers();
  idleInteractActive.value = false;
  idleBlinkVisible.value = false;
  passwordFrameIndex.value = 0;

  if (props.reducedMotion) {
    return;
  }
  const blinkDuration = 120;

  const scheduleNextBlink = (immediate = false) => {
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
    }, nextIdleBlinkDelay(immediate));
  };

  scheduleNextBlink(true);
}

function playIdleInteractSequence() {
  clearAllTimers();
  idleInteractActive.value = true;
  idleBlinkVisible.value = false;
  passwordFrameIndex.value = 0;
  restartAnimatedAsset();

  if (props.reducedMotion) {
    idleInteractActive.value = false;
    return;
  }

  sequenceTimer = window.setTimeout(() => {
    if (props.mode !== 'idle' || !idleInteractActive.value) {
      return;
    }

    idleInteractActive.value = false;
    playIdleSequence();
  }, Math.max(getSequenceDurationMs('idleInteract'), 160));
}

function handleIdleInteract() {
  if (props.mode !== 'idle' || idleInteractActive.value || props.reducedMotion) {
    return;
  }

  if (!animationAssetByMode.idleInteract) {
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
  idleInteractActive.value = false;
  passwordFrameIndex.value = 0;

  if (!passwordFrameSources.length) {
    return;
  }

  if (props.reducedMotion) {
    passwordFrameIndex.value = passwordFrameSources.length - 1;
    return;
  }

  const frameDuration = 1000 / getSequenceFps('password');

  const advancePasswordFrame = () => {
    if (props.mode !== 'password') {
      return;
    }

    if (passwordFrameIndex.value >= passwordFrameSources.length - 1) {
      return;
    }

    passwordFrameIndex.value += 1;
    sequenceTimer = window.setTimeout(advancePasswordFrame, frameDuration);
  };

  sequenceTimer = window.setTimeout(advancePasswordFrame, frameDuration);
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
  idleInteractActive.value = false;
  passwordFrameIndex.value = 0;
  restartAnimatedAsset();

  const animationAsset = animationAssetByMode[mode];
  if (!animationAsset) {
    if (mode === 'intro' || mode === 'success' || mode === 'error') {
      completeTimer = window.setTimeout(() => emitSequenceComplete(mode), 120);
    }
    return;
  }

  if (props.reducedMotion) {
    if (mode === 'intro' || mode === 'success' || mode === 'error') {
      completeTimer = window.setTimeout(() => emitSequenceComplete(mode), 160);
    }
    return;
  }

  sequenceTimer = window.setTimeout(() => {
    if (props.mode !== mode) {
      return;
    }

    emitSequenceComplete(mode);
  }, Math.max(getSequenceDurationMs(mode), 160));
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
    passwordFrameIndex.value = 0;

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

onMounted(() => {
  preloadPasswordFrames();
});

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
  backface-visibility: hidden;
}

.intro-sequence-wrap {
  width: 100%;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  animation: intro-run-in var(--intro-duration, 5000ms) steps(var(--intro-steps, 121), end) both;
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

.is-reduced-motion .bear-layer,
.is-reduced-motion .intro-sequence-wrap,
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
