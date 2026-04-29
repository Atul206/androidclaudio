#!/usr/bin/env node
'use strict';

const { Command } = require('commander');
const path = require('path');
const detector = require('./detector');
const scanner = require('./scanner');
const writer = require('./writer');

const program = new Command();

program
  .name('androplaudio-setup')
  .description('Scan Android/KMM/CMP project and write groups.json for AndroClaudio')
  .option('-p, --project-dir <dir>', 'Project root directory', process.cwd())
  .option('-o, --output <file>', 'Output file path', 'androplaudio-groups.json')
  .option('--dry-run', 'Print result without writing file', false)
  .action(async (options) => {
    const projectDir = path.resolve(options.projectDir);
    console.log(`[androplaudio] Scanning: ${projectDir}`);

    try {
      const detection = await detector.detect(projectDir);
      console.log(`[androplaudio] Platform: ${detection.platform}, DI: ${detection.framework}`);

      const groups = await scanner.scan(projectDir, detection);
      console.log(`[androplaudio] Found ${groups.length} groups`);

      const result = writer.buildOutput(detection, groups);

      if (options.dryRun) {
        console.log(JSON.stringify(result, null, 2));
      } else {
        const outputPath = path.resolve(options.output);
        writer.write(outputPath, result);
        console.log(`[androplaudio] Written: ${outputPath}`);
      }
    } catch (err) {
      console.error(`[androplaudio] Error: ${err.message}`);
      process.exit(1);
    }
  });

program.parse();
