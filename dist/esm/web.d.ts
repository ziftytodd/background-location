import { WebPlugin } from '@capacitor/core';
import type { BackgroundLocationPlugin, CheckPermissionsResult, RequestPermissionsResult, WatcherOptions } from './definitions';
export declare class BackgroundLocationWeb extends WebPlugin implements BackgroundLocationPlugin {
    addWatcher(options: WatcherOptions): Promise<void>;
    doCheckPermissions(): Promise<CheckPermissionsResult>;
    doRequestAlwaysPermission(): Promise<RequestPermissionsResult>;
    doRequestIgnoreBatteryOptimization(): Promise<RequestPermissionsResult>;
    doRequestIgnoreDataSaver(): Promise<RequestPermissionsResult>;
    doRequestPermissions(): Promise<RequestPermissionsResult>;
    doRequestWhenInUsePermission(): Promise<RequestPermissionsResult>;
    openSettings(): Promise<void>;
    removeWatcher(options: {
        id: string;
    }): Promise<void>;
    startMonitoring(options: WatcherOptions): Promise<void>;
    stayAwake(): Promise<void>;
    stopMonitoring(): Promise<void>;
}
