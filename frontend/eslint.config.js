import js from '@eslint/js';
import typescript from '@typescript-eslint/eslint-plugin';
import typescriptParser from '@typescript-eslint/parser';
import vue from 'eslint-plugin-vue';
import vueParser from 'vue-eslint-parser';
import prettier from 'eslint-plugin-prettier';
import prettierConfig from 'eslint-config-prettier';

export default [
    js.configs.recommended,
    ...vue.configs['flat/recommended'],
    prettierConfig,
    {
        files: ['**/*.{js,mjs,cjs,ts}'],
        plugins: {
            '@typescript-eslint': typescript,
            prettier: prettier,
        },
        languageOptions: {
            parser: typescriptParser,
            parserOptions: {
                ecmaVersion: 'latest',
                sourceType: 'module',
            },
        },
        rules: {
            'prettier/prettier': 'warn',
            '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
        },
    },
    {
        files: ['**/*.vue'],
        plugins: {
            '@typescript-eslint': typescript,
            prettier: prettier,
        },
        languageOptions: {
            parser: vueParser,
            parserOptions: {
                parser: typescriptParser,
                ecmaVersion: 'latest',
                sourceType: 'module',
            },
        },
        rules: {
            'prettier/prettier': 'warn',
            'vue/multi-word-component-names': 'off',
            '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
        },
    },
    {
        ignores: ['node_modules/**', 'dist/**', '.vite/**'],
    },
];
