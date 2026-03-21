<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { Plus, Trash2, StickyNote, Sparkles, PlusCircle } from 'lucide-vue-next'

interface Postit {
  id: number
  content: string
  color: string
}

const postits = ref<Postit[]>([])
const newContent = ref('')
const newColor = ref('#fef68a') // Amarelo post-it clássico mas pastel

const fetchPostits = async () => {
  try {
    const response = await axios.get('/api/v1/postits')
    postits.value = response.data
  } catch (error) {
    console.error('Erro ao buscar post-its:', error)
  }
}

const createPostit = async () => {
  if (!newContent.value.trim()) return
  
  try {
    const response = await axios.post('/api/v1/postits', {
      content: newContent.value,
      color: newColor.value
    })
    postits.value.unshift(response.data) // Adiciona no início
    newContent.value = ''
  } catch (error) {
    console.error('Erro ao criar post-it:', error)
  }
}

const deletePostit = async (id: number) => {
  try {
    await axios.delete(`/api/v1/postits/${id}`)
    postits.value = postits.value.filter(p => p.id !== id)
  } catch (error) {
    console.error('Erro ao excluir post-it:', error)
  }
}

onMounted(fetchPostits)
</script>

<template>
  <div class="app-wrapper">
    <!-- Background estético -->
    <div class="bg-blobs">
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
    </div>

    <div class="container">
      <header>
        <div class="logo">
          <div class="icon-box">
            <StickyNote :size="28" color="#fff" />
          </div>
          <h1>Post<span>it</span>.</h1>
        </div>
        <div class="status-badge">
          <Sparkles :size="14" /> Marcus Edition
        </div>
      </header>

      <main>
        <section class="input-card">
          <div class="input-header">
            <span>O que está na sua mente?</span>
            <div class="color-picker-wrapper">
              <input type="color" v-model="newColor" title="Escolha a cor" />
            </div>
          </div>
          <textarea 
            v-model="newContent" 
            placeholder="Digite sua nota aqui... (Ctrl + Enter para salvar)"
            @keyup.enter.ctrl="createPostit"
          ></textarea>
          <div class="input-footer">
            <p class="hint">Dica: Use cores diferentes para categorias.</p>
            <button @click="createPostit" :disabled="!newContent.trim()" class="btn-add">
              <PlusCircle :size="18" /> Criar Nota
            </button>
          </div>
        </section>

        <section class="notes-grid">
          <TransitionGroup name="list">
            <div 
              v-for="postit in postits" 
              :key="postit.id" 
              class="note"
              :style="{ backgroundColor: postit.color }"
            >
              <div class="note-pin"></div>
              <div class="note-body">
                <p>{{ postit.content }}</p>
              </div>
              <div class="note-footer">
                <button class="btn-delete" @click="deletePostit(postit.id)" title="Excluir">
                  <Trash2 :size="16" />
                </button>
              </div>
            </div>
          </TransitionGroup>
        </section>
      </main>
    </div>
  </div>
</template>

<style>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;600;800&family=Gochi+Hand&display=swap');

:root {
  --primary: #6366f1;
  --primary-hover: #4f46e5;
  --bg-main: #0f172a;
  --card-bg: rgba(255, 255, 255, 0.05);
  --text-main: #f8fafc;
  --text-dim: #94a3b8;
}

* {
  box-sizing: border-box;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

body {
  margin: 0;
  padding: 0;
  font-family: 'Inter', sans-serif;
  background-color: var(--bg-main);
  color: var(--text-main);
  overflow-x: hidden;
}

.app-wrapper {
  min-height: 100vh;
  position: relative;
  padding: 2rem 1rem;
}

/* Background Estético */
.bg-blobs {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: -1;
  overflow: hidden;
}

.blob {
  position: absolute;
  width: 500px;
  height: 500px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.2) 0%, rgba(168, 85, 247, 0.2) 100%);
  filter: blur(80px);
  border-radius: 50%;
}

.blob-1 { top: -100px; right: -100px; animation: orbit 20s infinite linear; }
.blob-2 { bottom: -100px; left: -100px; animation: orbit 25s infinite linear reverse; }

@keyframes orbit {
  from { transform: rotate(0deg) translateX(50px) rotate(0deg); }
  to { transform: rotate(360deg) translateX(50px) rotate(-360deg); }
}

