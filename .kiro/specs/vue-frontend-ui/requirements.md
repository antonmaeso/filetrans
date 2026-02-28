# Requirements Document

## Introduction

The Vue Frontend UI provides a web-based user interface for the file transfer application. It enables users to create and monitor file transfers, browse transferred files organized by date, view file metadata and AI analysis results, and search through their media library. The frontend communicates with the existing Spring Boot backend through REST APIs and identifies missing backend endpoints needed for a complete user experience.

## Glossary

- **Frontend**: The Vue.js single-page application that runs in the user's browser
- **Backend**: The Spring Boot application with transfer, metadata, and ai modules
- **Transfer_Job**: A Spring Batch job that moves files from source to target directories
- **Sidecar_File**: A JSON file (*.metadata.json) containing extracted metadata for a transferred file
- **Dashboard**: The main overview page showing recent transfers and system status
- **File_Browser**: A component that displays transferred files organized by date hierarchy
- **API_Client**: The frontend service layer that communicates with backend REST endpoints
- **Route**: A URL path in the single-page application that maps to a specific view
- **Component**: A reusable Vue.js UI element with template, logic, and styling
- **State_Store**: Centralized application state management (Pinia or Vuex)
- **Execution_ID**: Unique identifier for a Spring Batch job execution
- **Thumbnail**: A small preview image generated from the original file

## Requirements

### Requirement 1: Dashboard Overview

**User Story:** As a user, I want to see a dashboard with recent transfer activity, so that I can quickly understand the current state of my file transfers.

#### Acceptance Criteria

1. WHEN the Frontend loads, THE Dashboard SHALL display the 10 most recent Transfer_Jobs with their status
2. THE Dashboard SHALL display each Transfer_Job with execution ID, start time, status, and file count
3. WHEN a Transfer_Job is running, THE Dashboard SHALL update its progress every 5 seconds
4. THE Dashboard SHALL display system statistics including total files transferred and storage used
5. WHEN a user clicks on a Transfer_Job, THE Frontend SHALL navigate to the transfer details view

### Requirement 2: Create New Transfer

**User Story:** As a user, I want to create new file transfers through a form, so that I can move files from source to target directories.

#### Acceptance Criteria

1. THE Frontend SHALL provide a form with fields for source path, target base directory, and file extensions
2. WHEN a user submits the transfer form with valid inputs, THE API_Client SHALL POST to /transfers endpoint
3. WHEN the Backend returns a successful response, THE Frontend SHALL navigate to the transfer status page
4. IF the Backend returns a validation error, THEN THE Frontend SHALL display field-specific error messages
5. THE Frontend SHALL validate that source path and target directory are not empty before submission
6. WHERE the user has previously used source or target paths, THE Frontend SHALL offer autocomplete suggestions

### Requirement 3: Transfer History and Status

**User Story:** As a user, I want to view all my transfer jobs with their current status, so that I can track completed, running, and failed transfers.

#### Acceptance Criteria

1. THE Frontend SHALL display a paginated list of all Transfer_Jobs ordered by start time descending
2. WHEN a user requests the transfer history page, THE API_Client SHALL GET from /transfers endpoint
3. THE Frontend SHALL display each Transfer_Job with execution ID, source, target, status, start time, and completion time
4. WHEN a user clicks on a Transfer_Job row, THE Frontend SHALL navigate to detailed transfer view
5. THE Frontend SHALL display status badges with distinct colors for COMPLETED, RUNNING, FAILED, and STOPPED states
6. WHILE a Transfer_Job has RUNNING status, THE Frontend SHALL poll /transfers/{executionId} every 3 seconds
7. THE Frontend SHALL display a retry button for Transfer_Jobs with FAILED status
8. THE Frontend SHALL display a delete button for Transfer_Jobs with COMPLETED or FAILED status

### Requirement 4: File Browser

**User Story:** As a user, I want to browse transferred files organized by date, so that I can find files based on when they were transferred.

#### Acceptance Criteria

