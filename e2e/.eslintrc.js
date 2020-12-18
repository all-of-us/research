module.exports = {
  'extends': ['eslint:recommended', 'google'],
  'root': true,
  'env': {
    'node': true,
    'jest': true,
    'browser': true,
    'es6': true
  },
  'globals': {
    'page': true,
    'browser': true,
    'context': true,
    'jestPuppeteer': true
  },
  'parserOptions': {
    'ecmaVersion': 10,
    'sourceType': 'module',
    'ecmaFeatures': {
      'jsx': false
    }
  },
  'rules': {
    // 2 == error, 1 == warning, 0 == off
    'indent': [2, 2, {
      'SwitchCase': 1,
      'VariableDeclarator': 2,
      'MemberExpression': 2,
      'outerIIFEBody': 0
    }],
    'max-len': [2, 140, {
      'ignoreComments': true,
      'ignoreUrls': true,
      'tabWidth': 2
    }],
    'no-var': 2,
    'no-console': 0,
    'prefer-const': 2,

    // Disable rules
    'comma-dangle': 0,
    'arrow-parens': 0,
    'require-jsdoc': 0
  },
  'ignorePatterns': [
    'tsc-out/*'
  ]
};
