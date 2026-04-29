'use strict';

const fs = require('fs');
const { glob } = require('glob');

const LIFECYCLE_EXCLUDE = new Set([
  'onCreate', 'onDestroy', 'onStart', 'onStop', 'onPause', 'onResume',
  'onBind', 'onUnbind', 'onStartCommand', 'onRebind', 'onReceive',
  'doWork', 'onStopped', 'getForegroundInfo',
  'equals', 'hashCode', 'toString', 'copy',
  'query', 'insert', 'delete', 'update', 'getType', 'openFile',
]);

async function scan(projectDir, detection) {
  const { framework } = detection;

  const kotlinFiles = await glob('**/src/**/*.kt', {
    cwd: projectDir,
    ignore: ['**/build/**', '**/test/**', '**/androidTest/**'],
    absolute: true,
  });

  const groups = [];

  const extractors = {
    koin: extractKoinClasses,
    hilt: extractHiltClasses,
    dagger: extractDaggerClasses,
    manual: extractManualClasses,
  };
  const extractor = extractors[framework] || extractors.manual;

  for (const file of kotlinFiles) {
    try {
      const content = fs.readFileSync(file, 'utf8');
      groups.push(...extractor(content, file));
    } catch (_) {}
  }

  const manifestFiles = await glob('**/AndroidManifest.xml', {
    cwd: projectDir,
    ignore: ['**/build/**'],
    absolute: true,
  });

  for (const manifestFile of manifestFiles) {
    try {
      const content = fs.readFileSync(manifestFile, 'utf8');
      groups.push(...extractManifestComponents(content));
    } catch (_) {}
  }

  return deduplicateGroups(groups);
}

function extractKoinClasses(content, filePath) {
  const groups = [];
  const pkg = extractPackage(content);

  // single<Foo>(), factory<Foo>(), viewModel<Foo>(), scoped<Foo>()
  const re = /(?:single|factory|viewModel|scoped)\s*<\s*([\w.]+)\s*>/g;
  let m;
  while ((m = re.exec(content)) !== null) {
    const className = m[1];
    const fqcn = className.includes('.') ? className : (pkg ? `${pkg}.${className}` : className);
    groups.push({ id: classNameToGroupId(className.split('.').pop()), layer: inferLayer(filePath, fqcn), class: fqcn });
  }
  return groups;
}

function extractHiltClasses(content, filePath) {
  const groups = [];
  const pkg = extractPackage(content);

  const re = /@(?:HiltViewModel|AndroidEntryPoint|Singleton|ActivityScoped|ViewModelScoped)\s+(?:class|object)\s+(\w+)/g;
  let m;
  while ((m = re.exec(content)) !== null) {
    const className = m[1];
    const fqcn = pkg ? `${pkg}.${className}` : className;
    groups.push({ id: classNameToGroupId(className), layer: inferLayer(filePath, fqcn), class: fqcn });
  }
  return groups;
}

function extractDaggerClasses(content, filePath) {
  const groups = [];
  const pkg = extractPackage(content);

  const re = /@Provides\s+(?:@\w+\s+)*fun\s+\w+\s*\([^)]*\)\s*:\s*([\w.]+)/g;
  let m;
  while ((m = re.exec(content)) !== null) {
    const className = m[1];
    const fqcn = className.includes('.') ? className : (pkg ? `${pkg}.${className}` : className);
    groups.push({ id: classNameToGroupId(className.split('.').pop()), layer: inferLayer(filePath, fqcn), class: fqcn });
  }
  return groups;
}

function extractManualClasses(content, filePath) {
  if (!filePath.includes('/main/')) return [];
  const groups = [];
  const pkg = extractPackage(content);

  const SUFFIXES = 'Repository|UseCase|ViewModel|Service|Manager|Worker|Interactor|Controller';
  const re = new RegExp(`(?:class|object)\\s+(\\w+(?:${SUFFIXES})\\w*)`, 'g');
  let m;
  while ((m = re.exec(content)) !== null) {
    const className = m[1];
    const fqcn = pkg ? `${pkg}.${className}` : className;
    groups.push({ id: classNameToGroupId(className), layer: inferLayer(filePath, fqcn), class: fqcn });
  }
  return groups;
}

function extractManifestComponents(content) {
  const groups = [];
  const re = /android:name="([\w.]+)"/g;
  let m;
  while ((m = re.exec(content)) !== null) {
    const fqcn = m[1];
    if (fqcn.startsWith('.') || !fqcn.includes('.')) continue;
    const className = fqcn.split('.').pop();
    if (/(?:Activity|Service|Receiver|Provider|Worker)$/.test(className)) {
      groups.push({ id: `android.${classNameToGroupId(className)}`, layer: 'android', class: fqcn });
    }
  }
  return groups;
}

function extractPackage(content) {
  const m = content.match(/^package\s+([\w.]+)/m);
  return m ? m[1] : '';
}

function classNameToGroupId(className) {
  // "PaymentRepository" → "payment.repository"
  return className
    .replace(/([A-Z])/g, (c, char, i) => (i > 0 ? '.' : '') + char.toLowerCase())
    .replace(/\.+/g, '.');
}

function inferLayer(filePath, fqcn) {
  // Only use file path to determine layer — suffix heuristics cause false positives
  // (e.g. CurrencyService is shared, not an Android Service component).
  // Manifest components are tagged android explicitly in extractManifestComponents.
  if (
    filePath.includes('/androidMain/') ||
    filePath.includes('/android/src/')
  ) return 'android';
  return 'shared';
}

function deduplicateGroups(groups) {
  const seen = new Map();
  for (const g of groups) {
    if (!seen.has(g.class)) seen.set(g.class, g);
  }
  return Array.from(seen.values());
}

module.exports = { scan };
