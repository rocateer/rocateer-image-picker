import { NativeModules } from 'react-native';

const { RocateerImagePicker } = NativeModules;

// 네이티브 모듈이 없을 경우를 대비한 간단한 경고
if (!RocateerImagePicker) {
  console.warn("RocateerImagePicker native module not found. Did you link it properly?");
}

// 네이티브 모듈 객체 전체를 export 합니다.
export default RocateerImagePicker;
