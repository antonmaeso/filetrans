# Vue Frontend UI - Design Document

## Overview

The Vue Frontend UI is a single-page application (SPA) that provides a web-based interface for the file transfer system. It enables users to create and monitor file transfers, browse transferred files organized by date, view detailed metadata including AI analysis results, and search through their media library.

The frontend is built with Vue 3 using the Composition API and TypeScript for type safety. It communicates with the Spring Boot backend through REST APIs and implements a responsive design that works across desktop, tablet, and mobile devices.

### Key Design Goals

1. **User Experience**: Provide an intuitive, responsive interface with clear feedback and error handling
2. **Real-time Updates**: Poll backend APIs to show live transfer progress and metadata processing status
3. **Performance**: Implement caching and pagination to handle large file libraries efficiently
4. **Maintainability**: Use TypeScript, component-based architecture, and centralized state management
5. **Extensibility**: Design API client and state management to easily accommodate new features

### Technology Stack

- **Framework**: Vue 3 with Composition API
- **Language**: TypeScript
- **Build Tool**: Vite
- **State Management**: Pinia
- **Routing**: Vue Router
- **HTTP Client**: Axios
- **UI Components**: Custom components with scoped CSS
- **Development**: ESLint, Prettier for code quality

## Architecture

### High-Level Architecture

The frontend follows a layered architecture:

```
┌─────────────────────────────────────────────────────┐
│                    Views Layer                       │
│  (Dashboard, NewTransfer, TransferHistory,          │
│   FileBrowser, FileDetails, Settings)               │
└─────────────────────────────────────────────────────┘
                        │
┌─────────────────────────────────────────────────────┐
│                 Components Layer                     │
│  (FileCard, TransferStatusBadge, LoadingSpinner,   │
│   ErrorMessage, SearchBar, etc.)                    │
└─────────────────────────────────────────────────────┘
                        │
┌─────────────────────────────────────────────────────┐
│              State Management (Pinia)                │
│  (TransferStore, FileStore, SettingsStore)          │
└─────────────────────────────────────────────────────┘
                        │
┌─────────────────────────────────────────────────────┐
│                  API Client Layer                    │
│  (Axios instance with interceptors)                 │
└─────────────────────────────────────────────────────┘
                        │
┌─────────────────────────────────────────────────────┐
│              Spring Boot Backend                     │
│  (Transfer, Metadata, AI modules)                   │
└─────────────────────────────────────────────────────┘
```

### Directory Structure

```
frontend/
├── src/
│   ├── api/              # API client and type definitions
│   │   ├── client.ts     # Axios instance with interceptors
│   │   ├── transfers.ts  # Transfer API methods
│   │   ├── files.ts      # File API methods
│   │   └── types.ts      # TypeScript interfaces for API
│   ├── components/       # Reusable components
│   │   ├── FileCard.vue
│   │   ├── TransferStatusBadge.vue
│   │   ├── LoadingSpinner.vue
│   │   ├── ErrorMessage.vue
│   │   └── SearchBar.vue
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
│   │   ├── formatters.ts # Date, size formatting
│   │   └── validators.ts # Form validation
│   ├── App.vue           # Root component
│   └── main.ts           # Application entry point
├── public/               # Static assets
├── index.html            # HTML template
├── vite.config.ts        # Vite configuration
├── tsconfig.json         # TypeScript configuration
├── package.json          # Dependencies and scripts
└── README.md             # Setup instructions
```

### Routing Strategy

The application uses Vue Router with the following routes:

- `/` - Dashboard (default view)
- `/transfers/new` - Create new transfer form
- `/transfers` - Transfer history list
- `/transfers/:executionId` - Transfer details
- `/files` - File browser with date hierarchy
- `/files/:id` - File details with metadata
- `/settings` - User settings
- `*` - 404 Not Found page

All routes use lazy loading to optimize initial bundle size.

### State Management Strategy

Pinia stores manage application state with the following responsibilities:

**TransferStore**:
- Maintains list of transfers with pagination
- Polls running transfers for status updates
- Handles create, delete, and retry operations
- Caches transfer data with 60-second TTL

**FileStore**:
- Maintains file list with date hierarchy
- Handles search and filter operations
- Caches file metadata with 60-second TTL
- Manages thumbnail loading state

**SettingsStore**:
- Persists user preferences to localStorage
- Provides default values for transfer forms
- Stores API base URL configuration

## Components and Interfaces

### View Components

#### Dashboard.vue
Displays overview of recent transfers and system statistics.

**Props**: None

**State**:
- Recent transfers (from TransferStore)
- System statistics (computed from stores)

**Behavior**:
- Loads 10 most recent transfers on mount
- Polls running transfers every 5 seconds
- Navigates to transfer details on row click

