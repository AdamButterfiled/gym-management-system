export type FilterLogic = 'AND' | 'OR';
export type MatchMode = 'contains' | 'exact';
export type FormConfigTargetType = 'table' | 'form' | 'modal-form';
export type FormConfigIssueLevel = 'info' | 'warning' | 'error';
export type ControlType =
  | 'text'
  | 'select'
  | 'remote-api'
  | 'remote-form'
  | 'date'
  | 'date-range'
  | 'number'
  | 'number-range';

export type OptionSourceType = 'static' | 'dict' | 'remote-api' | 'remote-form';

export interface FormOptionItem {
  label: string;
  value: string | number | boolean;
  disabled?: boolean;
  raw?: Record<string, unknown>;
}

export interface FormFieldPreviewOption {
  label: string;
  value?: string | number | boolean;
  icon?: string;
  dynamic?: boolean;
}

export interface FormFieldPreviewSchema {
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
  options?: FormFieldPreviewOption[];
  dynamicOptionsHint?: string;
  checkedChildren?: string;
  uncheckedChildren?: string;
  extra?: string;
}

export interface FormFieldConfig {
  fieldKey: string;
  label: string;
  queryKey: string;
  visible?: boolean;
  order?: number;
  columnVisible?: boolean;
  columnOrder?: number;
  columnWidth?: number;
  layout?: FormFieldLayout;
  filterEnabled?: boolean;
  quickSearchEnabled?: boolean;
  controlType?: ControlType | string;
  operatorSet?: string[];
  defaultOperator?: string;
  defaultMatchMode?: MatchMode | string;
  allowMatchModeToggle?: boolean;
  allowMultiple?: boolean;
  optionSourceType?: OptionSourceType | string;
  optionSourceConfig?: Record<string, unknown>;
  placeholder?: string;
  previewSchema?: FormFieldPreviewSchema;
  hiddenButFilterable?: boolean;
  runtimeIssues?: FormConfigRuntimeIssue[];
}

export interface FormFieldLayout {
  x: number;
  y: number;
  w: number;
  h: number;
}

export interface FormConfigRuntimeIssue {
  code: string;
  level: FormConfigIssueLevel;
  message: string;
  targetKey?: string;
  fieldKey?: string;
}

export interface FormPageMenuBinding {
  menuId?: number;
  menuPath?: string;
  menuTitle?: string;
  menuTrail?: string[];
  componentPath?: string;
  routeName?: string;
}

export interface FormPageQuickSearch {
  enabled?: boolean;
  placeholder?: string;
  fields: string[];
  defaultLogic?: FilterLogic | string;
}

export interface FormConfigTargetSourceSignature {
  componentPath?: string;
  routePath?: string;
  routeName?: string;
  columnsBinding?: string;
  baseColumnsBinding?: string;
  modalBinding?: string;
  modalTitleBinding?: string;
  modalPreviewTitle?: string;
  modalWidth?: string;
  modalBodyStyle?: string;
  formLayout?: string;
  formClass?: string;
  formLabelSpan?: string;
  formWrapperSpan?: string;
  formLayoutSource?: 'configured-layout' | 'native-form' | 'row-col-form' | string;
  titleCandidate?: string;
  detectedBy?: string;
}

export interface FormConfigTarget {
  targetKey: string;
  targetType: FormConfigTargetType;
  title: string;
  enabled?: boolean;
  order?: number;
  sourceSignature?: FormConfigTargetSourceSignature;
  fields: FormFieldConfig[];
  runtimeIssues?: FormConfigRuntimeIssue[];
}

export interface FormPageConfig {
  version?: number;
  pageKey: string;
  routePath: string;
  pageTitle: string;
  enabled?: boolean;
  formInputFollowSystemRadius?: boolean;
  menuBinding?: FormPageMenuBinding;
  quickSearch?: FormPageQuickSearch;
  targets?: FormConfigTarget[];
  quickSearchPlaceholder?: string;
  quickSearchFields: string[];
  defaultFilterLogic?: FilterLogic | string;
  fields: FormFieldConfig[];
  runtimeIssues?: FormConfigRuntimeIssue[];
}

export interface FormFilterRule {
  id: string;
  fieldKey?: string;
  queryKey?: string;
  controlType?: string;
  operator?: string;
  matchMode?: string;
  value?: unknown;
  valueTo?: unknown;
}

export interface CompiledFilterRule {
  fieldKey: string;
  queryKey: string;
  controlType: string;
  operator: string;
  matchMode?: string;
  value?: unknown;
  valueTo?: unknown;
}

export interface TableQueryPayload {
  pageKey?: string;
  routePath?: string;
  pageNum: number;
  pageSize: number;
  keyword?: string;
  filterLogic?: string;
  filterRules: CompiledFilterRule[];
}

export interface SourcePageItem {
  pageKey: string;
  routePath: string;
  pageTitle: string;
}

export interface SourceFieldItem {
  fieldKey: string;
  label: string;
  queryKey: string;
  controlType?: string;
  filterEnabled?: boolean;
  columnVisible?: boolean;
}

export interface FormConfigManifestTargetField {
  fieldKey: string;
  label: string;
  queryKey?: string;
  controlType?: string;
  placeholder?: string;
  columnWidth?: number;
  layout?: FormFieldLayout;
  previewSchema?: FormFieldPreviewSchema;
}

export interface FormConfigManifestTarget {
  targetKey: string;
  targetType: FormConfigTargetType;
  title: string;
  order?: number;
  sourceSignature?: FormConfigTargetSourceSignature;
  fields: FormConfigManifestTargetField[];
  detectionIssues?: string[];
}

export interface FormConfigManifestPage {
  pageKey: string;
  routePath: string;
  routeName?: string;
  pageTitle: string;
  componentPath: string;
  quickSearchDetected?: boolean;
  targets: FormConfigManifestTarget[];
  detectionIssues?: string[];
}

export interface RemoteOptionsRequest {
  keyword?: string;
  optionSourceType?: string;
  optionSourceConfig?: Record<string, unknown>;
  pageNum?: number;
  pageSize?: number;
}
