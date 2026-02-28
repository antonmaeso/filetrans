# End-to-End Testing Guide

This document provides step-by-step instructions for testing the complete user flows of the File Transfer Frontend application.

## Prerequisites

Before starting the tests, ensure:

1. **Backend is running**: Start the Spring Boot backend on port 8080
   ```bash
   cd ..  # Navigate to project root
   ./mvnw spring-boot:run
   ```

2. **Frontend dev server is running**: Start the Vite dev server on port 5173
   ```bash
   npm run dev
   ```

3. **Test data**: Have some test files ready in a source directory for transfer testing

## Test Scenarios

### 1. Application Startup and Navigation

**Objective**: Verify the application loads correctly and navigation works.

**Steps**:
1. Open browser and navigate to http://localhost:5173
2. Verify the Dashboard page loads without errors
3. Check that the navigation bar displays all menu items:
   - Dashboard
   - New Transfer
   - Transfer History
   - File Browser
   - Settings
4. Click each navigation item and verify the corresponding page loads
5. Use browser back/forward buttons to verify navigation history works
6. Verify the active route is highlighted in the navigation bar

**Expected Results**:
- ✅ All pages load without console errors
- ✅ Navigation is smooth and responsive
- ✅ Active route is visually highlighted
- ✅ Browser navigation works correctly

---

### 2. Create New Transfer (Bulk Directory)

**Objective**: Test creating a transfer job for a directory of files.

**Steps**:
1. Navigate to "New Transfer" page
2. Fill in the form:
   - **Source Directory**: Enter path to a test directory with files (e.g., `/tmp/test-source`)
   - **Target Base Directory**: Enter target path (e.g., `/tmp/test-target`)
   - **File Extensions**: Enter `jpg, jpeg, png` (optional)
3. Click "Create Transfer" button
4. Verify success toast notification appears
5. Verify redirect to transfer details page with execution ID in URL
6. Observe transfer status updates (should show STARTING → STARTED → COMPLETED)

**Expected Results**:
- ✅ Form validation works (required fields)
- ✅ Success toast displays with execution ID
- ✅ Redirect to transfer status page occurs
- ✅ Transfer status updates in real-time
- ✅ File count increases as files are processed

---

### 3. Create New Transfer (Single File)

**Objective**: Test creating a transfer job for a single file.

**Steps**:
1. Navigate to "New Transfer" page
2. Fill in the form:
   - **File Path**: Enter path to a single test file (e.g., `/tmp/test-source/image.jpg`)
   - **Target Base Directory**: Enter target path (e.g., `/tmp/test-target`)
3. Verify Source Directory field is disabled
4. Click "Create Transfer" button
5. Verify success toast and redirect

