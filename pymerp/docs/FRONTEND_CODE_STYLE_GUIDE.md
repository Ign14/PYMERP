# 🎨 Frontend Code Style Guide - PYMERP UI

**Actualizado**: 4 de noviembre de 2025  
**Equipo**: Frontend Development  
**Herramientas**: ESLint 8.57.0 + Prettier 3.3.3 + EditorConfig

---

## 📋 Resumen

Esta guía define los estándares de código para el frontend React/TypeScript del proyecto PYMERP.

**Cumplimiento Obligatorio**:
- ✅ Todo código debe pasar `npm run lint`
- ✅ Todo código debe pasar `npm run format:check`
- ✅ Pre-commit hooks validan automáticamente

---

## 🔧 Configuración de IDE

### VS Code (Recomendado)

**Extensiones Requeridas**:
```json
{
  "recommendations": [
    "dbaeumer.vscode-eslint",
    "esbenp.prettier-vscode",
    "editorconfig.editorconfig"
  ]
}
```

**settings.json**:
```json
{
  "editor.formatOnSave": true,
  "editor.defaultFormatter": "esbenp.prettier-vscode",
  "editor.codeActionsOnSave": {
    "source.fixAll.eslint": true
  },
  "eslint.validate": [
    "javascript",
    "typescript",
    "javascriptreact",
    "typescriptreact"
  ]
}
```

### WebStorm / IntelliJ IDEA

1. Settings → Languages & Frameworks → JavaScript → Prettier
   - ✅ Enable "On save"
   - Package: `<project>/ui/node_modules/prettier`
   
2. Settings → Languages & Frameworks → JavaScript → Code Quality Tools → ESLint
   - ✅ Automatic ESLint configuration
   - ✅ Run eslint --fix on save

---

## 📝 Reglas de Código

### 1. Formateo General

**Prettier**:
- ✅ **NO semicolons** (`;`)
- ✅ **Single quotes** (`'`) para strings
- ✅ **Trailing commas** en ES5 (arrays, objects)
- ✅ **100 caracteres** de ancho máximo
- ✅ **2 espacios** de indentación
- ✅ **No tabs** (solo espacios)

```typescript
// ✅ CORRECTO
const user = {
  name: 'John',
  email: 'john@example.com',
}

// ❌ INCORRECTO
const user = {
  name: "John",
  email: "john@example.com"
};
```

### 2. TypeScript

#### 2.1 Tipos Explícitos

```typescript
// ✅ CORRECTO - Tipos explícitos en parámetros
function fetchUser(id: string): Promise<User> {
  return api.get<User>(`/users/${id}`)
}

// ⚠️ PERMITIDO CON WARNING - any solo cuando sea necesario
function handleDynamicData(data: any) {
  // @typescript-eslint/no-explicit-any: warn
}

// ❌ INCORRECTO - Parámetros sin tipo
function fetchUser(id) {
  return api.get(`/users/${id}`)
}
```

#### 2.2 Variables No Usadas

```typescript
// ✅ CORRECTO - Prefijo _ para variables intencionalmente ignoradas
function Component({ data, _meta }: Props) {
  return <div>{data.name}</div>
}

// ❌ INCORRECTO - Variable no usada sin prefijo
function Component({ data, meta }: Props) {
  return <div>{data.name}</div>  // meta no se usa
}
```

### 3. React

#### 3.1 Componentes Funcionales

```typescript
// ✅ CORRECTO - Componente funcional con TypeScript
interface UserCardProps {
  user: User
  onEdit?: (id: string) => void
}

export function UserCard({ user, onEdit }: UserCardProps) {
  return (
    <div className="user-card">
      <h3>{user.name}</h3>
      {onEdit && <button onClick={() => onEdit(user.id)}>Edit</button>}
    </div>
  )
}

// ❌ INCORRECTO - No usar React.FC (deprecated pattern)
export const UserCard: React.FC<UserCardProps> = ({ user }) => {
  return <div>{user.name}</div>
}
```

#### 3.2 Hooks

```typescript
// ✅ CORRECTO - Hooks deben estar en el top level
function Component() {
  const [count, setCount] = useState(0)
  
  useEffect(() => {
    // Effect logic
  }, [count])  // ✅ Dependencias correctas
  
  return <div>{count}</div>
}

// ❌ INCORRECTO - Hook dentro de condicional
function Component() {
  if (condition) {
    const [count, setCount] = useState(0)  // ❌ ERROR
  }
}

// ⚠️ WARNING - Dependencias faltantes
function Component({ userId }: Props) {
  useEffect(() => {
    fetchUser(userId)
  }, [])  // ⚠️ Falta userId en dependencias
}
```

#### 3.3 Imports de React

```typescript
// ✅ CORRECTO - React 18+ (no need to import React)
import { useState, useEffect } from 'react'

export function Component() {
  return <div>Hello</div>  // JSX funciona sin importar React
}

// ❌ INNECESARIO
import React from 'react'
```