.container {
  max-width: 1100px;
  margin: 0 auto;
}

/* Header Estiloso */
header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 3rem;
}

.logo {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.icon-box {
  background: var(--primary);
  padding: 0.5rem;
  border-radius: 12px;
  box-shadow: 0 10px 15px -3px rgba(99, 102, 241, 0.4);
}

.logo h1 {
  margin: 0;
  font-size: 1.75rem;
  font-weight: 800;
  letter-spacing: -1px;
}

.logo h1 span {
  color: var(--primary);
}

.status-badge {
  background: rgba(255, 255, 255, 0.1);
  padding: 0.4rem 0.8rem;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 0.4rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: var(--text-dim);
}

/* Input Card */
.input-card {
  background: var(--card-bg);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 24px;
  padding: 1.5rem;
  margin-bottom: 4rem;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
}

.input-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  color: var(--text-dim);
  font-size: 0.9rem;
  font-weight: 600;
}

.color-picker-wrapper {
  position: relative;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid rgba(255, 255, 255, 0.2);
}

input[type="color"] {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  cursor: pointer;
  border: none;
}

textarea {
  width: 100%;
  height: 120px;
  background: rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 16px;
  padding: 1.25rem;
  color: white;
  font-size: 1.1rem;
  font-family: 'Inter', sans-serif;
  resize: none;
  outline: none;
}

textarea:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.1);
}

.input-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 1rem;
}

.hint {
  font-size: 0.8rem;
  color: var(--text-dim);
  margin: 0;
}

.btn-add {
  background: var(--primary);
  color: white;
  border: none;
  padding: 0.8rem 1.5rem;
  border-radius: 12px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 0.6rem;
  box-shadow: 0 4px 14px 0 rgba(99, 102, 241, 0.39);
}

.btn-add:hover:not(:disabled) {
  background: var(--primary-hover);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(99, 102, 241, 0.23);
}

.btn-add:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  filter: grayscale(1);
}

/* Grid de Notas */
.notes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 2rem;
}

.note {
  min-height: 260px;
  padding: 2rem 1.5rem 1.5rem;
  border-radius: 2px 2px 40px 2px;
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  color: #1e293b; /* Texto escuro para post-its claros */
  box-shadow: 5px 5px 15px rgba(0, 0, 0, 0.3), 
              inset 0 -10px 20px rgba(0, 0, 0, 0.05);
  transform: rotate(var(--rotation, -1deg));
}

.note:nth-child(even) { --rotation: 1.5deg; }
.note:nth-child(3n) { --rotation: -2deg; }
.note:nth-child(4n) { --rotation: 1deg; }

.note:hover {
  transform: scale(1.05) rotate(0deg);
  z-index: 10;
  box-shadow: 10px 20px 30px rgba(0, 0, 0, 0.4);
}

.note-pin {
  position: absolute;
  top: 10px;
  left: 50%;
  transform: translateX(-50%);
  width: 12px;
  height: 12px;
  background: rgba(0, 0, 0, 0.1);
  border-radius: 50%;
  box-shadow: inset 2px 2px 4px rgba(0, 0, 0, 0.2);
}

.note-body {
  font-family: 'Gochi Hand', cursive;
  font-size: 1.4rem;
  line-height: 1.4;
  overflow-y: auto;
}

.note-footer {
  display: flex;
  justify-content: flex-end;
  opacity: 0;
  transform: translateY(10px);
}

.note:hover .note-footer {
  opacity: 1;
  transform: translateY(0);
}

.btn-delete {
  background: rgba(0, 0, 0, 0.1);
  border: none;
  padding: 0.5rem;
  border-radius: 8px;
  cursor: pointer;
  color: inherit;
}

.btn-delete:hover {
  background: rgba(255, 0, 0, 0.2);
  color: #ef4444;
}

/* Animações de Lista */
.list-enter-active,
.list-leave-active {
  transition: all 0.5s ease;
}
.list-enter-from {
  opacity: 0;
  transform: translateY(30px) scale(0.5);
}
.list-leave-to {
  opacity: 0;
  transform: scale(0.2);
}

/* Responsividade */
@media (max-width: 640px) {
  .notes-grid {
    grid-template-columns: 1fr;
  }
  .input-footer {
    flex-direction: column;
    gap: 1rem;
    align-items: stretch;
  }
  .hint { text-align: center; }
}
</style>