#### NewTransfer.vue
Form for creating new file transfers.

**Props**: None

**State**:
- Form data (sourceDir, targetBaseDir, extensions, filePath)
- Validation errors
- Submission loading state
- Autocomplete suggestions (from SettingsStore)

**Behavior**:
- Pre-populates form with saved defaults
- Validates inputs before submission
- Calls TransferStore.createTransfer()
- Navigates to transfer status on success
- Displays field-specific errors on validation failure

#### TransferHistory.vue
Paginated list of all transfer jobs.

**Props**: None

**State**:
- Transfer list (from TransferStore)
- Pagination state (page, size, totalPages)
- Polling intervals for running transfers

**Behavior**:
- Loads transfers with pagination
- Polls running transfers every 3 seconds
- Provides retry button for failed transfers
- Provides delete button for completed/failed transfers
- Navigates to transfer details on row click

#### FileBrowser.vue
Hierarchical view of transferred files organized by date.

**Props**: None

**State**:
- File tree structure (from FileStore)
- Expanded folders
- Pagination state
- Thumbnail loading states

**Behavior**:
- Loads files organized as YYYY/YYYY-MM-DD
- Expands/collapses date folders
- Displays thumbnails for images
- Paginates with 50 files per page
- Navigates to file details on file click

#### FileDetails.vue
Detailed view of a single file with metadata.

**Props**:
- `id: string` (from route params)

**State**:
- File metadata (from FileStore)
- Loading state
- Metadata polling interval

**Behavior**:
- Loads file metadata on mount
- Displays full-size image preview for images
- Shows EXIF data if available
- Shows AI analysis results if available
- Polls every 2 seconds if metadata not yet available

#### Settings.vue
User configuration page.

**Props**: None

**State**:
- Settings form data (from SettingsStore)

**Behavior**:
- Loads current settings on mount
- Saves to localStorage on submit
- Provides clear settings button
- Displays current API base URL

### Reusable Components

#### FileCard.vue
Displays file information with thumbnail.

**Props**:
```typescript
interface FileCardProps {
  file: {
    id: string;
    filename: string;
    size: number;
    transferDate: string;
    thumbnailUrl?: string;
  };
}
```

**Events**:
- `click` - Emitted when card is clicked

#### TransferStatusBadge.vue
Displays transfer status with color coding.

**Props**:
```typescript
interface StatusBadgeProps {
  status: 'COMPLETED' | 'RUNNING' | 'FAILED' | 'STOPPED' | 'STARTING' | 'STARTED' | 'STOPPING' | 'ABANDONED' | 'UNKNOWN';
}
```

**Styling**:
- COMPLETED: green
- RUNNING/STARTING/STARTED: blue
- FAILED: red
- STOPPED/STOPPING: orange
- ABANDONED/UNKNOWN: gray

#### LoadingSpinner.vue
Displays loading indicator.

**Props**:
```typescript
interface LoadingSpinnerProps {
  size?: 'small' | 'medium' | 'large';
  message?: string;
}
```

#### ErrorMessage.vue
Displays error messages with optional retry action.

**Props**:
```typescript
interface ErrorMessageProps {
  message: string;
  retryable?: boolean;
}
```

**Events**:
- `retry` - Emitted when retry button is clicked

#### SearchBar.vue
Search input with debouncing.

**Props**:
```typescript
interface SearchBarProps {
  placeholder?: string;
  debounceMs?: number; // default 500
}
```

**Events**:
- `search` - Emitted with search query after debounce

### API Client Interfaces

#### TypeScript Type Definitions

```typescript
// Transfer types
interface CreateTransferRequest {
  sourceDir?: string;
  targetBaseDir: string;
  filePath?: string;
  extensions?: string[];
}

interface TransferResponse {
  executionId: number;
  status: TransferStatus;
  startTime?: string;
  endTime?: string;
}

type TransferStatus = 
  | 'STARTING' 
  | 'STARTED' 
  | 'STOPPING' 
  | 'STOPPED' 
  | 'FAILED' 
  | 'COMPLETED' 
  | 'ABANDONED' 
  | 'UNKNOWN';

interface TransferListItem {
  executionId: number;
  sourcePath: string;
  targetPath: string;
  status: TransferStatus;
  startTime: string;
  endTime?: string;
  fileCount: number;
}

interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
}

// File types
interface FileListItem {
  id: string;
  path: string;
  filename: string;
  size: number;
  transferDate: string;
  thumbnailUrl?: string;
}

interface FileMetadata {
  id: string;
  path: string;
  filename: string;
  size: number;
  transferDate: string;
  sourcePath: string;
  hash: string;
  hashAlgorithm: string;
  exif?: ExifData;
  aiAnalysis?: AiAnalysisData;
}

interface ExifData {
  cameraModel?: string;
  captureDate?: string;
  gpsLatitude?: number;
  gpsLongitude?: number;
  focalLength?: string;
  aperture?: string;
  iso?: number;
  shutterSpeed?: string;
}

interface AiAnalysisData {
  description: string;
  tags: string[];
  confidence: number;
}

// Search types
interface SearchParams {
  q?: string;
  tags?: string[];
  dateFrom?: string;
  dateTo?: string;
  page?: number;
  size?: number;
}

// Error types
interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}
```

