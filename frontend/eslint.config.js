// ESLint plano con la configuración de Expo. El cliente generado no se revisa.
const { defineConfig } = require('eslint/config');
const expoConfig = require('eslint-config-expo/flat');

module.exports = defineConfig([
  expoConfig,
  {
    ignores: ['dist/*', '.expo/*', 'src/api/esquema.d.ts'],
  },
]);
