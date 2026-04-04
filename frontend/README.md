# Post-it Frontend

Vue 3 + TypeScript + Vite frontend application for the Post-it note management system.

## Project Structure

```
frontend/
├── src/
│   ├── components/               # Reusable Vue components
│   │   ├── PostitForm.vue        # Form for creating notes
│   │   ├── PostitCard.vue        # Individual note card
│   │   └── PostitGrid.vue        # Grid layout for notes
│   ├── composables/              # Vue 3 composables (reusable logic)
│   │   ├── usePostits.ts         # State management for post-its
│   │   └── useError.ts           # Error handling logic
│   ├── services/
│   │   └── postitApi.ts          # API client for backend
│   ├── App.vue                   # Root component
│   ├── main.ts                   # Entry point
│   └── assets/                   # Static assets
├── .env.example                  # Environment variables template
├── vite.config.ts                # Vite configuration with API proxy
├── tsconfig.json                 # TypeScript configuration
├── package.json                  # Dependencies
└── README.md                     # This file
```

## Prerequisites

- **Node.js** 18+ (LTS recommended)
- **npm** 9+
- Backend running on `http://localhost:8080` (for local development)

## Installation

```bash
# Install dependencies
npm install
```

## Development

### Run Dev Server

```bash
# Start Vite dev server with HMR (Hot Module Replacement)
npm run dev

# Server runs on http://localhost:5173 (unless port 5173 is in use)
```

**Features:**
- 🔥 Hot reload — changes reflect instantly
- 📍 Source maps — debug TypeScript directly in browser
- 🔌 API proxy — requests to `/api/*` forward to `http://localhost:8080`

### Build for Production

```bash
# Create optimized build
npm run build

# Output: dist/ directory (ready for deployment)
```

### Preview Production Build

```bash
# Serve production build locally
npm run preview

# Verify optimization, bundle size, etc.
```

## Configuration

### Environment Variables

Copy `.env.example` to `.env.local` for local overrides:

```bash
cp .env.example .env.local
```

Available variables:
- `VITE_API_BASE_URL` — Backend API URL (default: `http://localhost:8080/api/v1`)

**Development modes:**
- **Local backend:** `http://localhost:8080/api/v1`
- **Docker (via network):** `http://api:8080/api/v1`
- **Production:** `https://api.example.com/api/v1`

## Components

### PostitForm

Form component for creating new notes.

**Props:**
- None

**Emits:**
- `submit(payload: PostitRequest)` — Emitted when form is submitted
- `loading(isLoading: boolean)` — Emitted during async operations

**Features:**
- Color picker (native + presets)
- Character counter (max 120, warning at 100)
- Keyboard shortcuts (Ctrl+Enter)
- Form validation
- Error handling

### PostitCard

Individual note card component.

**Props:**
- `postit: Postit` — Note data
  - `id: number`
  - `content: string`
  - `color: string` (hex color)

**Emits:**
- `delete(id: number)` — Emitted when delete button clicked

**Features:**
- Smart text color contrast (readable on any background)
- Hover animations
- Delete button (hidden until hover)
- Handwritten font styling

### PostitGrid

Grid layout for displaying multiple notes.

**Props:**
- `postits: Postit[]` — Array of notes
- `isLoading?: boolean` — Loading state

**Emits:**
- `delete(id: number)` — Delegated from PostitCard

**Features:**
- Responsive grid (auto-fill layout)
- Empty state UI
- Loading skeleton
- Staggered animation on add/remove
- Mobile optimized

## Composables

### usePostits

State management for post-its. Handles all API interactions.

```typescript
const {
  postits,           // Ref<Postit[]>
  isLoading,         // Ref<boolean>
  error,             // Ref<string>
  isCreating,        // Ref<boolean>
  isDeletingId,      // Ref<number | null>

  fetchPostits,      // () => Promise<void>
  createPostit,      // (request) => Promise<Postit | null>
  deletePostit,      // (id) => Promise<boolean>
  updatePostit,      // (id, request) => Promise<Postit | null>

  isEmpty,           // ComputedRef<boolean>
  itemCount,         // ComputedRef<number>
} = usePostits();
```

**Error Handling:**
- Catches API errors and sets readable error messages
- Provides `isCreating` and `isDeletingId` flags for UI feedback
- Auto-updates state after successful operations

### useError

Reusable error management with auto-clear capability.

