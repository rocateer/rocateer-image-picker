import { NativeModules } from 'react-native';

const { RocateerImagePicker } = NativeModules;

if (!RocateerImagePicker) {
  console.warn(
    'RocateerImagePicker native module not found. Did you link it properly?'
  );
}

// 선택 모드
export type SelectionMode = 'single' | 'multiple';

export interface PickerOptions {
  /**
   * 모드 선택
   * @default 'single'
   */
  selectionMode?: SelectionMode;
  /**
   * 최대 갯수
   * @default Infinity
   */
  maxSelection?: number;
  /**
   * 체크박스 색상
   */
  checkboxTintColor?: string;
}

export interface ImageAsset {
  uri: string;
  width?: number;
  height?: number;
  fileName?: string;
  fileSize?: number; // bytes
  type?: string; // mime-type like 'image/jpeg'
}

/**
 * Internal: normalize and validate options before sending to native.
 */
function normalizeOptions(opts?: PickerOptions) {
  const selectionMode: SelectionMode = opts?.selectionMode ?? 'single';

  let allowMultiple = selectionMode === 'multiple';

  let maxSelection: number | undefined = undefined;
  if (allowMultiple) {
    if (opts?.maxSelection == null) {
      maxSelection = undefined; // let native decide (no cap) unless provided
    } else {
      const n = Number(opts.maxSelection);
      if (!Number.isFinite(n) || n < 1) {
        throw new Error(
          '[RocateerImagePicker] `maxSelection` must be a positive integer when using multiple selection.'
        );
      }
      maxSelection = Math.floor(n);
    }
  } else {
    maxSelection = 1; // force single
  }

  const checkboxTintColor = opts?.checkboxTintColor;
  if (checkboxTintColor != null && typeof checkboxTintColor !== 'string') {
    throw new Error(
      '[RocateerImagePicker] `checkboxTintColor` must be a string color.'
    );
  }

  return {
    allowMultiple,
    maxSelection,
    checkboxTintColor,
  } as const;
}

/**
 * 이미지 피커 오픈
 *
 * Native bridges should expose a method `open(options)` that resolves to an array of assets.
 *
 * Expected native option keys:
 * - `allowMultiple: boolean`
 * - `maxSelection?: number`   // respected only when allowMultiple=true
 * - `checkboxTintColor?: string`
 */
export async function pickImages(
  options?: PickerOptions
): Promise<ImageAsset[]> {
  if (!RocateerImagePicker || typeof RocateerImagePicker.open !== 'function') {
    throw new Error(
      'RocateerImagePicker native module is unavailable or missing `open` method.'
    );
  }

  const normalized = normalizeOptions(options);
  const raw: any = await RocateerImagePicker.open(normalized);

  const normalizeAsset = (a: any): ImageAsset | null => {
    if (!a) return null;
    // String -> assume it's a URI
    if (typeof a === 'string') {
      return { uri: a };
    }
    if (typeof a === 'object') {
      // Accept several common keys from native layers
      const uri = (a as any).uri ?? (a as any).path ?? (a as any).url ?? null;
      if (!uri || typeof uri !== 'string') return null;
      return {
        uri,
        width: typeof a.width === 'number' ? a.width : undefined,
        height: typeof a.height === 'number' ? a.height : undefined,
        fileName: typeof a.fileName === 'string' ? a.fileName : undefined,
        fileSize: typeof a.fileSize === 'number' ? a.fileSize : undefined,
        type: typeof a.type === 'string' ? a.type : undefined,
      };
    }
    return null;
  };

  if (Array.isArray(raw)) {
    return (raw.map(normalizeAsset).filter(Boolean) as ImageAsset[]) || [];
  }
  const single = normalizeAsset(raw);
  return single ? [single] : [];
}

/**
 * Convenience helper for single selection. Always returns the first asset or `null` when cancelled.
 */
export async function pickImage(
  options?: Omit<PickerOptions, 'selectionMode' | 'maxSelection'>
): Promise<ImageAsset | null> {
  const list = await pickImages({ ...options, selectionMode: 'single' });
  return list[0] ?? null;
}

/**
 * Export the raw native module as a fallback for advanced use-cases.
 */
export default {
  pickImages,
  pickImage,
  /** Raw native module (unstable). */
  native: RocateerImagePicker,
};