1. WHEN a user navigates to the file browser, THE API_Client SHALL GET from /files endpoint
2. THE File_Browser SHALL display files in a hierarchical tree structure organized as YYYY/YYYY-MM-DD
3. WHEN a user expands a date folder, THE File_Browser SHALL display all files transferred on that date
4. THE File_Browser SHALL display each file with thumbnail, filename, file size, and transfer timestamp
5. WHEN a user clicks on a file, THE Frontend SHALL navigate to the file details view
6. THE File_Browser SHALL support pagination with 50 files per page
7. WHERE a file is an image (JPG, JPEG, PNG), THE File_Browser SHALL display a Thumbnail from /files/{id}/thumbnail

### Requirement 5: File Details and Metadata

**User Story:** As a user, I want to view detailed information about a transferred file including its metadata and AI analysis, so that I can understand the file's content and properties.

#### Acceptance Criteria

1. WHEN a user navigates to file details, THE API_Client SHALL GET from /files/{id}/metadata endpoint
2. THE Frontend SHALL display the file's full path, size, transfer date, and original source path
3. WHERE the Sidecar_File contains EXIF data, THE Frontend SHALL display camera model, capture date, and GPS coordinates
4. WHERE the Sidecar_File contains AI analysis results, THE Frontend SHALL display description, tags, and confidence score
5. WHERE the file is an image, THE Frontend SHALL display a full-size preview
6. THE Frontend SHALL display file hash (MD5 or SHA-256) from the Sidecar_File
7. WHEN metadata is not yet available, THE Frontend SHALL display a loading indicator and poll every 2 seconds

### Requirement 6: Search and Filter

**User Story:** As a user, I want to search for files by tags, dates, and AI-generated descriptions, so that I can quickly find specific files in my library.

#### Acceptance Criteria

1. THE Frontend SHALL provide a search input field accessible from all pages
2. WHEN a user enters a search query and submits, THE API_Client SHALL GET from /files/search endpoint
3. THE Frontend SHALL support filtering by date range using a date picker component
4. THE Frontend SHALL support filtering by AI-generated tags using a tag selector
5. THE Frontend SHALL display search results in a grid layout with thumbnails and key metadata
6. WHEN search returns no results, THE Frontend SHALL display a helpful message
7. THE Frontend SHALL debounce search input by 500ms to avoid excessive API calls

### Requirement 7: Settings and Configuration

**User Story:** As a user, I want to configure default source and target directories, so that I don't have to type them repeatedly.

#### Acceptance Criteria

1. THE Frontend SHALL provide a settings page with fields for default source path and default target base directory
2. WHEN a user saves settings, THE Frontend SHALL store them in browser localStorage
3. WHEN a user opens the create transfer form, THE Frontend SHALL pre-populate fields with saved defaults
4. THE Frontend SHALL provide a button to clear saved settings
5. THE Frontend SHALL display the current API base URL in settings for debugging purposes

### Requirement 8: Error Handling and User Feedback

**User Story:** As a user, I want clear error messages and loading indicators, so that I understand what the application is doing and when something goes wrong.

#### Acceptance Criteria

1. WHEN any API call is in progress, THE Frontend SHALL display a loading indicator
2. IF an API call fails with a network error, THEN THE Frontend SHALL display a toast notification with retry option
3. IF the Backend returns a 4xx error, THEN THE Frontend SHALL display the error message from the response body
4. IF the Backend returns a 5xx error, THEN THE Frontend SHALL display a generic server error message
5. WHEN a user action succeeds (create transfer, delete job), THE Frontend SHALL display a success toast notification
6. THE Frontend SHALL log all API errors to the browser console for debugging
7. IF the Backend is unreachable, THEN THE Frontend SHALL display a connection error banner

### Requirement 9: Responsive Design

**User Story:** As a user, I want the interface to work well on different screen sizes, so that I can use it on desktop, tablet, and mobile devices.

#### Acceptance Criteria

