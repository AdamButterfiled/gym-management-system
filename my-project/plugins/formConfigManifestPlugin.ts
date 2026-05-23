import { readFileSync, readdirSync, statSync } from 'node:fs';
import path from 'node:path';
import { parse as parseSfc } from '@vue/compiler-sfc';
import type { Plugin } from 'vite';

const VIRTUAL_MODULE_ID = 'virtual:form-config-manifest';
const RESOLVED_VIRTUAL_MODULE_ID = `\0${VIRTUAL_MODULE_ID}`;

interface RouteRecord {
  routePath: string;
  routeName: string;
  pageTitle: string;
  componentImportPath: string;
  componentPath: string;
  filePath: string;
}

interface TargetFieldRecord {
  fieldKey: string;
  label: string;
  queryKey?: string;
  controlType?: string;
  placeholder?: string;
  columnWidth?: number;
  layout?: { x: number; y: number; w: number; h: number };
  previewSchema?: PreviewSchemaRecord;
}

interface PreviewOptionRecord {
  label: string;
  value?: string | number | boolean;
  icon?: string;
  dynamic?: boolean;
}

interface PreviewSchemaRecord {
  component: string;
  className?: string;
  styleText?: string;
  wrapperClassName?: string;
  wrapperStyleText?: string;
  placeholder?: string;
  variant?: string;
  noMargin?: boolean;
  allowClear?: boolean;
  showSearch?: boolean;
  showTime?: boolean;
  treeDefaultExpandAll?: boolean;
  valueFormat?: string;
  min?: number;
  max?: number;
  step?: number;
  rows?: number;
  controls?: boolean;
  radioStyle?: 'default' | 'button';
  options?: PreviewOptionRecord[];
  dynamicOptionsHint?: string;
  checkedChildren?: string;
  uncheckedChildren?: string;
  extra?: string;
}

interface TargetRecord {
  targetKey: string;
  targetType: 'table' | 'form' | 'modal-form';
  title: string;
  order: number;
  sourceSignature: Record<string, string>;
  fields: TargetFieldRecord[];
  detectionIssues?: string[];
}

interface ManifestPageRecord {
  pageKey: string;
  routePath: string;
  routeName?: string;
  pageTitle: string;
  componentPath: string;
  quickSearchDetected?: boolean;
  targets: TargetRecord[];
  detectionIssues?: string[];
}

export function formConfigManifestPlugin(): Plugin {
  let projectRoot = '';
  let manifestCode = 'export default []';

  const rebuildManifest = () => {
    const manifest = scanManifest(projectRoot);
    manifestCode = `export default ${JSON.stringify(manifest, null, 2)};`;
  };

  return {
    name: 'form-config-manifest',
    enforce: 'pre',
    configResolved(config) {
      projectRoot = config.root;
      rebuildManifest();
    },
    handleHotUpdate(ctx) {
      if (ctx.file.includes('/src/views/') || ctx.file.endsWith('/src/router/index.ts')) {
        rebuildManifest();
      }
    },
    resolveId(id) {
      const cleanId = id.replace(/\?.*$/, '');
      if (cleanId === VIRTUAL_MODULE_ID) {
        return `${RESOLVED_VIRTUAL_MODULE_ID}${id.slice(cleanId.length)}`;
      }
      return null;
    },
    load(id) {
      const cleanId = id.replace(/\?.*$/, '');
      if (cleanId === RESOLVED_VIRTUAL_MODULE_ID) {
        return manifestCode;
      }
      return null;
    },
  };
}

function scanManifest(projectRoot: string): ManifestPageRecord[] {
  const viewsRoot = path.join(projectRoot, 'src/views');
  const routerFile = path.join(projectRoot, 'src/router/index.ts');
  const routes = readRoutes(routerFile, viewsRoot);
  const routesByFile = new Map(routes.map((route) => [route.filePath, route]));
  const routesByComponent = new Map(routes.map((route) => [route.componentPath, route]));
  const files = listVueFiles(viewsRoot);
  const pages: ManifestPageRecord[] = [];

  files.forEach((filePath) => {
    const route = routesByFile.get(filePath) || routesByComponent.get(toComponentPath(viewsRoot, filePath));
    pages.push(scanViewFile(filePath, viewsRoot, route || null));
  });

  return pages
    .sort((left, right) => {
      if (left.routePath && right.routePath) {
        return left.routePath.localeCompare(right.routePath, 'zh-CN');
      }
      return left.componentPath.localeCompare(right.componentPath, 'zh-CN');
    });
}