## Data Models

### Frontend State Models

#### Transfer Store State

```typescript
interface TransferState {
  transfers: TransferListItem[];
  currentTransfer: TransferResponse | null;
  pagination: {
    page: number;
    size: number;
    totalPages: number;
    totalElements: number;
  };
  loading: boolean;
  error: string | null;
  pollingIntervals: Map<number, number>; // executionId -> intervalId
}
```

#### File Store State

```typescript
interface FileState {
  files: FileListItem[];
  fileTree: DateHierarchy;
  currentFile: FileMetadata | null;
  searchResults: FileListItem[];
  pagination: {
    page: number;
    size: number;
    totalPages: number;
    totalElements: number;
  };
  loading: boolean;
  error: string | null;
  metadataCache: Map<string, CachedMetadata>;
}

interface DateHierarchy {
  [year: string]: {
    [date: string]: FileListItem[];
  };
}

interface CachedMetadata {
  data: FileMetadata;
  timestamp: number;
  ttl: number; // milliseconds
}
```

#### Settings Store State

```typescript
interface SettingsState {
  defaultSourceDir: string;
  defaultTargetBaseDir: string;
  apiBaseUrl: string;
  recentSourceDirs: string[];
  recentTargetDirs: string[];
}
```

### Backend API Contracts

The frontend requires the following backend endpoints. Some already exist, others need to be implemented:

#### Existing Endpoints

1. **POST /transfers** - Create transfer (exists)
2. **GET /transfers/{executionId}** - Get transfer status (exists)
3. **POST /api/metadata/analyze** - Trigger metadata analysis (exists)
4. **POST /ai/analyze** - Trigger AI analysis (exists)

#### Missing Endpoints (Requirements 13-19)

5. **GET /transfers** - List all transfers with pagination
   - Query params: page, size, sort
   - Returns: PaginatedResponse<TransferListItem>

6. **GET /files** - List transferred files
   - Query params: page, size, dateFrom, dateTo, directory
   - Returns: PaginatedResponse<FileListItem>

7. **GET /files/{id}/metadata** - Get file metadata from sidecar
   - Returns: FileMetadata
   - 404 if sidecar doesn't exist

8. **GET /files/{id}/thumbnail** - Serve image thumbnail
   - Returns: image/jpeg (200x200px max)
   - 404 if not an image

9. **GET /files/search** - Search files
   - Query params: q, tags, dateFrom, dateTo, page, size
   - Returns: PaginatedResponse<FileListItem>

10. **DELETE /transfers/{executionId}** - Delete transfer record
    - Returns: 204 on success
    - 409 if transfer is running

11. **POST /transfers/{executionId}/retry** - Retry failed transfer
    - Returns: 201 with new TransferResponse
    - 400 if not in FAILED status

### Data Flow Examples

#### Creating a Transfer

```
User fills form → NewTransfer.vue validates
  → TransferStore.createTransfer(request)
  → API Client POST /transfers
  → Backend creates job
  → Returns TransferResponse
  → Store updates state
  → Router navigates to /transfers/{executionId}
  → TransferHistory.vue starts polling
```

#### Browsing Files

```
User navigates to /files → FileBrowser.vue mounts
  → FileStore.loadFiles()
  → API Client GET /files?page=0&size=50
  → Backend queries file system
  → Returns PaginatedResponse<FileListItem>
  → Store organizes into date hierarchy
  → Component renders tree structure
  → User expands date folder
  → Component renders files with thumbnails
  → Thumbnails loaded via GET /files/{id}/thumbnail
```

#### Viewing File Details

```
User clicks file → Router navigates to /files/{id}
  → FileDetails.vue mounts
  → FileStore.loadMetadata(id)
  → Check cache (60s TTL)
  → If expired: API Client GET /files/{id}/metadata
  → Backend reads sidecar JSON
  → Returns FileMetadata
  → Store caches result
  → Component displays metadata
  → If metadata incomplete: poll every 2s
```

#### Searching Files

```
User types in SearchBar → Debounce 500ms
  → Emits search event
  → FileStore.search(params)
  → API Client GET /files/search?q=...&tags=...
  → Backend searches filenames, AI descriptions, tags
  → Returns PaginatedResponse<FileListItem>
  → Store updates searchResults
  → Component displays results in grid
```

