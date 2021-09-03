var capacitorBackgroundLocation = (function (exports, core) {
	'use strict';

	const BackgroundLocation = core.registerPlugin('BackgroundLocation', {
	// web: () => import('./web').then(m => new m.BackgroundLocationWeb()),
	});

	exports.BackgroundLocation = BackgroundLocation;

	Object.defineProperty(exports, '__esModule', { value: true });

	return exports;

}({}, capacitorExports));
//# sourceMappingURL=plugin.js.map