### 4. Console & Debugging

```typescript
// ✅ CORRECTO - Solo console.warn y console.error permitidos
console.error('Failed to load user:', error)
console.warn('Deprecated API usage')

// ⚠️ WARNING - console.log debe ser removido antes de commit
console.log('User data:', user)  // OK en desarrollo, remover en producción

// ❌ PROHIBIDO - debugger statements
debugger  // Debe ser removido
```

### 5. Variables y Constantes

```typescript
// ✅ CORRECTO - const por defecto
const API_URL = 'https://api.example.com'
const users = await fetchUsers()

// ✅ CORRECTO - let solo cuando realmente se reasigna
let count = 0
count += 1

// ❌ INCORRECTO - var está prohibido
var name = 'John'  // ❌ Usar const o let
```

---

## 🚀 Comandos de Desarrollo

### Verificación de Código

```bash
# Verificar linting (no modifica archivos)
npm run lint

# Verificar formateo (no modifica archivos)
npm run format:check

# Ejecutar ambas verificaciones
npm run check
```

### Corrección Automática

```bash
# Fix automático de ESLint
npm run lint:fix

# Fix automático de Prettier
npm run format

# Recomendado: ejecutar ambos
npm run lint:fix && npm run format
```

### Durante Desarrollo

```bash
# Iniciar dev server (hot reload automático)
npm run dev

# El IDE debería formatear automáticamente al guardar
# Si no, ejecutar manualmente:
npm run format
```

---

## 📦 Estructura de Archivos

### Naming Conventions

```
ui/src/
├── components/
│   ├── UserCard.tsx          # ✅ PascalCase para componentes
│   ├── SalesChart.tsx
│   └── dialogs/
│       └── UserFormDialog.tsx
├── pages/
│   ├── DashboardPage.tsx     # ✅ PascalCase + sufijo "Page"
│   └── CustomersPage.tsx
├── hooks/
│   ├── useAuth.ts            # ✅ camelCase + prefijo "use"
│   └── useDebouncedValue.ts
├── services/
│   ├── apiClient.ts          # ✅ camelCase
│   └── authService.ts
├── utils/
│   ├── formatters.ts
│   └── validators.ts
└── constants/
    └── apiRoutes.ts
```

### Imports Order

```typescript
// ✅ CORRECTO - Orden de imports
// 1. Node modules
import { useState, useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'

// 2. Absolute imports (con alias @ si está configurado)
import { Button } from '@/components/Button'
import { useAuth } from '@/hooks/useAuth'

// 3. Relative imports
import { UserCard } from './UserCard'
import './styles.css'

// ❌ INCORRECTO - Mezclar tipos de imports sin orden
import './styles.css'
import { useState } from 'react'
import { UserCard } from './UserCard'
```

---

## ✅ Pre-commit Checklist

Antes de hacer commit, el pre-commit hook automáticamente verifica:

1. ✅ ESLint pasa sin errores
2. ✅ Prettier está aplicado
3. ✅ No hay `console.log` (solo warnings)
4. ✅ No hay `debugger` statements
5. ✅ No hay variables no usadas sin prefijo `_`

**Si el commit falla**:

```bash
# Fix los issues automáticamente
npm run lint:fix
npm run format

# Revisar cambios
git diff

# Intentar commit de nuevo
git add .
git commit -m "feat: tu mensaje"
```

---

## 🔍 Troubleshooting

### "ESLint warnings pero quiero commitear"

```bash
# Opción 1: Fix warnings
npm run lint:fix

# Opción 2: Bypass pre-commit (NO RECOMENDADO)
git commit --no-verify
```

### "Prettier cambió muchos archivos"

Esto es **normal** en la primera vez. Prettier formateará todo el código existente.

```bash
# Hacer un commit separado de formateo
git add .
git commit -m "style: apply prettier formatting to all files"
```

### "TypeScript errors en VSCode"

```bash
# Reinstalar dependencias
cd ui
rm -rf node_modules package-lock.json
npm install

# Reiniciar TypeScript server en VSCode
# Cmd/Ctrl + Shift + P → "TypeScript: Restart TS Server"
```

---

## 📚 Referencias

- **ESLint Config**: `ui/.eslintrc.json`
- **Prettier Config**: `ui/.prettierrc`
- **EditorConfig**: `.editorconfig` (raíz del proyecto)
- **Pre-commit Hook**: `.git/hooks/pre-commit`

**Documentación Externa**:
- ESLint Rules: https://eslint.org/docs/rules/
- TypeScript ESLint: https://typescript-eslint.io/rules/
- Prettier Options: https://prettier.io/docs/en/options.html
- React Hooks Rules: https://react.dev/reference/rules/rules-of-hooks

---

**¿Dudas?** Consulta con el equipo de frontend o abre un issue en el repositorio.
