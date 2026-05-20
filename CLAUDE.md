# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Enterprise IT technology management web application (企业信息技术管理).

Monorepo: frontend at root, backend in `backend/`.

## Commands

```bash
npm run dev                  # Start frontend dev server (HMR)
npm run build                # Frontend production build
npm run preview              # Preview production build locally
cd backend && mvnw spring-boot:run  # Start backend (port 8080)
```

## Architecture

- **Frontend**: Vue 3 with `<script setup>` SFC composition API, Vite build
- **Backend**: Spring Boot 3.3.7, Java 21, JPA + MySQL
- **Database**: MySQL `tech_manage` on localhost:3306, root / no password
- **Entry**: frontend `src/main.js` → `src/App.vue`; backend `TechManageApplication.java`

## Conventions

- Frontend components use `<script setup>` with Composition API
- No router or state management library yet — add Vue Router / Pinia when needed
- CSS variables prefixed with `--` are defined in `src/style.css :root`