function scanViewFile(filePath: string, viewsRoot: string, route: RouteRecord | null): ManifestPageRecord {
  const source = readFileSync(filePath, 'utf-8');
  const sfc = parseSfc(source, { filename: filePath });
  const templateContent = sfc.descriptor.template?.content || '';
  const scriptContent = [sfc.descriptor.script?.content || '', sfc.descriptor.scriptSetup?.content || ''].join('\n');
  const headings = readHeadings(templateContent);
  const tables = readTables(templateContent, scriptContent, headings);
  const modals = readModals(templateContent, headings, scriptContent);
  const detectionIssues = [...(route ? [] : ['未挂载路由'])];

  if (!tables.length && !modals.length && !templateContent.includes('<TableSearchToolbar')) {
    detectionIssues.push('未检测到可配置目标');
  }

  return {
    pageKey: route?.routePath ? slugToPageKey(route.routePath) : kebabCase(path.basename(filePath, '.vue')),
    routePath: route?.routePath || '',
    routeName: route?.routeName,
    pageTitle: route?.pageTitle || headings[0]?.title || humanizeName(path.basename(filePath, '.vue')),
    componentPath: route?.componentPath || toComponentPath(viewsRoot, filePath),
    quickSearchDetected: templateContent.includes('<TableSearchToolbar'),
    targets: [...tables, ...modals],
    detectionIssues,
  };
}

