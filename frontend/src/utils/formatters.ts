/**
 * Format a date string or Date object into a human-readable format
 * @param date - Date string (ISO 8601) or Date object
 * @param format - Format type: 'short' (MM/DD/YYYY), 'long' (Month DD, YYYY HH:MM), 'time' (HH:MM:SS)
 * @returns Formatted date string
 */
export function formatDate(
    date: string | Date | undefined | null,
    format: 'short' | 'long' | 'time' = 'long'
): string {
    if (!date) return 'N/A';

    const dateObj = typeof date === 'string' ? new Date(date) : date;

    if (isNaN(dateObj.getTime())) {
        return 'Invalid Date';
    }

    switch (format) {
        case 'short':
            return dateObj.toLocaleDateString('en-US', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
            });

        case 'time':
            return dateObj.toLocaleTimeString('en-US', {
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit',
            });

        case 'long':
        default:
            return dateObj.toLocaleString('en-US', {
                year: 'numeric',
                month: 'short',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit',
            });
    }
}

/**
 * Format file size in bytes to human-readable format
 * @param bytes - File size in bytes
 * @param decimals - Number of decimal places (default: 2)
 * @returns Formatted file size string (e.g., "1.5 MB")
 */
export function formatFileSize(bytes: number | undefined | null, decimals: number = 2): string {
    if (bytes === undefined || bytes === null) return 'N/A';
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const dm = decimals < 0 ? 0 : decimals;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB'];

    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return `${parseFloat((bytes / Math.pow(k, i)).toFixed(dm))} ${sizes[i]}`;
}

/**
 * Format duration in milliseconds to human-readable format
 * @param milliseconds - Duration in milliseconds
 * @param format - Format type: 'short' (1h 30m), 'long' (1 hour 30 minutes), 'precise' (1h 30m 45s)
 * @returns Formatted duration string
 */
export function formatDuration(
    milliseconds: number | undefined | null,
    format: 'short' | 'long' | 'precise' = 'short'
): string {
    if (milliseconds === undefined || milliseconds === null) return 'N/A';
    if (milliseconds < 0) return 'Invalid Duration';

    const seconds = Math.floor(milliseconds / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);

    const remainingHours = hours % 24;
    const remainingMinutes = minutes % 60;
    const remainingSeconds = seconds % 60;

    if (format === 'long') {
        const parts: string[] = [];
        if (days > 0) parts.push(`${days} day${days > 1 ? 's' : ''}`);
        if (remainingHours > 0)
            parts.push(`${remainingHours} hour${remainingHours > 1 ? 's' : ''}`);
        if (remainingMinutes > 0)
            parts.push(`${remainingMinutes} minute${remainingMinutes > 1 ? 's' : ''}`);
        if (remainingSeconds > 0 && days === 0)
            parts.push(`${remainingSeconds} second${remainingSeconds > 1 ? 's' : ''}`);

        return parts.length > 0 ? parts.join(' ') : '0 seconds';
    }

    if (format === 'precise') {
        const parts: string[] = [];
        if (days > 0) parts.push(`${days}d`);
        if (remainingHours > 0) parts.push(`${remainingHours}h`);
        if (remainingMinutes > 0) parts.push(`${remainingMinutes}m`);
        if (remainingSeconds > 0) parts.push(`${remainingSeconds}s`);

        return parts.length > 0 ? parts.join(' ') : '0s';
    }

    // 'short' format (default)
    if (days > 0) {
        return `${days}d ${remainingHours}h`;
    }
    if (hours > 0) {
        return `${hours}h ${remainingMinutes}m`;
    }
    if (minutes > 0) {
        return `${minutes}m ${remainingSeconds}s`;
    }
    return `${seconds}s`;
}
