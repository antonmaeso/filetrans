<template>
    <div class="not-found">
        <h1>404 - Page Not Found</h1>
        <p>The page you're looking for doesn't exist.</p>
        <router-link to="/">Go to Dashboard</router-link>
    </div>
</template>

<script setup lang="ts">
// NotFound page
</script>

<style scoped>
.not-found {
    padding: 2rem;
    text-align: center;
}

.not-found h1 {
    color: #e74c3c;
    margin-bottom: 1rem;
}

.not-found a {
    display: inline-block;
    margin-top: 1rem;
    padding: 0.5rem 1rem;
    background-color: #3498db;
    color: white;
    text-decoration: none;
    border-radius: 4px;
}

.not-found a:hover {
    background-color: #2980b9;
}
</style>
