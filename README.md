# 🏋️ UniFIT

<p align="center">
  <strong>Aplicativo de gerenciamento da academia da Universidade de Fortaleza (Unifor).</strong>
</p>

<p align="center">
  Treinos, aulas coletivas, acompanhamento físico, pagamentos e comunicação com professores em um único aplicativo.
</p>

<p align="center">
  <img alt="Android" src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-XML%20Views-7F52FF?logo=kotlin&logoColor=white">
  <img alt="Supabase" src="https://img.shields.io/badge/Supabase-Postgres%20%2B%20Realtime-3FCF8E?logo=supabase&logoColor=white">
  <img alt="Pix" src="https://img.shields.io/badge/Pagamento-Pix-32BCAD">
</p>

---

<p align="center">
  <img src="Banner.jpg" alt="Banner UniFIT" width="100%">
</p>

## 📌 Sobre o projeto

O **UniFIT** é um aplicativo Android desenvolvido em Kotlin para centralizar os serviços da academia da Universidade de Fortaleza (Unifor).

O projeto conta com duas interfaces: uma para **alunos**, que podem consultar treinos, acompanhar seu progresso, participar de aulas coletivas e conversar com professores; e outra para **professores**, responsáveis pelo gerenciamento dos alunos, exercícios, treinos e aulas.

A aplicação utiliza o **Supabase** como backend, integrando autenticação, banco de dados PostgreSQL, atualizações em tempo real e gerenciamento de permissões.

---

## 📱 Funcionalidades

### 🧑‍🎓 Área do aluno

| Funcionalidade | Descrição |
|---|---|
| 🏠 **Início** | Treino do dia, atalhos e sugestões de treinos por categoria. |
| 💪 **Treinos** | Exercícios com vídeos, séries, repetições e registro de cargas. |
| 🔥 **Frequência** | Acompanhamento dos dias treinados durante a semana. |
| 📅 **Aulas** | Calendário de aulas coletivas, inscrições e vagas disponíveis. |
| 📈 **Progresso** | Histórico de peso, medidas corporais e gráficos de evolução. |
| 💬 **Chat** | Comunicação em tempo real com professores, confirmação de leitura e exclusão de mensagens. |
| 🔔 **Notificações** | Histórico de notificações com busca, filtros e exclusão. |
| 💳 **Plano** | Consulta do plano, pagamento por Pix e atualização automática após confirmação. |

### 🧑‍🏫 Área do professor

| Funcionalidade | Descrição |
|---|---|
| 📊 **Dashboard** | Visão geral da academia, usuários online e aulas do dia. |
| 👥 **Alunos** | Consulta de perfis, acompanhamento e gerenciamento de treinos individuais. |
| 🏋️ **Exercícios** | Biblioteca com categorias, equipamentos, imagens e vídeos demonstrativos. |
| 📋 **Treinos** | Criação de modelos de treinos reutilizáveis e atribuição aos alunos. |
| 🗓️ **Aulas** | Cadastro, edição, exclusão e gerenciamento de inscrições em aulas coletivas. |
| 💬 **Atendimento** | Acesso às conversas dos alunos por meio de um chat compartilhado entre professores. |

---

## ⚙️ Como funciona?

O UniFIT utiliza uma arquitetura cliente-servidor. O aplicativo Android se comunica diretamente com o Supabase para autenticação, consulta e atualização dos dados.

As operações que exigem credenciais privadas, como a integração com o Mercado Pago, são processadas por funções executadas no backend.

```text
┌──────────────────────┐        ┌─────────────────────────────────────┐
│   📱 App Android     │        │            ☁️ Supabase             │
│                      │  HTTPS │  ┌─────────┐  ┌──────────────────┐  │
│  Fragments + XML     │◄──────►│  │  Auth   │  │ Postgres + RLS   │  │
│  supabase-kt         │        │  └─────────┘  │ Triggers/Funções │  │
│  Realtime (WebSocket)│◄──────►│  Realtime ◄───┤ Chat e Presença  │  │
│                      │        │               └──────────────────┘  │
│                      │        │  ┌──────────────────────────────┐   │
│                      │───────►│  │ Edge Functions (Deno/TS)     │   │
└──────────────────────┘        │  │ Pix · Webhook Mercado Pago   │   │
                               │  └──────────────┬───────────────┘   │
                               └─────────────────┼───────────────────┘
                                                 ▼
                                        💳 Mercado Pago (Pix)
```

### 🔐 Autenticação e segurança

O login e o cadastro são realizados pelo Supabase Auth, utilizando e-mail e senha.

Cada usuário possui um nível de permissão que determina quais funcionalidades e informações podem ser acessadas.

O banco utiliza **Row Level Security (RLS)** para restringir o acesso aos registros, impedindo, por exemplo, que um aluno consulte ou altere informações privadas de outros usuários.

### ⚡ Atualizações em tempo real

O Supabase Realtime é utilizado para sincronizar mensagens e outras informações entre os dispositivos conectados.

