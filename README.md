# SICOI Mobile — Guia de Configuração e Integração

## Visão Geral

Aplicativo Android nativo em **Kotlin + Jetpack Compose** para o ecossistema SICOI.  
Compartilha o mesmo banco Supabase do sistema Web.

---

## 📋 Pré-requisitos

| Ferramenta | Versão Mínima |
|---|---|
| Android Studio | Iguana (2024.x) ou superior |
| JDK | 17 |
| Gradle | 8.6 |
| Android SDK | API 26+ (Android 8.0) |

---

## 🚀 Configuração Inicial

### 1. Preparar o Supabase

Execute o script SQL no painel do Supabase:

```bash
# Abra o Supabase Dashboard → SQL Editor
# Cole e execute o conteúdo de: ../qr-asset-control/MOBILE_APP_TABLES.sql
```

### 2. Criar o Bucket de Armazenamento

No Supabase Dashboard → **Storage**:

1. Crie um bucket chamado: `os-attachments`
2. Marque como **Público**
3. Defina o tamanho máximo de upload para **10MB**

### 3. Configurar o Firebase (para Push Notifications)

1. Acesse: https://console.firebase.google.com
2. Crie um novo projeto: **SICOI Industrial**
3. Adicione um app Android com o package: `br.com.sicoi.mobile`
4. Baixe o arquivo `google-services.json`
5. Coloque o arquivo em: `app/google-services.json`

> ⚠️ **Sem o `google-services.json` o projeto não compila com FCM ativo.**  
> Para compilar sem FCM temporariamente, remova as linhas do Firebase do `app/build.gradle.kts`.

### 4. Abrir no Android Studio

```bash
# Navegue até o diretório:
cd "c:\Users\User\Desktop\Projetos VIBE CODING\Sicoi 26\sicoi-android"

# Abra o Android Studio → File → Open → selecione esta pasta
```

---

## 📱 Arquitetura

```
MVVM + Clean Architecture
├── data/
│   ├── model/       → Data classes (espelho das tabelas Supabase)
│   └── repository/  → AuthRepository, WorkOrderRepository
├── core/
│   ├── network/     → SupabaseClient (singleton)
│   ├── database/    → Room (AppDatabase, DAOs, Entities)
│   ├── sync/        → OfflineSyncWorker (WorkManager)
│   └── fcm/         → SicoiFirebaseService
└── ui/
    ├── theme/       → Color, Theme, Typography (identidade SICOI)
    ├── login/       → LoginScreen + SignupScreen + AuthViewModel
    ├── modules/     → ModulesScreen
    ├── technicians/ → TechniciansScreen + ViewModel
    ├── workorders/  → WorkOrdersScreen + ViewModel
    └── osform/      → OSFormScreen + ViewModel + SignatureCanvas + CameraCapture
```

---

## 🔄 Fluxo de Autenticação

```
1. Usuário faz cadastro pelo app
   → Conta criada no Supabase Auth com is_mobile_user = true
   → approval_status = 'pending'

2. Admin vê o badge "Configurações" no Web Dashboard
   → Acessa /admin/users
   → Aprova ou rejeita o usuário

3. Usuário tenta login:
   → approved → acesso ao app
   → pending  → alerta "aguardando aprovação"
   → rejected → mensagem de acesso negado
```

---

## 📶 Modo Offline

```
Conectado:
  fetchOpenOrders() → Supabase → cacheia no Room

Sem conexão:
  getOpenOrdersFlow() → Room (dados em cache)
  finalizeWorkOrder() → salva localmente com syncPending = true

Ao reconectar:
  OfflineSyncWorker → detecta OS com syncPending = true → envia ao Supabase
```

---

## 🔔 Push Notifications (FCM)

O payload enviado pelo sistema Web via Firebase Admin SDK deve seguir este formato:

```json
{
  "token": "FCM_TOKEN_DO_TECNICO",
  "data": {
    "type": "NEW_OS",
    "os_id": "uuid-da-os",
    "numero_os": "OS-2026-001",
    "prioridade": "Urgente",
    "equipamento": "Torno CNC 3",
    "tecnico": "Rodrigo Santos"
  }
}
```

O token FCM de cada técnico é atualizado automaticamente na tabela `user_profiles.fcm_token`.

---

## 🏗️ Build e Deploy

```bash
# Debug (instalação via USB/emulador)
./gradlew installDebug

# Release APK
./gradlew assembleRelease

# Bundle para Google Play
./gradlew bundleRelease
```

---

## 🗄️ Tabelas Supabase Utilizadas

| Tabela | Uso |
|---|---|
| `auth.users` | Autenticação (Supabase nativo) |
| `public.user_profiles` | Perfis + status de aprovação + FCM token |
| `public.ind_maint_technicians` | Lista de técnicos |
| `public.ind_maint_os` | Ordens de Serviço |
| `storage/os-attachments` | Fotos e assinaturas digitais |

---

## 📞 Funções RPC Supabase Utilizadas

| Função | Descrição |
|---|---|
| `get_open_os_by_technician(p_technician_name)` | OS abertas filtradas por técnico |
| `finalize_os(...)` | Finaliza uma OS com solução, peças, mídia |
| `update_fcm_token(p_fcm_token, ...)` | Atualiza o token FCM do usuário logado |
| `approve_user(user_id)` | Aprova um usuário mobile (chamada do Web) |
| `reject_user(user_id)` | Rejeita um usuário mobile (chamada do Web) |
