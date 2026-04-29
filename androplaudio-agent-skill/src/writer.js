'use strict';

const fs = require('fs');

const MAX_SIZE_BYTES = 10 * 1024; // 10KB

function buildOutput(detection, groups) {
  return {
    version: '1.0',
    platform: detection.platform,
    framework: detection.framework,
    groups: groups.map(g => ({ id: g.id, layer: g.layer, class: g.class })),
  };
}

function write(outputPath, result) {
  const json = JSON.stringify(result, null, 2);
  if (Buffer.byteLength(json, 'utf8') > MAX_SIZE_BYTES) {
    console.warn(`[androplaudio] Warning: groups.json is ${Buffer.byteLength(json, 'utf8')} bytes (limit 10KB). Consider filtering groups manually.`);
  }
  fs.writeFileSync(outputPath, json, 'utf8');
}

module.exports = { buildOutput, write };
