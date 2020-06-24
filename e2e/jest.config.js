module.exports = {
  "verbose": true,
  "bail": 3,  // Stop running tests after `n` failures
  "preset": "jest-puppeteer",
  "testTimeout": 600000,
  "testRunner": "jest-circus/runner",
  "testEnvironment": "<rootDir>/puppeteer-custom-environment.ts",
  "setupFilesAfterEnv": [
    "<rootDir>/jest-circus.setup.ts",
    "<rootDir>/jest.test-setup.ts"
  ],
  "reporters": [
    "default",
    "jest-junit"
  ],
  "transform": {
    "\\.(ts|tsx)$": "ts-jest"
  },
  "globals": {
    "ts-jest": {
      "tsConfig": "tsconfig.jest.json"
    }
  },
  "testPathIgnorePatterns": [
    "/node_modules/",
    "/tsc-out/"
  ],
  "testMatch": [
    "<rootDir>/tests/**/*.spec.ts"
  ],
  "transformIgnorePatterns": [
    "<rootDir>/node_modules/(?!tests)"
  ],
  "moduleFileExtensions": [
    "ts",
    "spec.ts",
    "tsx",
    "js",
    "jsx",
    "json",
    "node"
  ],
  "modulePaths": [
    "<rootDir>"
  ]
};
