import { WebPlugin } from '@capacitor/core';
export class BackgroundLocationWeb extends WebPlugin {
    start() {
        if (!this.locUpdateTimer) {
            this.locUpdateTimer = setInterval(() => {
                this.notifyListeners('locationUpdate', {
                    location: {
                        latitude: 33.7542194,
                        longitude: -84.3914007,
                        accuracy: 10,
                        altitude: 0,
                        altitudeAccuracy: 10,
                        heading: 0,
                        speed: 0,
                        time: new Date().getTime(),
                    }
                });
            }, 5000);
        }
    }
    addWatcher(options) {
        console.log('Add watcher', options);
        this.start();
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
        this.start();
        return Promise.resolve(undefined);
    }
    stayAwake() {
        return Promise.resolve(undefined);
    }
    stopMonitoring() {
        return Promise.resolve(undefined);
    }
}
//# sourceMappingURL=web.js.map