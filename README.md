# StudyWise

[English](#english) | [Português](#português)

---

## English

StudyWise is a modern Android application designed to help users create, manage, and take quizzes efficiently. Built with the latest Android development practices, it provides a seamless learning experience with features like real-time quiz creation, performance tracking, and detailed review of past attempts.

### Features

- **User Authentication**: Secure login and sign-up using Appwrite.
- **Quiz Management**: Create custom quizzes with descriptions and multiple-choice questions.
- **Interactive Quizzing**: Take quizzes with a smooth UI, featuring a "question pile" stack-based navigation.
- **Review System**: Detailed review of quiz attempts, including explanations for correct and incorrect answers.
- **Search**: Discover quizzes by title or topic.
- **Offline Support**: Local data persistence using Room database.

### Tech Stack

- Jetpack Compose
- Kotlin Coroutines & Flow
- Hilt (Dependency Injection)
- Appwrite (Backend)
- Room (Local Database)
- MVVM Architecture

### Project Structure

- `ui/screens/`: UI logic for different screens (Home, Login, Create Quiz, Answer Quiz, Review Attempt, etc.).
- `viewmodels/`: Management of UI state and business logic.
- `data/repository/`: Data operations, abstracting remote and local data sources.
- `data/db/`: Room database configuration and entities.
- `ui/components/`: Reusable UI components.

### Getting Started

1. **Clone the repository**:
   ```bash
   git clone https://github.com/yourusername/StudyWise.git
   ```
2. **Setup Appwrite**:
   - Ensure you have an Appwrite instance running.
   - Update `Appwrite.kt` with your Project ID and Endpoint.
3. **Build and Run**:
   - Open the project in Android Studio.
   - Sync Gradle and run the `:app` module.

---

## Português

O StudyWise é um aplicativo Android moderno projetado para ajudar os usuários a criar, gerenciar e realizar quizzes de forma eficiente. Desenvolvido com as práticas mais recentes de desenvolvimento Android, ele oferece uma experiência de aprendizado fluida, com recursos como criação de quizzes em tempo real, acompanhamento de desempenho e revisão detalhada de tentativas anteriores.

### Funcionalidades

- **Autenticação de Usuário**: Login e cadastro seguros usando Appwrite.
- **Gerenciamento de Quizzes**: Crie quizzes personalizados com descrições e perguntas de múltipla escolha.
- **Quizzes Interativos**: Realize quizzes com uma interface suave, apresentando uma navegação baseada em pilha de questões.
- **Sistema de Revisão**: Revisão detalhada das tentativas, incluindo explicações para respostas corretas e incorretas.
- **Busca**: Descubra quizzes por título ou tópico.
- **Suporte Offline**: Persistência de dados local usando o banco de dados Room.

### Tecnologias

- Jetpack Compose
- Kotlin Coroutines & Flow
- Hilt (Injeção de Dependência)
- Appwrite (Backend)
- Room (Banco de dados local)
- Arquitetura MVVM

### Estrutura do Projeto

- `ui/screens/`: Lógica de interface para as diferentes telas (Início, Login, Criar Quiz, Responder Quiz, Revisar Tentativa, etc.).
- `viewmodels/`: Gerenciamento de estado da interface e lógica de negócios.
- `data/repository/`: Operações de dados, abstraindo fontes remotas e locais.
- `data/db/`: Configuração e entidades do banco de dados Room.
- `ui/components/`: Componentes de interface reutilizáveis.

### Começando

1. **Clonar o repositório**:
   ```bash
   git clone https://github.com/seuusuario/StudyWise.git
   ```
2. **Configurar Appwrite**:
   - Certifique-se de ter uma instância do Appwrite rodando.
   - Atualize o arquivo `Appwrite.kt` com seu ID de Projeto e Endpoint.
3. **Compilar e Rodar**:
   - Abra o projeto no Android Studio.
   - Sincronize o Gradle e execute o módulo `:app`.
