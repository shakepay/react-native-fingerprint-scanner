import { NativeModules } from 'react-native';
import createError from './createError';

const { ReactNativeFingerprintScanner } = NativeModules;

export default () => {
  return ReactNativeFingerprintScanner.isKeyguardSecure()
    .catch(error => {
      throw createError(error.message);
    });
}