```typescript
const {
  error,        // Ref<string>
  isError,      // ComputedRef<boolean>
  setError,     // (message, duration?) => void
  clearError,   // () => void
} = useError();

// Usage
setError('Something went wrong', 3000); // Auto-clears after 3s
```

## API Service

### postitApi

Singleton service for backend communication.

```typescript
import { postitApi, type Postit, type PostitRequest } from './services/postitApi';

// Get all notes
const notes = await postitApi.getAllPostits();

// Create note
const newNote = await postitApi.createPostit({
  content: 'My note',
  color: '#fef68a'
});

// Update note
const updated = await postitApi.updatePostit(id, {
  content: 'Updated',
  color: '#fca5a5'
});

// Delete note
await postitApi.deletePostit(id);
```

**Features:**
- Centralized axios instance
- Automatic error logging (dev mode)
- Configurable base URL via environment
- TypeScript types for all operations
- Request timeout (5s)

## Styling

### Design System

**Colors (CSS Variables):**
```css
--primary: #6366f1 (Indigo)
--primary-hover: #4f46e5
--bg-main: #0f172a (Dark blue)
--card-bg: rgba(255, 255, 255, 0.05) (Frosted glass)
--text-main: #f8fafc (Light gray)
--text-dim: #94a3b8 (Medium gray)
```

**Fonts:**
- `Inter` (sans-serif) — UI text
- `Gochi Hand` (cursive) — Note content (handwritten feel)

### Responsive Breakpoints

- **Desktop:** 1024px+
- **Tablet:** 768px - 1023px
- **Mobile:** < 768px

All components are fully responsive with mobile-first approach.

## TypeScript

Project uses **TypeScript 5.3.3** with strict mode enabled.

**Type-safe development:**
- Interface definitions for API models (Postit, PostitRequest)
- Component props and emits fully typed
- Composable return types explicit
- No `any` types without justification

## Performance Optimizations

- 📦 **Tree-shaking** — Unused code removed in build
- 🎯 **Code splitting** — Automatic chunk separation
- 🖼️ **Asset optimization** — Images/fonts optimized
- ⚡ **HMR** — Fast refresh during development
- 🔄 **Service worker** — Optional PWA caching

## Browser Support

- Chrome/Edge 90+
- Firefox 88+
- Safari 14+
- Mobile browsers (iOS Safari, Chrome Mobile)

## Troubleshooting

### "Cannot GET /api/postits" Error

**Issue:** Frontend can't reach backend API.

**Solutions:**
1. Verify backend is running: `curl http://localhost:8080/actuator/health`
2. Check Vite proxy config in `vite.config.ts`
3. In Docker, ensure networks are shared and API is healthy

### Port Already in Use

```bash
# Kill process on port 5173 (macOS/Linux)
lsof -ti:5173 | xargs kill -9

# Windows: use Task Manager or
netstat -ano | findstr :5173
taskkill /PID <PID> /F
```

### Styling Issues

- Clear browser cache: `Ctrl+Shift+Delete`
- Rebuild: `npm run build`
- Check CSS variables are inherited from App.vue

### TypeScript Errors

```bash
# Type-check entire project
npx tsc --noEmit

# Generate type definitions
npm run build
```

## Development Workflow

### 1. Create a New Component

```bash
# Create new component
touch src/components/MyComponent.vue
```

Template:
```vue
<script setup lang="ts">
// Import types and utilities
import { ref } from 'vue';

interface Props {
  title?: string;
}

withDefaults(defineProps<Props>(), {
  title: 'Default Title',
});

const state = ref('initial');
</script>

<template>
  <div class="my-component">
    <!-- Template here -->
  </div>
</template>

<style scoped>
.my-component {
  /* Styles here */
}
</style>
```

### 2. Use in Parent Component

```vue
<script setup lang="ts">
import MyComponent from './components/MyComponent.vue';
</script>

<template>
  <MyComponent title="Hello" />
</template>
```

### 3. Run Tests (if configured)

```bash
# Unit tests with Vitest (future enhancement)
npm run test
```

## Deployment

### Static Hosting (Netlify, Vercel, GitHub Pages)

```bash
# Build
npm run build

# Deploy `dist/` folder to hosting provider
```

### Docker (included in docker-compose.yml)

```dockerfile
# Multi-stage build
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Serve with Nginx
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

## Contributing

1. Follow Vue 3 Composition API best practices
2. Use TypeScript for type safety
3. Keep components small and focused
4. Document complex logic with comments
5. Test locally before committing

## License

MIT
