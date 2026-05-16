# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Enterprise IT budget and contract management web application (企业信息科技预算管理、合同管理).

## Commands

```bash
npm run dev       # Start dev server (HMR)
npm run build     # Production build
npm run preview   # Preview production build locally
```

## Architecture

- **Framework**: Vue 3 with `<script setup>` SFC composition API
- **Build**: Vite with `@vitejs/plugin-vue`
- **Entry**: `src/main.js` mounts `src/App.vue` to `index.html#app`
- **Styling**: Plain CSS (`src/style.css`), no preprocessor configured

## Conventions

- Components use `<script setup>` with Composition API
- No router or state management library yet — add Vue Router / Pinia when needed
- CSS variables prefixed with `--` are defined in `src/style.css :root`