1. THE Frontend SHALL use responsive CSS that adapts to screen widths from 320px to 2560px
2. WHEN viewed on mobile (width < 768px), THE Frontend SHALL display a hamburger menu for navigation
3. WHEN viewed on mobile, THE File_Browser SHALL switch from grid to list layout
4. THE Frontend SHALL use touch-friendly button sizes (minimum 44x44px) on mobile devices
5. THE Frontend SHALL test layouts on Chrome, Firefox, Safari, and Edge browsers

### Requirement 10: API Client Architecture

**User Story:** As a developer, I want a well-structured API client layer, so that backend communication is consistent and maintainable.

#### Acceptance Criteria

1. THE API_Client SHALL use Axios or Fetch API for HTTP requests
2. THE API_Client SHALL define TypeScript interfaces for all request and response types
3. THE API_Client SHALL include an interceptor that adds authentication headers to all requests
4. THE API_Client SHALL include an interceptor that handles common error responses
5. THE API_Client SHALL expose methods for each backend endpoint (createTransfer, getTransferStatus, listFiles, etc.)
6. THE API_Client SHALL use environment variables for the Backend base URL
7. THE API_Client SHALL implement request timeout of 30 seconds for all calls

### Requirement 11: State Management

**User Story:** As a developer, I want centralized state management, so that application state is predictable and easy to debug.

#### Acceptance Criteria

1. THE Frontend SHALL use Pinia for state management
2. THE State_Store SHALL maintain stores for transfers, files, and user settings
3. THE State_Store SHALL cache API responses to avoid redundant requests
4. WHEN cached data is older than 60 seconds, THE State_Store SHALL refetch from the Backend
5. THE State_Store SHALL provide actions for all data mutations
6. THE State_Store SHALL be accessible from all Components via composition API

### Requirement 12: Routing and Navigation

**User Story:** As a user, I want intuitive navigation between different sections of the application, so that I can easily access all features.

#### Acceptance Criteria

1. THE Frontend SHALL use Vue Router for client-side routing
2. THE Frontend SHALL define Routes for dashboard (/), new transfer (/transfers/new), transfer history (/transfers), file browser (/files), file details (/files/:id), and settings (/settings)
3. THE Frontend SHALL display a navigation bar with links to all main sections
4. WHEN a user navigates to an invalid Route, THE Frontend SHALL display a 404 page
5. THE Frontend SHALL highlight the active Route in the navigation bar
6. THE Frontend SHALL support browser back/forward navigation

### Requirement 13: Missing Backend API - List Transfers

**User Story:** As a frontend developer, I need an endpoint to list all transfers with pagination, so that I can display transfer history.

#### Acceptance Criteria

1. THE Backend SHALL provide GET /transfers endpoint
2. THE Backend SHALL accept query parameters: page (default 0), size (default 20), sort (default "startTime,desc")
3. THE Backend SHALL return a paginated response with content array, page number, total pages, and total elements
4. THE Backend SHALL include execution ID, source path, target path, status, start time, end time, and file count for each transfer
5. THE Backend SHALL return 200 status for successful requests

### Requirement 14: Missing Backend API - List Files

**User Story:** As a frontend developer, I need an endpoint to list transferred files with date organization, so that I can implement the file browser.

#### Acceptance Criteria

1. THE Backend SHALL provide GET /files endpoint
2. THE Backend SHALL accept query parameters: page, size, dateFrom, dateTo, directory
3. THE Backend SHALL return files with id, path, filename, size, transfer date, and thumbnail URL
4. THE Backend SHALL organize results by the date-based directory structure (YYYY/YYYY-MM-DD)
5. THE Backend SHALL return 200 status for successful requests

### Requirement 15: Missing Backend API - Get File Metadata

**User Story:** As a frontend developer, I need an endpoint to retrieve file metadata from sidecar files, so that I can display detailed file information.

#### Acceptance Criteria

1. THE Backend SHALL provide GET /files/{id}/metadata endpoint
2. THE Backend SHALL read the corresponding Sidecar_File for the requested file
3. THE Backend SHALL return metadata including file hash, EXIF data, and AI analysis results
4. IF the Sidecar_File does not exist, THEN THE Backend SHALL return 404 status
5. THE Backend SHALL return 200 status with JSON metadata for successful requests

