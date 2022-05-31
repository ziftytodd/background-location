var capacitorBackgroundLocation = (function (exports, core) {
    'use strict';

    const BackgroundLocation = core.registerPlugin('BackgroundLocation', {
        web: () => Promise.resolve().then(function () { return web; }).then(m => new m.BackgroundLocationWeb()),
    });

    class BackgroundLocationWeb extends core.WebPlugin {
        addWatcher(options) {
            console.log('Add watcher', options);
            return Promise.resolve(undefined);
        }
        doCheckPermissions() {
            return Promise.resolve({
                status: 'always',
                available: true,
                useSettingsPage: false,
                appWhitelisted: true,
                ignoreBatteryOptimizations: true
            });
        }
        doRequestAlwaysPermission() {
            return Promise.resolve({ status: 'always' });
        }
        doRequestIgnoreBatteryOptimization() {
            return Promise.resolve({ status: 'always' });
        }
        doRequestIgnoreDataSaver() {
            return Promise.resolve({ status: 'always' });
        }
        doRequestPermissions() {
            return Promise.resolve({ status: 'always' });
        }
        doRequestWhenInUsePermission() {
            return Promise.resolve({ status: 'always' });
        }
        openSettings() {
            return Promise.resolve(undefined);
        }
        removeWatcher(options) {
            console.log('Remove watcher', options);
            return Promise.resolve(undefined);
        }
        startMonitoring(options) {
            console.log('Start monitoring', options);
            return Promise.resolve(undefined);
        }
        stayAwake() {
            return Promise.resolve(undefined);
        }
        stopMonitoring() {
            return Promise.resolve(undefined);
        }
    }

    var web = /*#__PURE__*/Object.freeze({
        __proto__: null,
        BackgroundLocationWeb: BackgroundLocationWeb
    });

    exports.BackgroundLocation = BackgroundLocation;

    Object.defineProperty(exports, '__esModule', { value: true });

    return exports;

}({}, capacitorExports));
//# sourceMappingURL=plugin.js.map