**Expected Results**:
- ✅ Mutual exclusivity works (can't fill both Source Directory and File Path)
- ✅ Transfer completes successfully for single file
- ✅ File is organized in YYYY/YYYY-MM-DD structure

---

### 4. Transfer History and Monitoring

**Objective**: Test viewing and monitoring transfer jobs.

**Steps**:
1. Navigate to "Transfer History" page
2. Verify the list displays all transfers with:
   - Execution ID
   - Source and target paths
   - Status badge (color-coded)
   - Start and end times
   - File count
3. Click on a transfer row to view details
4. For a RUNNING transfer, verify status updates every 3 seconds
5. For a FAILED transfer, verify "Retry" button appears
6. For a COMPLETED transfer, verify "Delete" button appears
7. Test pagination if more than 20 transfers exist

**Expected Results**:
- ✅ All transfers display correctly
- ✅ Status badges are color-coded appropriately
- ✅ Running transfers update automatically
- ✅ Action buttons appear for appropriate statuses
- ✅ Pagination works correctly

---

### 5. Retry Failed Transfer

**Objective**: Test retrying a failed transfer job.

**Steps**:
1. Navigate to "Transfer History" page
2. Find a transfer with FAILED status (or create one by providing invalid paths)
3. Click the "Retry" button
4. Confirm the retry action in the dialog
5. Verify success toast appears
6. Verify a new transfer is created with a new execution ID
7. Verify the new transfer starts processing

**Expected Results**:
- ✅ Retry button only appears for FAILED transfers
- ✅ Confirmation dialog appears
- ✅ New transfer is created with same parameters
- ✅ Success toast displays
- ✅ New transfer processes successfully

---

### 6. Delete Transfer Record

**Objective**: Test deleting a transfer job record.

**Steps**:
1. Navigate to "Transfer History" page
2. Find a transfer with COMPLETED or FAILED status
3. Click the "Delete" button
4. Confirm the deletion in the dialog
5. Verify success toast appears
6. Verify the transfer is removed from the list
7. Verify transferred files are NOT deleted (check target directory)

**Expected Results**:
- ✅ Delete button only appears for COMPLETED/FAILED transfers
- ✅ Confirmation dialog warns that files won't be deleted
- ✅ Transfer record is removed from list
- ✅ Files remain in target directory
- ✅ Success toast displays

---

### 7. File Browser - Date Hierarchy

**Objective**: Test browsing transferred files organized by date.

**Steps**:
1. Navigate to "File Browser" page
2. Verify files are organized in year folders (YYYY)
3. Click on a year folder to expand it
4. Verify date folders (YYYY-MM-DD) appear
5. Click on a date folder to expand it
6. Verify files transferred on that date are displayed
7. Verify each file shows:
   - Thumbnail (for images) or file icon
   - Filename
   - File size
   - Transfer date
8. Click on a file card to view details

**Expected Results**:
- ✅ Files are organized by date hierarchy
- ✅ Folders expand/collapse correctly
- ✅ Thumbnails load for image files
- ✅ File information is accurate
- ✅ Clicking a file navigates to details page

---

### 8. File Details and Metadata

**Objective**: Test viewing detailed file information.

**Steps**:
1. From File Browser, click on an image file
2. Verify the File Details page displays:
   - Full-size image preview
   - Filename, path, size
   - Transfer date and source path
   - File hash (MD5 or SHA-256)
3. If EXIF data is available, verify it displays:
   - Camera model
   - Capture date
   - GPS coordinates (with "View on Map" link)
   - Focal length, aperture, ISO, shutter speed
4. If AI analysis is available, verify it displays:
   - Description
   - Tags (as colored badges)
   - Confidence score (as percentage)
5. If metadata is not yet available, verify loading indicator appears
6. Click "Back" button to return to File Browser

**Expected Results**:
- ✅ All file information displays correctly
- ✅ Image preview loads properly
- ✅ EXIF data is formatted and readable
- ✅ AI analysis results are displayed
- ✅ Loading state shows when metadata is processing
- ✅ Back button works correctly

---

### 9. Search Files

**Objective**: Test searching for files by various criteria.

**Steps**:
1. Navigate to "File Browser" page
2. Enter a search query in the search bar (e.g., filename or AI description keyword)
3. Verify search results appear after 500ms debounce
4. Test date range filtering:
   - Select a "Date From" date
   - Select a "Date To" date
   - Verify only files in that range appear
5. Test tag filtering (if tags are available):
   - Select one or more tags from the dropdown
   - Verify only files with those tags appear
6. Test combined filters (query + date + tags)
7. Click "Clear Search" to return to normal view
8. Verify "no results" message appears for searches with no matches

**Expected Results**:
- ✅ Search is debounced (doesn't fire on every keystroke)
- ✅ Results update based on query
- ✅ Date filtering works correctly
- ✅ Tag filtering works correctly
- ✅ Combined filters work together
- ✅ Clear search resets all filters
- ✅ Empty results show helpful message

---

### 10. Settings Management

**Objective**: Test saving and clearing user settings.

**Steps**:
1. Navigate to "Settings" page
2. Verify current settings are displayed
3. Update the default paths:
   - **Default Source Directory**: Enter a new path
   - **Default Target Base Directory**: Enter a new path
4. Click "Save Settings" button
5. Verify success toast appears
6. Navigate to "New Transfer" page
7. Verify the form is pre-populated with saved defaults
8. Return to "Settings" page
9. Click "Clear All Settings" button
10. Confirm the action in the dialog
11. Verify success toast appears
12. Verify settings are reset to defaults
13. Verify recent paths list is cleared

**Expected Results**:
- ✅ Settings save to localStorage
- ✅ Success toast displays on save
- ✅ Saved settings pre-populate transfer form
- ✅ Clear settings resets to defaults
- ✅ Recent paths are cleared
- ✅ API base URL is displayed (read-only)

---

### 11. Responsive Design - Mobile View

**Objective**: Test the application on mobile screen sizes.

**Steps**:
1. Open browser DevTools and switch to mobile view (320px width)
2. Navigate through all pages
3. Verify the hamburger menu appears in the navigation bar
4. Click the hamburger menu to open mobile navigation
5. Verify all navigation links are accessible
6. Test Transfer History page:
   - Verify table switches to card layout
   - Verify cards are touch-friendly (min 44px height)
7. Test File Browser page:
   - Verify grid switches to single column
   - Verify file cards are full width
8. Test forms (New Transfer, Settings):
   - Verify inputs are full width
   - Verify buttons are full width and touch-friendly
9. Test at various widths: 320px, 375px, 768px, 1024px, 1440px

**Expected Results**:
- ✅ Hamburger menu appears on mobile
- ✅ Mobile navigation works correctly
- ✅ Tables switch to card layout
- ✅ All interactive elements are touch-friendly (≥44px)
- ✅ Content is readable at all screen sizes
- ✅ No horizontal scrolling occurs
- ✅ Images and layouts adapt appropriately

---

### 12. Error Handling

**Objective**: Test error handling and user feedback.

**Steps**:
1. **Network Error Test**:
   - Stop the backend server
   - Try to create a transfer
   - Verify connection error banner appears
   - Verify error toast displays
   - Restart backend and verify banner disappears

2. **Validation Error Test**:
   - Navigate to "New Transfer"
   - Try to submit with empty required fields
   - Verify field-specific error messages appear
   - Try to fill both Source Directory and File Path
   - Verify mutual exclusivity error appears

3. **404 Error Test**:
   - Navigate to a non-existent route (e.g., /invalid-page)
   - Verify 404 page displays
   - Verify navigation back to home works

4. **API Error Test**:
   - Try to delete a running transfer (should return 409)
   - Verify appropriate error message displays
   - Verify error toast appears

**Expected Results**:
- ✅ Connection errors show banner and toast
- ✅ Validation errors are field-specific
- ✅ 404 page displays for invalid routes
- ✅ API errors show appropriate messages
- ✅ All errors are logged to console
- ✅ Retry options work where applicable

---

### 13. Loading States

**Objective**: Verify loading indicators appear during async operations.

**Steps**:
1. Navigate to Dashboard and observe loading spinner while transfers load
2. Navigate to File Browser and observe loading spinner while files load
3. Create a new transfer and observe loading spinner on submit button
4. View file details and observe loading spinner while metadata loads
5. Verify loading spinners have appropriate sizes and messages

**Expected Results**:
- ✅ Loading spinners appear during all async operations
- ✅ Spinners have appropriate sizes (small, medium, large)
- ✅ Loading messages are descriptive
- ✅ UI remains responsive during loading
- ✅ Loading states don't block navigation

---

### 14. Toast Notifications

**Objective**: Test toast notification system.

**Steps**:
1. Perform actions that trigger success toasts:
   - Create a transfer
   - Save settings
   - Delete a transfer
   - Retry a transfer
2. Verify each toast:
   - Appears in bottom-right corner (desktop) or bottom-center (mobile)
   - Has appropriate color (green for success, red for error)
   - Has appropriate icon
   - Auto-dismisses after 5 seconds
   - Can be manually dismissed by clicking X
3. Trigger multiple toasts quickly and verify they stack correctly

**Expected Results**:
- ✅ Toasts appear for all user actions
- ✅ Colors and icons are appropriate
- ✅ Toasts auto-dismiss after 5 seconds
- ✅ Manual dismiss works
- ✅ Multiple toasts stack without overlapping
- ✅ Toasts are positioned correctly on mobile

---

## Browser Compatibility Testing

Test the application in the following browsers:

- ✅ Chrome (latest)
- ✅ Firefox (latest)
- ✅ Safari (latest)
- ✅ Edge (latest)

Verify all features work consistently across browsers.

---

## Performance Testing

1. **Large File Lists**: Test with 100+ files in File Browser
   - Verify pagination works
   - Verify scrolling is smooth
   - Verify thumbnails load efficiently

2. **Multiple Running Transfers**: Create several transfers simultaneously
   - Verify polling doesn't cause performance issues
   - Verify UI remains responsive

3. **Network Throttling**: Test with slow 3G connection
   - Verify loading states appear
   - Verify timeouts are handled gracefully

---

## Accessibility Testing

1. **Keyboard Navigation**: Navigate the entire app using only keyboard
   - Tab through all interactive elements
   - Verify focus indicators are visible
   - Verify Enter/Space activate buttons

2. **Screen Reader**: Test with a screen reader (NVDA, JAWS, VoiceOver)
   - Verify all content is announced
   - Verify form labels are associated correctly
   - Verify error messages are announced

3. **Color Contrast**: Verify all text meets WCAG AA standards
   - Use browser DevTools to check contrast ratios
   - Verify status badges are distinguishable

---

## Test Completion Checklist

- [ ] All 14 test scenarios completed successfully
- [ ] Tested on all supported browsers
- [ ] Tested on mobile devices (or DevTools mobile view)
- [ ] Performance is acceptable with large datasets
- [ ] No console errors during normal operation
- [ ] All loading states display correctly
- [ ] All error states display correctly
- [ ] Toast notifications work as expected
- [ ] Navigation works smoothly
- [ ] Forms validate correctly
- [ ] API integration works end-to-end

---

## Reporting Issues

If you encounter any issues during testing:

1. Note the exact steps to reproduce
2. Capture browser console errors
3. Take screenshots if applicable
4. Note the browser and version
5. Note the screen size (for responsive issues)
6. Document expected vs. actual behavior

---

## Next Steps

After completing all tests:

1. Document any bugs found
2. Verify all critical user flows work
3. Confirm the application is ready for deployment
4. Consider adding automated E2E tests using Playwright or Cypress

