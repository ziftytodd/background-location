import {WebPlugin} from '@capacitor/core';

import type {
    BackgroundLocationPlugin,
    CheckPermissionsResult,
    RequestPermissionsResult,
    WatcherOptions
} from './definitions';

export class BackgroundLocationWeb
    extends WebPlugin
    implements BackgroundLocationPlugin {

    addWatcher(options: WatcherOptions): Promise<void> {
        console.log('Add watcher', options);
        return Promise.resolve(undefined);
    }

    doCheckPermissions(): Promise<CheckPermissionsResult> {
        return Promise.resolve({
            status: 'always',
            available: true,
            useSettingsPage: false,
            appWhitelisted: true,
            ignoreBatteryOptimizations: true
        });
    }

    doRequestAlwaysPermission(): Promise<RequestPermissionsResult> {
        return Promise.resolve({ status: 'always' });
    }

    doRequestIgnoreBatteryOptimization(): Promise<RequestPermissionsResult> {
        return Promise.resolve({ status: 'always' });
    }

    doRequestIgnoreDataSaver(): Promise<RequestPermissionsResult> {
        return Promise.resolve({ status: 'always' });
    }

    doRequestPermissions(): Promise<RequestPermissionsResult> {
        return Promise.resolve({ status: 'always' });
    }

    doRequestWhenInUsePermission(): Promise<RequestPermissionsResult> {
        return Promise.resolve({ status: 'always' });
    }

    openSettings(): Promise<void> {
        return Promise.resolve(undefined);
    }

    removeWatcher(options: { id: string }): Promise<void> {
        console.log('Remove watcher', options);
        return Promise.resolve(undefined);
    }

    startMonitoring(options: WatcherOptions): Promise<void> {
        console.log('Start monitoring', options);
        return Promise.resolve(undefined);
    }

    stayAwake(): Promise<void> {
        return Promise.resolve(undefined);
    }

    stopMonitoring(): Promise<void> {
        return Promise.resolve(undefined);
    }

}

