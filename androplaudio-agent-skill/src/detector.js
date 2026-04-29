#!/usr/bin/env node
'use strict';

const fs = require('fs');
const path = require('path');
const { glob } = require('glob');

const DI_PATTERNS = {
  hilt: [/com\.google\.dagger:hilt-android/, /dagger\.hilt/],
  koin: [/io\.insert-koin:koin/, /org\.koin:koin/],
  dagger: [/com\.google\.dagger:dagger/, /javax\.inject/],
};

const PLATFORM_PATTERNS = {
  cmp: [/org\.jetbrains\.compose/, /compose-multiplatform/],
  kmm: [/kotlin-multiplatform/, /org\.jetbrains\.kotlin\.multiplatform/, /commonMain/],
};

async function detect(projectDir) {
  const gradleFiles = await glob('**/build.gradle{,.kts}', {
    cwd: projectDir,
    ignore: ['**/build/**', '**/node_modules/**', '**/.gradle/**'],
    absolute: true,
  });

  let allContent = '';
  for (const file of gradleFiles) {
    try { allContent += fs.readFileSync(file, 'utf8') + '\n'; } catch (_) {}
  }

  return {
    platform: detectPlatform(allContent),
    framework: detectFramework(allContent),
    projectDir,
  };
}

function detectPlatform(content) {
  for (const pattern of PLATFORM_PATTERNS.cmp) {
    if (pattern.test(content)) return 'cmp';
  }
  for (const pattern of PLATFORM_PATTERNS.kmm) {
    if (pattern.test(content)) return 'kmm';
  }
  return 'android';
}

function detectFramework(content) {
  for (const [name, patterns] of Object.entries(DI_PATTERNS)) {
    for (const pattern of patterns) {
      if (pattern.test(content)) return name;
    }
  }
  return 'manual';
}

module.exports = { detect };
