# File Transfer Frontend

A Vue 3 + TypeScript single-page application that provides a web interface for the file transfer system.

## Features

- Create and monitor file transfers
- Browse transferred files organized by date
- View detailed file metadata including EXIF data
- View AI analysis results (descriptions, tags, confidence scores)
- Search files by tags, dates, and AI-generated descriptions
- Responsive design for desktop, tablet, and mobile devices

## Technology Stack

- **Framework**: Vue 3 with Composition API
- **Language**: TypeScript
- **Build Tool**: Vite
- **State Management**: Pinia
- **Routing**: Vue Router
- **HTTP Client**: Axios
- **Code Quality**: ESLint, Prettier

## Prerequisites

- Node.js 18.x or higher
- npm 9.x or higher
- Backend API running on http://localhost:8080

## Installation

1. Install dependencies:

```bash
npm install
```

2. Copy the environment file and configure if needed:

```bash
cp .env.example .env
```

3. Update `.env` if your backend runs on a different port:

```
VITE_API_BASE_URL=http://localhost:8080
```

## Development

Start the development server:

```bash
npm run dev
```

The application will be available at http://localhost:5173

The dev server is configured with a proxy to forward API requests to the backend at http://localhost:8080.

## Building for Production

Build the application:

```bash
npm run build
```

The built files will be in the `dist/` directory.

Preview the production build:

```bash
npm run preview
```

## Code Quality

Run ESLint:

```bash
npm run lint
```

Fix ESLint issues automatically:

```bash
npm run lint:fix
```

Format code with Prettier:

```bash
npm run format
```

## Project Structure

```
frontend/
├── src/
│   ├── api/              # API client and type definitions
│   │   ├── client.ts     # Axios instance with interceptors
│   │   ├── transfers.ts  # Transfer API methods
│   │   ├── files.ts      # File API methods
│   │   └── types.ts      # TypeScript interfaces
│   ├── components/       # Reusable components
│   │   ├── FileCard.vue
│   │   ├── TransferStatusBadge.vue
│   │   ├── LoadingSpinner.vue
│   │   └── ...
│   ├── layouts/          # Page layouts
│   │   └── MainLayout.vue
│   ├── router/           # Vue Router configuration
│   │   └── index.ts
│   ├── stores/           # Pinia stores
│   │   ├── transfers.ts
│   │   ├── files.ts
│   │   └── settings.ts
│   ├── views/            # Page-level components
│   │   ├── Dashboard.vue
│   │   ├── NewTransfer.vue
│   │   ├── TransferHistory.vue
│   │   ├── FileBrowser.vue
│   │   ├── FileDetails.vue
│   │   └── Settings.vue
│   ├── utils/            # Utility functions
│   │   ├── formatters.ts
│   │   └── validators.ts
│   ├── App.vue           # Root component
│   └── main.ts           # Application entry point
├── public/               # Static assets
├── .env                  # Environment variables
├── .eslintrc.cjs         # ESLint configuration
├── .prettierrc.json      # Prettier configuration
├── vite.config.ts        # Vite configuration
├── tsconfig.json         # TypeScript configuration
└── package.json          # Dependencies and scripts
```

## API Proxy Configuration

During development, the Vite dev server proxies the following paths to the backend:

- `/api/*` → http://localhost:8080/api/*
- `/transfers/*` → http://localhost:8080/transfers/*
- `/files/*` → http://localhost:8080/files/*

This allows the frontend to make API calls without CORS issues during development.

## Environment Variables

- `VITE_API_BASE_URL`: Base URL for the backend API (default: http://localhost:8080)

## Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)

## Development Guidelines

- Use Vue 3 Composition API for all components
- Define prop types using TypeScript interfaces
- Use scoped styles to prevent CSS conflicts
- Follow the ESLint and Prettier configurations
- Keep components small and focused on a single responsibility
- Use Pinia stores for shared state management
- Implement proper error handling and loading states

## Troubleshooting

### Backend Connection Issues

If you see connection errors, ensure:

1. The backend is running on http://localhost:8080
2. The `.env` file has the correct `VITE_API_BASE_URL`
3. CORS is properly configured on the backend

### Build Errors

If you encounter TypeScript errors during build:

1. Run `npm run lint:fix` to fix auto-fixable issues
2. Check that all imports are correct
3. Ensure all TypeScript interfaces are properly defined

## License

This project is part of the file transfer system.
