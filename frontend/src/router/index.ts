import { createRouter, createWebHistory } from 'vue-router';

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/',
            name: 'Dashboard',
            component: () => import('../views/Dashboard.vue'),
            meta: {
                title: 'Dashboard',
            },
        },
        {
            path: '/transfers/new',
            name: 'NewTransfer',
            component: () => import('../views/NewTransfer.vue'),
            meta: {
                title: 'New Transfer',
            },
        },
        {
            path: '/transfers',
            name: 'TransferHistory',
            component: () => import('../views/TransferHistory.vue'),
            meta: {
                title: 'Transfer History',
            },
        },
        {
            path: '/transfers/:executionId',
            name: 'TransferDetails',
            component: () => import('../views/TransferDetails.vue'),
            meta: {
                title: 'Transfer Details',
            },
            props: true,
        },
        {
            path: '/files',
            name: 'FileBrowser',
            component: () => import('../views/FileBrowser.vue'),
            meta: {
                title: 'File Browser',
            },
        },
        {
            path: '/files/:id',
            name: 'FileDetails',
            component: () => import('../views/FileDetails.vue'),
            meta: {
                title: 'File Details',
            },
            props: true,
        },
        {
            path: '/settings',
            name: 'Settings',
            component: () => import('../views/Settings.vue'),
            meta: {
                title: 'Settings',
            },
        },
        {
            path: '/:pathMatch(.*)*',
            name: 'NotFound',
            component: () => import('../views/NotFound.vue'),
            meta: {
                title: 'Page Not Found',
            },
        },
    ],
});

// Navigation guard to update document title
router.beforeEach((to, _from, next) => {
    const title = to.meta.title as string | undefined;
    if (title) {
        document.title = `${title} - File Transfer`;
    }
    next();
});

export default router;