- **Chat:** mensagens recebidas por eventos `postgres_changes`, com sincronização periódica complementar.
- **Presença online:** identificação de usuários ativos nos últimos dois minutos.
- **Notificações:** criação automática por gatilhos no banco, com intervalo de 30 minutos para determinados avisos de mensagens.
- **Aulas:** atualização de inscrições e disponibilidade de vagas.

### 💳 Pagamentos por Pix

A integração com o Mercado Pago permite gerar cobranças Pix diretamente pelo aplicativo.

O pagamento é confirmado pelo backend, que consulta o estado da transação antes de atualizar o plano do aluno.

O sistema também trata notificações duplicadas para evitar renovações indevidas e mantém o dia de referência do vencimento da mensalidade.

### 🎬 Vídeos dos exercícios

Os vídeos demonstrativos utilizam a **YouTube IFrame Player API**, integrada a uma WebView Android.

O player permite controlar a reprodução, pausar vídeos e utilizar o modo de tela cheia.

---

## 🛠️ Tecnologias utilizadas

| Categoria | Tecnologias |
|---|---|
| 📱 **Android** | Kotlin, XML Views, Fragments, RecyclerView, Material Components |
| ☁️ **Backend** | Supabase Auth, PostgreSQL, Realtime e Edge Functions |
| 🔐 **Segurança** | Row Level Security (RLS), autenticação e controle de permissões |
| 📚 **Bibliotecas** | supabase-kt, Ktor/OkHttp, kotlinx.serialization, MPAndroidChart |
| 💳 **Pagamentos** | Mercado Pago API (Pix) |
| 🎬 **Vídeos** | YouTube IFrame API, WebView |
| 📲 **Compatibilidade** | Android 8.0 (API 26) ou superior |

---

## 📂 Estrutura do projeto

```text
📦 UniFIT
 ┣ 📂 app/src/main/
 ┃ ┣ 📂 java/.../unifit/
 ┃ ┃ ┣ 📂 auth          → Login, cadastro e recuperação de senha
 ┃ ┃ ┣ 📂 cliente       → Funcionalidades do aluno
 ┃ ┃ ┣ 📂 admin         → Funcionalidades do professor
 ┃ ┃ ┣ 📂 server        → Integração com Supabase
 ┃ ┃ ┗ 📂 utilitarios   → Componentes reutilizáveis
 ┃ ┗ 📂 res             → Layouts, imagens e recursos
 ┣ 📂 supabase
 ┃ ┣ 📜 *.sql           → Tabelas, funções, triggers e RLS
 ┃ ┗ 📂 functions      → Edge Functions de pagamento
 ┗ 📜 README.md
```

---

## 🚀 Como executar

### 1. Configurar o Supabase

1. Crie um projeto no [Supabase](https://supabase.com).
2. Ative a autenticação por e-mail.
3. Acesse o SQL Editor e execute os scripts da pasta [`supabase/`](supabase/), respeitando a ordem de dependências.
4. Opcionalmente, execute `dados_exemplo.sql` para inserir dados de teste.

### 2. Configurar o aplicativo

1. Clone o repositório e abra o projeto no Android Studio.
2. No arquivo `server/Supabase.kt`, configure a URL e a chave pública do projeto Supabase.
3. Sincronize as dependências do Gradle.
4. Execute o aplicativo em um dispositivo ou emulador Android.

Também é possível gerar um APK de desenvolvimento utilizando:

```bash
./gradlew :app:assembleDebug
```

### 3. Configurar os pagamentos (opcional)

Para utilizar os pagamentos por Pix:

1. Crie uma aplicação no Mercado Pago.
2. Configure o segredo `MP_ACCESS_TOKEN` no Supabase.
3. Publique as Edge Functions responsáveis pelos pagamentos.
4. Configure o webhook para receber atualizações das transações.

As instruções completas estão disponíveis em [`supabase/PIX_MERCADO_PAGO.md`](supabase/PIX_MERCADO_PAGO.md).

> **Acesso administrativo:** contas de professores devem receber a permissão `is_admin = true` por meio de uma operação administrativa autorizada. O cadastro convencional cria contas de alunos.

---

## 🛡️ Segurança

Algumas medidas adotadas no projeto:

- 🔐 **RLS:** restrição de acesso aos dados conforme o usuário autenticado.
- 👤 **Permissões:** separação entre funcionalidades de alunos e professores.
- 🔑 **Credenciais:** tokens privados e chaves administrativas são mantidos no backend.
- 💳 **Pagamentos:** confirmação realizada pelo servidor, com proteção contra processamento duplicado.
- 🗄️ **Banco de dados:** restrições e funções para proteger operações sensíveis.

> **Importante:** não publique tokens privados do Mercado Pago, a chave `service_role` do Supabase ou arquivos locais com credenciais. O aplicativo Android utiliza apenas a chave pública do Supabase.

---

<p align="center">
  Desenvolvido com Kotlin, Android Studio e Supabase.
</p>
