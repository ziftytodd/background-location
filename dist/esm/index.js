import { registerPlugin } from '@capacitor/core';
const BackgroundLocation = registerPlugin('BackgroundLocation', {
// web: () => import('./web').then(m => new m.BackgroundLocationWeb()),
});
export * from './definitions';
export { BackgroundLocation };
//# sourceMappingURL=index.js.map