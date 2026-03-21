# Frontend Quick Start

Get the Vue 3 frontend running in 5 minutes.

## Prerequisites

✅ Node.js 18+ installed
✅ Backend running on `http://localhost:8080` (or `npm run dev` will handle it)

## Option 1: Local Development (Recommended for Development)

```bash
# Navigate to frontend
cd frontend

# Install dependencies (one-time)
npm install

# Start dev server with hot reload
npm run dev

# Open browser to http://localhost:5173
```

**What you get:**
- 🔥 Hot module replacement (instant refresh on save)
- 🐛 Full TypeScript support
- 📍 Source maps (debug original code)
- 🔌 API proxy (requests to `/api/*` forward to backend)

### Test the API Connection

1. Ensure backend is running:
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

2. Open http://localhost:5173 in browser

3. Create a note → it should appear in the grid

If API calls fail, check:
- Backend health: `curl http://localhost:8080/actuator/health`
- Browser console for error messages
- Network tab in DevTools (check /api/v1/postits requests)

---

## Option 2: Docker (Full Stack)

```bash
# From project root
docker compose up -d

# Access frontend at http://localhost:3000
```

Services:
- 🗄️ PostgreSQL: localhost:5432
- 📡 Backend API: localhost:8080
- 🖥️ Frontend: localhost:3000

Cleanup:
```bash
# Stop containers
docker compose down

# Remove volumes (DELETE DATA)
docker compose down -v
```

---

## Option 3: Production Build

```bash
cd frontend

# Build optimized bundle
npm run build

# Preview production build locally
npm run preview

# Output: dist/ directory (ready for deployment)
```

---

## File Organization

| File | Purpose |
|------|---------|
| `src/App.vue` | Root component (layout + orchestration) |
| `src/components/PostitForm.vue` | Create note form |
| `src/components/PostitCard.vue` | Individual note display |
| `src/components/PostitGrid.vue` | Note grid layout |
| `src/services/postitApi.ts` | API client |
| `src/composables/usePostits.ts` | State management |
| `vite.config.ts` | Build configuration + API proxy |
| `package.json` | Dependencies (Vue 3, Axios, Vite) |

---

## Common Tasks

### Create a Note

1. Type in textarea
2. Choose color (or use default yellow)
3. Click "Criar Nota" or press `Ctrl+Enter`
4. Note appears at top of grid

### Delete a Note

1. Hover over note card
2. Click trash icon (appears on hover)
3. Confirm deletion

### Change API URL

Edit `frontend/.env.local`:
```env
# Local backend on localhost:8080
VITE_API_BASE_URL=http://localhost:8080/api/v1

# Or Docker internal network
VITE_API_BASE_URL=http://api:8080/api/v1

# Or production API
VITE_API_BASE_URL=https://api.example.com/api/v1
```

Restart dev server after changing.

---

## Troubleshooting

### "Failed to fetch from /api/v1/postits"

**Cause:** Backend not running

**Fix:**
```bash
# Terminal 1: Backend
cd backend
./mvnw spring-boot:run

# Terminal 2: Frontend
cd frontend
npm run dev
```

### Empty notes list, but no errors

**Cause:** Backend database has no notes yet

**Fix:** Create a note using the form

### Styles not loading correctly

**Cause:** CSS not being compiled

**Fix:**
```bash
# Clear Vite cache
rm -rf frontend/node_modules/.vite

# Restart
npm run dev
```

### Port already in use

**Port 5173 (frontend dev):**
```bash
lsof -ti:5173 | xargs kill -9
```

**Port 8080 (backend):**
```bash
lsof -ti:8080 | xargs kill -9
```

---

## Development Tips

### Use Browser DevTools

- **Vue DevTools** (Chrome extension): `https://devtools.vuejs.org/`
- **Network tab**: Monitor API calls
- **Console**: Check for errors

### Hot Reload Shortcuts

- `Ctrl+Shift+R`: Full page refresh
- `Ctrl+Z`: Undo
- `F12`: Toggle DevTools

### Debug API Calls

Open browser console → Network tab:
```
GET /api/v1/postits (200 OK)
POST /api/v1/postits (201 Created)
DELETE /api/v1/postits/1 (204 No Content)
```

---

## What's Included

✅ **Vue 3** with Composition API
✅ **TypeScript** for type safety
✅ **Vite** for blazing-fast dev server
✅ **Axios** for API calls
✅ **Lucide Vue** for icons
✅ **Responsive design** (mobile, tablet, desktop)
✅ **Beautiful animations** and transitions
✅ **Accessibility** features (ARIA labels, keyboard nav)

---

## Architecture Overview

```
Frontend Request
    ↓
App.vue (orchestration)
    ↓
PostitForm.vue (user input) or PostitGrid.vue (display)
    ↓
usePostits composable (state management)
    ↓
postitApi service (HTTP client)
    ↓
Axios HTTP request
    ↓
Backend API (/api/v1/postits)
    ↓
Spring Boot + PostgreSQL
```

---

## Next Steps

1. **Run locally:** `npm run dev`
2. **Create notes:** Use the form
3. **Delete notes:** Hover and click trash
4. **Explore code:** Check `src/` directory
5. **Customize:** Edit colors, fonts, animations in components

---

## Resources

- 📖 [Vue 3 Docs](https://vuejs.org/)
- 📖 [Vite Docs](https://vitejs.dev/)
- 📖 [TypeScript Docs](https://www.typescriptlang.org/)
- 📖 [Axios Docs](https://axios-http.com/)

---

## Support

For issues, check:
1. Console errors (F12 → Console)
2. Network errors (F12 → Network → /api/*)
3. Backend health: `curl http://localhost:8080/actuator/health`
4. Documentation in `frontend/README.md`
