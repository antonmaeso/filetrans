# Implementation Plan: Vue Frontend UI

## Overview

This plan implements a Vue 3 + TypeScript single-page application that provides a web interface for the file transfer system. The implementation is divided into frontend development (Vue/TypeScript) and backend API development (Java/Spring Boot). Tasks are ordered to build incrementally, starting with project setup, then core infrastructure, followed by features, and finally backend APIs.

## Tasks

- [x] 1. Initialize Vue project with Vite and TypeScript
  - Create new Vite project with Vue 3 and TypeScript template
  - Install dependencies: vue-router, pinia, axios
  - Configure vite.config.ts with proxy to http://localhost:8080
  - Set up tsconfig.json for strict type checking
  - Add ESLint and Prettier configurations
  - Create basic directory structure (api/, components/, views/, stores/, router/, utils/)
  - _Requirements: 21.1, 21.2, 21.3, 21.4, 21.5, 21.7_

- [ ] 2. Implement API client layer with TypeScript types
  - [x] 2.1 Define TypeScript interfaces for all API types
    - Create src/api/types.ts with interfaces for CreateTransferRequest, TransferResponse, TransferStatus, TransferListItem, PaginatedResponse, FileListItem, FileMetadata, ExifData, AiAnalysisData, SearchParams, ApiError
    - _Requirements: 10.2_
  
  - [x] 2.2 Create Axios client with interceptors
    - Create src/api/client.ts with Axios instance
    - Configure base URL from environment variable (import.meta.env.VITE_API_BASE_URL)
    - Add request interceptor for authentication headers
    - Add response interceptor for error handling (4xx, 5xx, network errors)
    - Set 30-second timeout for all requests
    - _Requirements: 10.1, 10.3, 10.4, 10.6, 10.7_
  
  - [x] 2.3 Implement transfer API methods
    - Create src/api/transfers.ts with methods: createTransfer, getTransferStatus, listTransfers, deleteTransfer, retryTransfer
    - Use TypeScript interfaces for request/response types
    - _Requirements: 10.5, 2.2, 3.2, 3.7, 3.8_
  
  - [x] 2.4 Implement file API methods
    - Create src/api/files.ts with methods: listFiles, getFileMetadata, getThumbnailUrl, searchFiles
    - Use TypeScript interfaces for request/response types
    - _Requirements: 10.5, 4.1, 5.1, 6.2_