function readRoutes(routerFile: string, viewsRoot: string): RouteRecord[] {
  const source = readFileSync(routerFile, 'utf-8');
  const pattern =
    /{\s*path:\s*'([^']+)'\s*,[\s\S]*?name:\s*'([^']+)'\s*,[\s\S]*?component:\s*\(\)\s*=>\s*import\('(@\/views\/[^']+\.vue)'\)\s*,[\s\S]*?meta:\s*{\s*title:\s*'([^']+)'/g;
  const routes: RouteRecord[] = [];
  let match: RegExpExecArray | null;
  while ((match = pattern.exec(source))) {
    const [, rawPath, routeName, componentImportPath, pageTitle] = match;
    const routePath = rawPath.startsWith('/') ? rawPath : `/${rawPath}`;
    const filePath = path.join(viewsRoot, componentImportPath.replace(/^@\/views\//, ''));
    routes.push({
      routePath,
      routeName,
      pageTitle,
      componentImportPath,
      componentPath: componentImportPath.replace(/^@\/views\//, '').replace(/\.vue$/, ''),
      filePath,
    });
  }
  return routes;
}

function readTables(templateContent: string, scriptContent: string, headings: Array<{ index: number; title: string }>): TargetRecord[] {
  const pattern = /<StandardTable\b[\s\S]*?:columns="([A-Za-z0-9_]+)"[\s\S]*?>/g;
  const matches = [...templateContent.matchAll(pattern)];

  return matches.map((match, index) => {
    const columnsBinding = match[1];
    const tableIndex = match.index || 0;
    const baseColumnsBinding = findBaseColumnsBinding(scriptContent, columnsBinding);
    const columnRecords = readColumnArray(scriptContent, baseColumnsBinding || columnsBinding);
    const targetKey = deriveTableTargetKey(columnsBinding, baseColumnsBinding, matches.length, index);
    const title = closestHeading(headings, tableIndex) || humanizeName(targetKey);
    return {
      targetKey,
      targetType: 'table',
      title,
      order: index,
      sourceSignature: {
        columnsBinding,
        baseColumnsBinding: baseColumnsBinding || columnsBinding,
        titleCandidate: title,
        detectedBy: 'build-scan',
      },
      fields: columnRecords.map((field, fieldIndex) => ({
        fieldKey: field.configKey || field.key || field.dataIndex || `column-${fieldIndex + 1}`,
        label: field.title || field.key || field.dataIndex || `列 ${fieldIndex + 1}`,
        queryKey: field.dataIndex || field.key || field.configKey,
        columnWidth: field.width,
      })),
      detectionIssues: columnRecords.length ? [] : ['未解析到表格列定义'],
    };
  });
}

function readModals(templateContent: string, headings: Array<{ index: number; title: string }>, scriptContent: string): TargetRecord[] {
  const pattern = /<StandardModal\b([\s\S]*?)>([\s\S]*?)<\/StandardModal>/g;
  const modals: TargetRecord[] = [];
  let match: RegExpExecArray | null;
  let modalIndex = 0;
  while ((match = pattern.exec(templateContent))) {
    const attrs = match[1] || '';
    const body = match[2] || '';
    const boundTitle = readBoundAttr(attrs, ':title');
    const title =
      readStaticAttr(attrs, 'title') ||
      boundTitle ||
      closestHeading(headings, match.index || 0) ||
      `表单 ${modalIndex + 1}`;
    const previewTitle = boundTitle ? readRefString(scriptContent, boundTitle) || title : title;
    const visibleBinding = readBoundAttr(attrs, 'v-model:visible') || readBoundAttr(attrs, ':visible') || '';
    const targetKey = deriveModalTargetKey(visibleBinding, title, modalIndex);
    const fields = readFormFields(body, scriptContent);
    const formSignature = readFormSignature(body);
    const modalSignature = readModalSignature(attrs);
    modals.push({
      targetKey,
      targetType: 'modal-form',
      title,
      order: 100 + modalIndex,
      sourceSignature: {
        modalBinding: visibleBinding,
        modalTitleBinding: boundTitle,
        modalPreviewTitle: previewTitle,
        modalWidth: modalSignature.width,
        modalBodyStyle: modalSignature.bodyStyle,
        formLayout: formSignature.layout,
        formClass: formSignature.formClass,
        formLabelSpan: formSignature.labelSpan,
        formWrapperSpan: formSignature.wrapperSpan,
        formLayoutSource: detectFormLayoutSource(body),
        titleCandidate: title,
        detectedBy: 'build-scan',
      },
      fields,
      detectionIssues: fields.length ? [] : ['未解析到表单字段'],
    });
    modalIndex += 1;
  }
  return modals;
}

function readFormFields(body: string, scriptContent = ''): TargetFieldRecord[] {
  const configuredLayoutMatch = body.match(/<ConfiguredFormLayout\b[\s\S]*?>([\s\S]*?)<\/ConfiguredFormLayout>/);
  if (configuredLayoutMatch) {
    return readConfiguredFormLayoutFields(configuredLayoutMatch[1] || '', scriptContent);
  }

  const fields: TargetFieldRecord[] = [];
  const rowBlocks = [...body.matchAll(/<a-row\b[\s\S]*?>([\s\S]*?)<\/a-row>/g)];
  let consumedBody = body;
  let rowIndex = 0;

  rowBlocks.forEach((rowMatch) => {
    const rowContent = rowMatch[1] || '';
    let x = 0;
    const colBlocks = [...rowContent.matchAll(/<a-col\b([\s\S]*?)>([\s\S]*?)<\/a-col>/g)];
    colBlocks.forEach((colMatch, colIndex) => {
      const attrContent = colMatch[1] || '';
      const field = readSingleFormItem(colMatch[2] || '', '', scriptContent);
      if (!field) {
        return;
      }
      const width = readSpanValue(attrContent);
      fields.push({
        ...field,
        layout: { x, y: rowIndex, w: width, h: 1 },
      });
      x += width;
      if (x >= 24) {
        x = 0;
      }
      if (colIndex === colBlocks.length - 1) {
        rowIndex += 1;
      }
    });
    consumedBody = consumedBody.replace(rowMatch[0], '');
  });

  const standaloneItems = [...consumedBody.matchAll(/<a-form-item\b[\s\S]*?<\/a-form-item>/g)];
  standaloneItems.forEach((itemMatch, index) => {
    const field = readSingleFormItem(itemMatch[0], '', scriptContent);
    if (!field) {
      return;
    }
    fields.push({
      ...field,
      layout: { x: 0, y: rowIndex + index, w: 24, h: 1 },
    });
  });

  return fields;
}

function detectFormLayoutSource(body: string) {
  if (/<ConfiguredFormLayout\b/.test(body)) {
    return 'configured-layout';
  }
  if (/<a-row\b[\s\S]*?<a-col\b/.test(body)) {
    return 'row-col-form';
  }
  return 'native-form';
}

function readConfiguredFormLayoutFields(source: string, scriptContent = ''): TargetFieldRecord[] {
  const fields: TargetFieldRecord[] = [];
  const slotBlocks = [...source.matchAll(/<template\s+#field-([A-Za-z0-9_]+)[^>]*>([\s\S]*?)<\/template>/g)];
  slotBlocks.forEach((slotMatch, index) => {
    const field = readSingleFormItem(slotMatch[2] || '', slotMatch[1], scriptContent);
    if (!field) {
      return;
    }
    const tallRows = field.previewSchema?.rows && field.previewSchema.rows > 3 ? 2 : 1;
    fields.push({
      ...field,
      layout: { x: 0, y: index, w: 24, h: tallRows },
    });
  });
  return fields;
}

function readSingleFormItem(source: string, preferredFieldKey = '', scriptContent = ''): TargetFieldRecord | null {
  const fieldKey =
    preferredFieldKey ||
    source.match(/data-field-key="([^"]+)"/)?.[1] ||
    source.match(/isFieldVisible\('([^']+)'\)/)?.[1] ||
    source.match(/v-model(?::[a-zA-Z-]+)?="[^"]+\.([A-Za-z0-9_]+)"/)?.[1] ||
    source.match(/v-model(?::[a-zA-Z-]+)?="([A-Za-z0-9_]+)"/)?.[1] ||
    '';
  const labelMatch = source.match(/label="([^"]+)"/);
  const placeholderMatch = source.match(/placeholder="([^"]+)"/);
  if (!fieldKey) {
    return null;
  }
  const label = labelMatch?.[1] || humanizeName(fieldKey);
  const lowerSource = source.toLowerCase();
  let controlType = 'text';
  if (/<a-range-picker\b/.test(lowerSource)) {
    controlType = 'date-range';
  } else if (/<a-time-picker\b/.test(lowerSource)) {
    controlType = 'date';
  } else if (/<a-date-picker\b/.test(lowerSource)) {
    controlType = 'date';
  } else if (/<a-input-number\b/.test(lowerSource)) {
    controlType = 'number';
  } else if (/<a-select\b/.test(lowerSource) || /<a-tree-select\b/.test(lowerSource)) {
    controlType = 'select';
  } else if (/<a-textarea\b/.test(lowerSource)) {
    controlType = 'textarea';
  }
  return {
    fieldKey,
    label,
    queryKey: fieldKey,
    controlType,
    placeholder: placeholderMatch?.[1] || '',
    previewSchema: readPreviewSchema(source, scriptContent),
  };
}

function readPreviewSchema(source: string, scriptContent = ''): PreviewSchemaRecord | undefined {
  const controlMatch = source.match(
    /<(StandardInput|a-tree-select|a-range-picker|a-time-picker|a-date-picker|a-input-number|a-select|a-radio-group|a-checkbox-group|a-switch|a-textarea|a-input)\b([^>]*)>/,
  );
  if (!controlMatch) {
    return undefined;
  }

  const component = controlMatch[1];
  const attrs = controlMatch[2] || '';
  const wrapperMatch = source.match(
    /<div\b([^>]*)>\s*<(?:StandardInput|a-tree-select|a-range-picker|a-time-picker|a-date-picker|a-input-number|a-select|a-radio-group|a-checkbox-group|a-switch|a-textarea|a-input)\b/,
  );
  const schema: PreviewSchemaRecord = {
    component,
    className: readStaticAttr(attrs, 'class') || '',
    styleText: readInlineStyle(attrs),
    wrapperClassName: wrapperMatch ? readStaticAttr(wrapperMatch[1] || '', 'class') || '' : '',
    wrapperStyleText: wrapperMatch ? readInlineStyle(wrapperMatch[1] || '') : '',
    placeholder: readStaticAttr(attrs, 'placeholder') || '',
    variant: readStaticAttr(attrs, 'variant') || '',
    noMargin: readBooleanValue(attrs, 'noMargin'),
    allowClear: readFlagValue(attrs, 'allow-clear'),
    showSearch: readFlagValue(attrs, 'show-search'),
    showTime: readFlagValue(attrs, 'show-time'),
    treeDefaultExpandAll: readFlagValue(attrs, 'tree-default-expand-all'),
    valueFormat: readStaticAttr(attrs, 'value-format') || '',
    min: readNumericValue(attrs, 'min'),
    max: readNumericValue(attrs, 'max'),
    step: readNumericValue(attrs, 'step'),
    rows: readNumericValue(attrs, 'rows'),
    controls: readBooleanValue(attrs, 'controls'),
    checkedChildren: readStaticAttr(attrs, 'checked-children') || '',
    uncheckedChildren: readStaticAttr(attrs, 'un-checked-children') || '',
    extra: readStaticAttr(source, 'extra') || '',
  };

  if (component === 'a-select' || component === 'a-tree-select') {
    const options = readSelectOptions(source, scriptContent);
    if (options.length) {
      schema.options = options;
    } else if (/<a-select-option\b/.test(source) && /v-for=/.test(source)) {
      schema.dynamicOptionsHint = 'dynamic';
    }
  }

  if (component === 'a-radio-group') {
    schema.radioStyle = /<a-radio-button\b/.test(source) ? 'button' : 'default';
    const options = readChoiceOptions(source, 'radio');
    if (options.length) {
      schema.options = options;
    }
  }

  if (component === 'a-checkbox-group') {
    const options = readChoiceOptions(source, 'checkbox');
    if (options.length) {
      schema.options = options;
    }
  }

  return schema;
}

function readSelectOptions(source: string, scriptContent = ''): PreviewOptionRecord[] {
  const optionMatches = [...source.matchAll(/<a-select-option\b([^>]*)>([\s\S]*?)<\/a-select-option>/g)];
  if (!optionMatches.length) {
    return [];
  }

  const dynamicCollectionMatch = source.match(/v-for="(?:\([^,]+,\s*([^)]+)\)|([A-Za-z0-9_]+))\s+in\s+([A-Za-z0-9_]+)"/);
  const dynamicCollection = dynamicCollectionMatch?.[3] || '';
  if (dynamicCollection) {
    const objectKeys = readObjectShorthandKeys(scriptContent, dynamicCollection);
    if (objectKeys.length) {
      return objectKeys.map((key) => ({
        label: key,
        value: key,
        icon: /icon/i.test(dynamicCollection) ? key : undefined,
        dynamic: true,
      }));
    }
  }

  return optionMatches.map((match, index) => ({
    label: normalizeOptionLabel(match[2], `选项 ${index + 1}`),
    value: readPrimitiveValue(match[1] || '', 'value'),
    dynamic: /v-for=/.test(match[0]),
  }));
}

function readChoiceOptions(source: string, type: 'radio' | 'checkbox'): PreviewOptionRecord[] {
  const pattern =
    type === 'radio'
      ? /<a-radio(?:-button)?\b([^>]*)>([\s\S]*?)<\/a-radio(?:-button)?>/g
      : /<a-checkbox\b([^>]*)>([\s\S]*?)<\/a-checkbox>/g;
  return [...source.matchAll(pattern)].map((match, index) => ({
    label: normalizeOptionLabel(match[2], `选项 ${index + 1}`),
    value: readPrimitiveValue(match[1] || '', 'value'),
    dynamic: /v-for=/.test(match[0]),
  }));
}

function normalizeOptionLabel(source: string, fallback: string) {
  const stripped = source
    .replace(/<[^>]+>/g, ' ')
    .replace(/{{[^}]+}}/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
  return stripped || fallback;
}

function readPrimitiveValue(source: string, attrName: string) {
  const staticValue = readStaticAttr(source, attrName);
  if (staticValue) {
    return toPrimitiveValue(staticValue);
  }
  const boundValue = readBoundAttr(source, `:${attrName}`);
  if (boundValue) {
    return toPrimitiveValue(boundValue);
  }
  return undefined;
}

function toPrimitiveValue(raw: string) {
  if (raw === 'true') {
    return true;
  }
  if (raw === 'false') {
    return false;
  }
  if (/^-?\d+(\.\d+)?$/.test(raw)) {
    return Number(raw);
  }
  return raw;
}

function readNumericValue(source: string, attrName: string) {
  const raw = readStaticAttr(source, attrName) || readBoundAttr(source, `:${attrName}`);
  return raw && /^-?\d+(\.\d+)?$/.test(raw) ? Number(raw) : undefined;
}

function readBooleanValue(source: string, attrName: string) {
  const raw = readBoundAttr(source, `:${attrName}`) || readStaticAttr(source, attrName);
  if (!raw) {
    return undefined;
  }
  if (raw === 'true') {
    return true;
  }
  if (raw === 'false') {
    return false;
  }
  return undefined;
}

function readFlagValue(source: string, attrName: string) {
  if (new RegExp(`(^|\\s)${attrName}(\\s|$|=)`).test(source)) {
    return true;
  }
  return readBooleanValue(source, attrName);
}

function readInlineStyle(source: string) {
  const staticStyle = readStaticAttr(source, 'style');
  if (staticStyle) {
    return staticStyle;
  }
  const boundStyle = readBoundAttr(source, ':style');
  if (!boundStyle) {
    return '';
  }
  const styleParts: string[] = [];
  const widthMatch = boundStyle.match(/width\s*:\s*'([^']+)'/);
  if (widthMatch) {
    styleParts.push(`width: ${widthMatch[1]}`);
  }
  const minHeightMatch = boundStyle.match(/minHeight\s*:\s*'([^']+)'/i);
  if (minHeightMatch) {
    styleParts.push(`min-height: ${minHeightMatch[1]}`);
  }
  return styleParts.join('; ');
}

function readFormSignature(body: string) {
  const formMatch = body.match(/<a-form\b([\s\S]*?)>/);
  if (!formMatch) {
    return {
      layout: '',
      formClass: '',
      labelSpan: '',
      wrapperSpan: '',
    };
  }

  const attrs = formMatch[1] || '';
  return {
    layout: readStaticAttr(attrs, 'layout') || '',
    formClass: readStaticAttr(attrs, 'class') || '',
    labelSpan: readObjectSpan(attrs, 'label-col'),
    wrapperSpan: readObjectSpan(attrs, 'wrapper-col'),
  };
}

function readModalSignature(attrs: string) {
  return {
    width: readStaticAttr(attrs, 'width') || readBoundAttr(attrs, ':width') || '',
    bodyStyle: readStyleObjectText(readBoundAttr(attrs, ':body-style') || readStaticAttr(attrs, 'body-style')),
  };
}

function readRefString(scriptContent: string, variableName: string) {
  if (!variableName || !/^[A-Za-z_$][A-Za-z0-9_$]*$/.test(variableName)) {
    return '';
  }
  const escaped = variableName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const refMatch = scriptContent.match(new RegExp(`const\\s+${escaped}\\s*=\\s*ref\\(\\s*['"\`]([^'"\`]+)['"\`]\\s*\\)`));
  if (refMatch) {
    return refMatch[1];
  }
  const assignmentExpressions = [
    ...scriptContent.matchAll(new RegExp(`${escaped}\\.value\\s*=([^\\n;]+)`, 'g')),
  ].map((match) => match[1]);
  const createTitleCandidates = assignmentExpressions
    .flatMap((expression) => [...expression.matchAll(/['"`]([^'"`]*(?:新增|添加|创建|发布)[^'"`]*)['"`]/g)].map((match) => match[1]))
    .sort((left, right) => Number(left.includes('${')) - Number(right.includes('${')));
  if (createTitleCandidates.length) {
    return createTitleCandidates[0];
  }
  const assignments = [
    ...scriptContent.matchAll(new RegExp(`${escaped}\\.value\\s*=\\s*['"\`]([^'"\`]+)['"\`]`, 'g')),
  ].map((match) => match[1]);
  return assignments.find((value) => /^(新增|添加|创建|发布)/.test(value)) || assignments[0] || '';
}

function readStyleObjectText(rawStyle: string) {
  const raw = String(rawStyle || '');
  if (!raw) {
    return '';
  }
  const keys = ['paddingTop', 'paddingRight', 'paddingBottom', 'paddingLeft', 'maxHeight', 'overflowY'];
  return keys
    .map((key) => {
      const match = raw.match(new RegExp(`${key}\\s*:\\s*['"]([^'"]+)['"]`));
      if (!match) {
        return '';
      }
      const cssKey = key.replace(/[A-Z]/g, (letter) => `-${letter.toLowerCase()}`);
      return `${cssKey}: ${match[1]}`;
    })
    .filter(Boolean)
    .join('; ');
}

function readColumnArray(scriptContent: string, variableName: string) {
  const arrayLiteral = extractArrayLiteral(scriptContent, variableName);
  if (!arrayLiteral) {
    return [] as Array<{ title?: string; key?: string; dataIndex?: string; configKey?: string; width?: number }>;
  }
  const objectMatches = extractTopLevelObjects(arrayLiteral);
  return objectMatches.map((objectLiteral) => ({
    title: readObjectString(objectLiteral, 'title'),
    key: readObjectString(objectLiteral, 'key'),
    dataIndex: readObjectString(objectLiteral, 'dataIndex'),
    configKey: readObjectString(objectLiteral, 'configKey'),
    width: readObjectNumber(objectLiteral, 'width'),
  }));
}

function findBaseColumnsBinding(scriptContent: string, binding: string) {
  const pattern = new RegExp(
    `const\\s+${binding}\\s*=\\s*computed\\([\\s\\S]*?=>\\s*buildColumns\\(([^,)]+)`,
    'm',
  );
  const match = scriptContent.match(pattern);
  return match?.[1]?.trim();
}

function extractArrayLiteral(source: string, variableName: string) {
  const declaration = new RegExp(`const\\s+${variableName}\\s*=`, 'm');
  const match = declaration.exec(source);
  if (!match) {
    return '';
  }
  const start = source.indexOf('[', match.index);
  if (start < 0) {
    return '';
  }
  let depth = 0;
  let quote: string | null = null;
  for (let index = start; index < source.length; index += 1) {
    const char = source[index];
    const prev = source[index - 1];
    if (quote) {
      if (char === quote && prev !== '\\') {
        quote = null;
      }
      continue;
    }
    if (char === '"' || char === '\'' || char === '`') {
      quote = char;
      continue;
    }
    if (char === '[') {
      depth += 1;
    } else if (char === ']') {
      depth -= 1;
      if (depth === 0) {
        return source.slice(start, index + 1);
      }
    }
  }
  return '';
}

function extractTopLevelObjects(arrayLiteral: string) {
  const objects: string[] = [];
  let braceDepth = 0;
  let quote: string | null = null;
  let start = -1;

  for (let index = 0; index < arrayLiteral.length; index += 1) {
    const char = arrayLiteral[index];
    const prev = arrayLiteral[index - 1];

    if (quote) {
      if (char === quote && prev !== '\\') {
        quote = null;
      }
      continue;
    }

    if (char === '"' || char === '\'' || char === '`') {
      quote = char;
      continue;
    }

    if (char === '{') {
      if (braceDepth === 0) {
        start = index;
      }
      braceDepth += 1;
      continue;
    }

    if (char === '}') {
      braceDepth -= 1;
      if (braceDepth === 0 && start >= 0) {
        objects.push(arrayLiteral.slice(start, index + 1));
        start = -1;
      }
    }
  }

  return objects;
}

function readObjectShorthandKeys(source: string, variableName: string) {
  const declaration = new RegExp(`const\\s+${variableName}(?:\\s*:[^=]+)?\\s*=`, 'm');
  const match = declaration.exec(source);
  if (!match) {
    return [] as string[];
  }
  const start = source.indexOf('{', match.index);
  if (start < 0) {
    return [] as string[];
  }
  let depth = 0;
  let quote: string | null = null;
  for (let index = start; index < source.length; index += 1) {
    const char = source[index];
    const prev = source[index - 1];
    if (quote) {
      if (char === quote && prev !== '\\') {
        quote = null;
      }
      continue;
    }
    if (char === '"' || char === '\'' || char === '`') {
      quote = char;
      continue;
    }
    if (char === '{') {
      depth += 1;
    } else if (char === '}') {
      depth -= 1;
      if (depth === 0) {
        const objectLiteral = source.slice(start + 1, index);
        return objectLiteral
          .split(',')
          .map((item) => item.trim())
          .filter(Boolean)
          .map((item) => item.match(/^([A-Za-z_][A-Za-z0-9_]*)$/)?.[1] || '')
          .filter(Boolean);
      }
    }
  }
  return [] as string[];
}

function readHeadings(templateContent: string) {
  return [...templateContent.matchAll(/<h2[^>]*workspace-section-title[^>]*>([^<]+)<\/h2>/g)].map((match) => ({
    index: match.index || 0,
    title: match[1].trim(),
  }));
}

function closestHeading(headings: Array<{ index: number; title: string }>, targetIndex: number) {
  const candidates = headings.filter((heading) => heading.index <= targetIndex);
  return candidates.length ? candidates[candidates.length - 1].title : '';
}

function readStaticAttr(source: string, attrName: string) {
  const escaped = attrName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const match = source.match(new RegExp(`(?:^|\\s)${escaped}="([^"]+)"`));
  return match?.[1] || '';
}

function readBoundAttr(source: string, attrName: string) {
  const escaped = attrName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const match = source.match(new RegExp(`(?:^|\\s)${escaped}="([^"]+)"`));
  return match?.[1] || '';
}

function readObjectString(source: string, key: string) {
  const match = source.match(new RegExp(`${key}\\s*:\\s*'([^']+)'`));
  return match?.[1];
}

function readObjectNumber(source: string, key: string) {
  const match = source.match(new RegExp(`${key}\\s*:\\s*(\\d+)`));
  return match ? Number(match[1]) : undefined;
}

function readSpanValue(source: string) {
  const direct = source.match(/(?:^|\s)(?::span|span)="?(\d+)"?/);
  if (direct) {
    return Number(direct[1]);
  }
  const small = source.match(/(?:^|\s)(?::sm|sm)="?(\d+)"?/);
  if (small) {
    return Number(small[1]);
  }
  return 12;
}

function readObjectSpan(source: string, attrName: string) {
  const escaped = attrName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const bound = source.match(new RegExp(`:${escaped}="\\{\\s*span:\\s*(\\d+)`));
  if (bound) {
    return bound[1];
  }
  const direct = source.match(new RegExp(`${escaped}="\\{\\s*span:\\s*(\\d+)`));
  return direct?.[1] || '';
}

function deriveTableTargetKey(columnsBinding: string, baseColumnsBinding: string | undefined, totalTables: number, index: number) {
  if (totalTables === 1) {
    return 'main-table';
  }
  const raw = baseColumnsBinding || columnsBinding;
  const normalized = raw
    .replace(/^base/, '')
    .replace(/Columns?$/, '')
    .replace(/^[A-Z]/, (char) => char.toLowerCase());
  const slug = kebabCase(normalized);
  return slug ? `${slug}-table` : `table-${index + 1}`;
}

function deriveModalTargetKey(visibleBinding: string, title: string, index: number) {
  if (visibleBinding) {
    const normalized = visibleBinding
      .replace(/Visible$/, '')
      .replace(/Modal$/, '')
      .replace(/Form$/, '');
    const slug = kebabCase(normalized);
    if (slug) {
      return `${slug}-form`;
    }
  }
  const titleSlug = kebabCase(title.replace(/^新增|^编辑/, ''));
  return titleSlug ? `${titleSlug}-form` : index === 0 ? 'main-form' : `modal-form-${index + 1}`;
}

function listVueFiles(directory: string): string[] {
  const entries = readdirSync(directory);
  const files: string[] = [];
  entries.forEach((entry) => {
    const fullPath = path.join(directory, entry);
    const stats = statSync(fullPath);
    if (stats.isDirectory()) {
      files.push(...listVueFiles(fullPath));
      return;
    }
    if (fullPath.endsWith('.vue')) {
      files.push(fullPath);
    }
  });
  return files;
}

function toComponentPath(viewsRoot: string, filePath: string) {
  return path.relative(viewsRoot, filePath).replace(/\\/g, '/').replace(/\.vue$/, '');
}

function humanizeName(value: string) {
  return value
    .replace(/([a-z])([A-Z])/g, '$1 $2')
    .replace(/[-_]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function kebabCase(value: string) {
  return value
    .replace(/([a-z])([A-Z])/g, '$1-$2')
    .replace(/[_\s]+/g, '-')
    .replace(/-+/g, '-')
    .replace(/^-|-$/g, '')
    .toLowerCase();
}

function slugToPageKey(routePath: string) {
  const normalized = routePath.replace(/^\/+/, '').replace(/\/+/g, '-');
  return normalized.replace(/-records$/, '-record').replace(/-orders$/, '-order').replace(/-conflicts$/, '-conflict');
}