### Requirement 16: Missing Backend API - Get File Thumbnail

**User Story:** As a frontend developer, I need an endpoint to serve image thumbnails, so that I can display preview images in the file browser.

#### Acceptance Criteria

1. THE Backend SHALL provide GET /files/{id}/thumbnail endpoint
2. WHERE the file is an image, THE Backend SHALL generate or retrieve a thumbnail with maximum dimensions 200x200px
3. THE Backend SHALL return the thumbnail as image/jpeg content type
4. THE Backend SHALL cache generated thumbnails to avoid repeated processing
5. IF the file is not an image, THEN THE Backend SHALL return 404 status
6. THE Backend SHALL return 200 status with image data for successful requests

### Requirement 17: Missing Backend API - Search Files

**User Story:** As a frontend developer, I need an endpoint to search files by various criteria, so that I can implement the search feature.

#### Acceptance Criteria

1. THE Backend SHALL provide GET /files/search endpoint
2. THE Backend SHALL accept query parameters: q (text query), tags (comma-separated), dateFrom, dateTo, page, size
3. THE Backend SHALL search in filenames, AI descriptions, and AI tags
4. THE Backend SHALL return results in the same format as GET /files endpoint
5. THE Backend SHALL return 200 status with matching files

### Requirement 18: Missing Backend API - Delete Transfer

**User Story:** As a frontend developer, I need an endpoint to delete transfer job records, so that users can clean up their transfer history.

#### Acceptance Criteria

1. THE Backend SHALL provide DELETE /transfers/{executionId} endpoint
2. THE Backend SHALL remove the Transfer_Job record from the database
3. THE Backend SHALL NOT delete the transferred files themselves
4. IF the Transfer_Job is currently running, THEN THE Backend SHALL return 409 status
5. THE Backend SHALL return 204 status for successful deletion

### Requirement 19: Missing Backend API - Retry Transfer

**User Story:** As a frontend developer, I need an endpoint to retry failed transfers, so that users can recover from transfer failures.

#### Acceptance Criteria

1. THE Backend SHALL provide POST /transfers/{executionId}/retry endpoint
2. THE Backend SHALL create a new Transfer_Job with the same parameters as the failed job
3. THE Backend SHALL return the new Execution_ID in the response
4. IF the original Transfer_Job is not in FAILED status, THEN THE Backend SHALL return 400 status
5. THE Backend SHALL return 201 status with the new execution details for successful retry

### Requirement 20: Component Architecture

**User Story:** As a developer, I want a well-organized component structure, so that the codebase is maintainable and components are reusable.

#### Acceptance Criteria

1. THE Frontend SHALL organize Components into directories: views (page-level), components (reusable), and layouts (page templates)
2. THE Frontend SHALL create reusable Components for FileCard, TransferStatusBadge, LoadingSpinner, ErrorMessage, and SearchBar
3. THE Frontend SHALL use Vue 3 Composition API for all Components
4. THE Frontend SHALL define prop types using TypeScript interfaces
5. THE Frontend SHALL emit custom events for child-to-parent communication
6. THE Frontend SHALL use scoped styles to prevent CSS conflicts

### Requirement 21: Build and Development Setup

**User Story:** As a developer, I want a proper build configuration, so that I can develop, test, and deploy the frontend efficiently.

#### Acceptance Criteria

1. THE Frontend SHALL use Vite as the build tool
2. THE Frontend SHALL include npm scripts for dev (development server), build (production build), and preview (preview production build)
3. THE Frontend SHALL use TypeScript for type safety
4. THE Frontend SHALL include ESLint and Prettier for code quality
5. THE Frontend SHALL proxy API requests to http://localhost:8080 during development
6. THE Frontend SHALL build static assets to a dist directory for deployment
7. THE Frontend SHALL include a README with setup and development instructions
