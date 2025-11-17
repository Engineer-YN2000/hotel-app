module.exports = {
  extends: [
    'react-app',
    'react-app/jest',
  ],
  rules: {},
  overrides: [
    {
      files: ['src/service-worker.js'],
      env: { serviceworker: true, browser: false },
      rules: {
        'no-restricted-globals': 'off'
      },
    },
  ],
};