- [x] 3. Set up routing with Vue Router
  - Create src/router/index.ts with route definitions
  - Define routes: / (Dashboard), /transfers/new (NewTransfer), /transfers (TransferHistory), /transfers/:executionId (TransferDetails), /files (FileBrowser), /files/:id (FileDetails), /settings (Settings), /* (NotFound)
  - Configure lazy loading for all route components
  - Set up navigation guards if needed
  - _Requirements: 12.1, 12.2, 12.4, 12.6_

- [ ] 4. Create Pinia stores for state management
  - [x] 4.1 Implement TransferStore
    - Create src/stores/transfers.ts with state, getters, and actions
    - State: transfers list, currentTransfer, pagination, loading, error, pollingIntervals
    - Actions: loadTransfers, loadTransferStatus, createTransfer, deleteTransfer, retryTransfer, startPolling, stopPolling
    - Implement 60-second cache TTL for transfer data
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 3.6_
  
  - [x] 4.2 Implement FileStore
    - Create src/stores/files.ts with state, getters, and actions
    - State: files list, fileTree (date hierarchy), currentFile, searchResults, pagination, loading, error, metadataCache
    - Actions: loadFiles, loadFileMetadata, searchFiles, buildDateHierarchy
    - Implement 60-second cache TTL for metadata
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 5.7_
  
  - [x] 4.3 Implement SettingsStore
    - Create src/stores/settings.ts with state, getters, and actions
    - State: defaultSourceDir, defaultTargetBaseDir, apiBaseUrl, recentSourceDirs, recentTargetDirs
    - Actions: loadSettings, saveSettings, clearSettings, addRecentPath
    - Persist to localStorage on save, load on init
    - _Requirements: 11.1, 11.2, 7.2, 7.3_

- [ ] 5. Build reusable UI components
  - [x] 5.1 Create LoadingSpinner component
    - Create src/components/LoadingSpinner.vue
    - Props: size ('small' | 'medium' | 'large'), message (optional)
    - Implement CSS spinner animation
    - _Requirements: 20.2, 8.1_
  
  - [x] 5.2 Create ErrorMessage component
    - Create src/components/ErrorMessage.vue
    - Props: message (string), retryable (boolean)
    - Emit 'retry' event when retry button clicked
    - _Requirements: 20.2, 8.2, 8.3, 8.4_
  
  - [x] 5.3 Create TransferStatusBadge component
    - Create src/components/TransferStatusBadge.vue
    - Props: status (TransferStatus enum)
    - Color coding: COMPLETED (green), RUNNING/STARTING/STARTED (blue), FAILED (red), STOPPED/STOPPING (orange), ABANDONED/UNKNOWN (gray)
    - _Requirements: 20.2, 3.5_
  
  - [x] 5.4 Create FileCard component
    - Create src/components/FileCard.vue
    - Props: file (FileListItem interface)
    - Display thumbnail, filename, size, transfer date
    - Emit 'click' event when card clicked
    - _Requirements: 20.2, 4.4_
  
  - [x] 5.5 Create SearchBar component
    - Create src/components/SearchBar.vue
    - Props: placeholder (string), debounceMs (number, default 500)
    - Implement debounced input with composable or setTimeout
    - Emit 'search' event with query string
    - _Requirements: 20.2, 6.1, 6.7_

- [x] 6. Implement Dashboard view
  - Create src/views/Dashboard.vue
  - Use TransferStore to load 10 most recent transfers
  - Display transfer list with execution ID, start time, status badge, file count
  - Implement polling for running transfers (every 5 seconds)
  - Display system statistics (computed from store data)
  - Navigate to transfer details on row click
  - Use LoadingSpinner and ErrorMessage components
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [x] 7. Implement NewTransfer view
  - Create src/views/NewTransfer.vue
  - Form fields: sourceDir, targetBaseDir, filePath, extensions (multi-select or comma-separated)
  - Load default values from SettingsStore
  - Implement autocomplete for source/target paths using recent paths
  - Client-side validation: required fields, path format
  - Call TransferStore.createTransfer on submit
  - Display field-specific validation errors
  - Navigate to /transfers/{executionId} on success
  - Use LoadingSpinner during submission
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

- [x] 8. Implement TransferHistory view
  - Create src/views/TransferHistory.vue
  - Use TransferStore to load paginated transfer list
  - Display table with columns: execution ID, source, target, status badge, start time, end time, file count
  - Implement pagination controls (previous, next, page numbers)
  - Poll running transfers every 3 seconds
  - Display retry button for FAILED transfers
  - Display delete button for COMPLETED/FAILED transfers
  - Navigate to transfer details on row click
  - Clean up polling intervals on unmount
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8_

- [x] 9. Implement FileBrowser view
  - Create src/views/FileBrowser.vue
  - Use FileStore to load files and build date hierarchy
  - Display hierarchical tree: YYYY folders containing YYYY-MM-DD folders
  - Implement expand/collapse for date folders
  - Display files using FileCard component with thumbnails
  - Implement pagination (50 files per page)
  - Load thumbnails via getThumbnailUrl for images (JPG, JPEG, PNG)
  - Navigate to file details on file click
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7_

- [x] 10. Implement FileDetails view
  - Create src/views/FileDetails.vue
  - Get file ID from route params
  - Use FileStore to load file metadata
  - Display file path, size, transfer date, source path, hash
  - Display EXIF data if available (camera model, capture date, GPS, focal length, aperture, ISO, shutter speed)
  - Display AI analysis if available (description, tags, confidence score)
  - Display full-size image preview for image files
  - Implement polling (every 2 seconds) if metadata not yet available
  - Clean up polling interval on unmount
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7_

- [x] 11. Implement search functionality
  - Add SearchBar component to MainLayout or FileBrowser
  - Implement date range picker for filtering
  - Implement tag selector for filtering (multi-select)
  - Call FileStore.searchFiles with query params
  - Display search results in grid layout with FileCard components
  - Display "no results" message when search returns empty
  - Clear search results when navigating away
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7_

- [x] 12. Implement Settings view
  - Create src/views/Settings.vue
  - Form fields: defaultSourceDir, defaultTargetBaseDir
  - Load current settings from SettingsStore on mount
  - Save to SettingsStore (persists to localStorage) on submit
  - Display current API base URL (read-only)
  - Provide "Clear Settings" button to reset defaults
  - Display success toast on save
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 13. Implement main layout and navigation
  - Create src/layouts/MainLayout.vue
  - Navigation bar with links to Dashboard, New Transfer, Transfer History, File Browser, Settings
  - Highlight active route in navigation
  - Implement responsive hamburger menu for mobile (width < 768px)
  - Display connection error banner when backend unreachable
  - Include toast notification container for success/error messages
  - _Requirements: 12.3, 12.5, 9.2, 8.7_

- [x] 14. Add responsive design and styling
  - Implement responsive CSS for all components and views
  - Support screen widths from 320px to 2560px
  - Switch FileBrowser from grid to list layout on mobile
  - Use touch-friendly button sizes (minimum 44x44px) on mobile
  - Test layouts on Chrome, Firefox, Safari, Edge
  - Add CSS variables for theme colors and spacing
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_

- [x] 15. Implement error handling and user feedback
  - Add toast notification system (use library or custom component)
  - Display loading indicators during all API calls
  - Display toast on network errors with retry option
  - Display error messages from 4xx responses
  - Display generic error for 5xx responses
  - Display success toasts for create, delete, retry actions
  - Log all API errors to console
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6_

- [x] 16. Create utility functions
  - Create src/utils/formatters.ts with functions: formatDate, formatFileSize, formatDuration
  - Create src/utils/validators.ts with functions: validatePath, validateExtensions, validateRequired
  - Use formatters in all views and components
  - Use validators in NewTransfer form
  - _Requirements: 2.5, 4.4, 5.2_

- [x] 17. Set up App.vue and main.ts
  - Create src/App.vue with router-view and MainLayout
  - Create src/main.ts with Vue app initialization
  - Register Pinia and Vue Router
  - Mount app to #app element
  - Add global error handler for uncaught errors
  - _Requirements: 21.1_

- [x] 18. Checkpoint - Test frontend functionality
  - Ensure all views render without errors
  - Test navigation between all routes
  - Test form validation and submission
  - Test responsive design on different screen sizes
  - Ensure all tests pass, ask the user if questions arise

- [ ] 19. Implement backend API - List Transfers (GET /transfers)
  - [x] 19.1 Create TransferListResponse DTO in transfer module
    - Add fields: executionId, sourcePath, targetPath, status, startTime, endTime, fileCount
    - Use OpenAPI spec or manual DTO creation
    - _Requirements: 13.4_
  
  - [x] 19.2 Create endpoint in FileTransferController
    - Add GET /transfers method with @RequestParam for page, size, sort
    - Query Spring Batch JobRepository for job executions
    - Map JobExecution to TransferListResponse
    - Return Page<TransferListResponse> with pagination metadata
    - _Requirements: 13.1, 13.2, 13.3, 13.5_
  
  - [ ]* 19.3 Write integration test for list transfers endpoint
    - Test pagination parameters
    - Test sorting by startTime descending
    - Test response structure matches PaginatedResponse
    - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5_

- [ ] 20. Implement backend API - List Files (GET /files)
  - [x] 20.1 Create FileListResponse DTO in metadata module
    - Add fields: id, path, filename, size, transferDate, thumbnailUrl
    - _Requirements: 14.3_
  
  - [x] 20.2 Create FilesController in metadata module
    - Add GET /files method with @RequestParam for page, size, dateFrom, dateTo, directory
    - Scan target directory for files
    - Filter by date range if provided
    - Organize by YYYY/YYYY-MM-DD structure
    - Return Page<FileListResponse>
    - _Requirements: 14.1, 14.2, 14.4, 14.5_
  
  - [ ]* 20.3 Write integration test for list files endpoint
    - Test pagination
    - Test date filtering
    - Test directory filtering
    - _Requirements: 14.1, 14.2, 14.3, 14.4, 14.5_

- [ ] 21. Implement backend API - Get File Metadata (GET /files/{id}/metadata)
  - [x] 21.1 Create FileMetadataResponse DTO in metadata module
    - Add fields: id, path, filename, size, transferDate, sourcePath, hash, hashAlgorithm, exif (nested), aiAnalysis (nested)
    - _Requirements: 15.3_
  
  - [x] 21.2 Add endpoint to FilesController
    - Add GET /files/{id}/metadata method
    - Read corresponding .metadata.json sidecar file
    - Parse JSON to FileMetadataResponse
    - Return 404 if sidecar doesn't exist
    - Return 200 with metadata on success
    - _Requirements: 15.1, 15.2, 15.4, 15.5_
  
  - [ ]* 21.3 Write integration test for get metadata endpoint
    - Test successful metadata retrieval
    - Test 404 when sidecar missing
    - Test EXIF data parsing
    - Test AI analysis data parsing
    - _Requirements: 15.1, 15.2, 15.3, 15.4, 15.5_

- [ ] 22. Implement backend API - Get File Thumbnail (GET /files/{id}/thumbnail)
  - [x] 22.1 Create ThumbnailService in metadata module
    - Implement thumbnail generation using Java ImageIO or external library
    - Generate 200x200px max thumbnails
    - Cache generated thumbnails in memory or disk
    - _Requirements: 16.2, 16.4_
  
  - [x] 22.2 Add endpoint to FilesController
    - Add GET /files/{id}/thumbnail method
    - Check if file is an image (JPG, JPEG, PNG)
    - Call ThumbnailService to generate or retrieve thumbnail
    - Return image/jpeg content type with byte array
    - Return 404 if not an image
    - _Requirements: 16.1, 16.3, 16.5, 16.6_
  
  - [ ]* 22.3 Write integration test for thumbnail endpoint
    - Test thumbnail generation for JPG files
    - Test 404 for non-image files
    - Test thumbnail dimensions
    - Test caching behavior
    - _Requirements: 16.1, 16.2, 16.3, 16.4, 16.5, 16.6_

- [ ] 23. Implement backend API - Search Files (GET /files/search)
  - [x] 23.1 Create SearchService in metadata module
    - Implement search logic: scan sidecar files for matching text
    - Search in: filename, AI description, AI tags
    - Support date range filtering
    - Support tag filtering (comma-separated)
    - _Requirements: 17.3_
  
  - [x] 23.2 Add endpoint to FilesController
    - Add GET /files/search method with @RequestParam for q, tags, dateFrom, dateTo, page, size
    - Call SearchService with parameters
    - Return Page<FileListResponse> matching search criteria
    - _Requirements: 17.1, 17.2, 17.4, 17.5_
  
  - [ ]* 23.3 Write integration test for search endpoint
    - Test text search in filenames
    - Test text search in AI descriptions
    - Test tag filtering
    - Test date range filtering
    - Test pagination
    - _Requirements: 17.1, 17.2, 17.3, 17.4, 17.5_

- [ ] 24. Implement backend API - Delete Transfer (DELETE /transfers/{executionId})
  - [x] 24.1 Add endpoint to FileTransferController
    - Add DELETE /transfers/{executionId} method
    - Check if transfer is currently running (query JobRepository)
    - Return 409 if running
    - Delete JobExecution and related records from Spring Batch tables
    - Return 204 on success
    - _Requirements: 18.1, 18.2, 18.3, 18.4, 18.5_
  
  - [ ]* 24.2 Write integration test for delete transfer endpoint
    - Test successful deletion of completed transfer
    - Test 409 when deleting running transfer
    - Test that files are not deleted
    - _Requirements: 18.1, 18.2, 18.3, 18.4, 18.5_

- [ ] 25. Implement backend API - Retry Transfer (POST /transfers/{executionId}/retry)
  - [x] 25.1 Add endpoint to FileTransferController
    - Add POST /transfers/{executionId}/retry method
    - Query JobRepository for original JobExecution
    - Check if status is FAILED
    - Return 400 if not FAILED
    - Extract original JobParameters
    - Launch new job with same parameters
    - Return 201 with new TransferResponse (new executionId)
    - _Requirements: 19.1, 19.2, 19.3, 19.4, 19.5_
  
  - [ ]* 25.2 Write integration test for retry transfer endpoint
    - Test successful retry of failed transfer
    - Test 400 when retrying non-failed transfer
    - Test that new execution has same parameters
    - _Requirements: 19.1, 19.2, 19.3, 19.4, 19.5_

- [x] 26. Update CORS configuration for frontend
  - Update Spring Boot CORS configuration to allow requests from http://localhost:5173 (Vite dev server)
  - Allow methods: GET, POST, DELETE
  - Allow headers: Content-Type, Authorization
  - _Requirements: 10.1_

- [x] 27. Create frontend README with setup instructions
  - Document prerequisites (Node.js version)
  - Document installation steps (npm install)
  - Document development commands (npm run dev, npm run build, npm run preview)
  - Document environment variables (VITE_API_BASE_URL)
  - Document project structure
  - _Requirements: 21.7_

- [x] 28. Final checkpoint - End-to-end testing
  - Start backend on port 8080
  - Start frontend dev server on port 5173
  - Test complete user flows: create transfer, monitor status, browse files, view details, search
  - Test error scenarios: network errors, validation errors, 404s
  - Test responsive design on mobile and desktop
  - Ensure all tests pass, ask the user if questions arise

## Notes

- Tasks marked with `*` are optional test tasks and can be skipped for faster MVP
- Frontend tasks (1-18) can be developed independently before backend APIs are ready
- Backend API tasks (19-25) implement the missing endpoints identified in Requirements 13-19
- The frontend uses TypeScript for type safety and Vue 3 Composition API for modern reactive patterns
- The backend follows Spring Boot best practices with proper separation between transfer, metadata, and ai modules
- All API endpoints should follow RESTful conventions and return appropriate HTTP status codes
- Pagination should be consistent across all list endpoints using Spring Data Page abstraction
