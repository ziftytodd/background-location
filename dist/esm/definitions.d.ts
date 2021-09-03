export interface WatcherOptions {
    backgroundMessage?: string;
    backgroundTitle?: string;
    requestPermissions?: boolean;
    stale?: boolean;
    distanceFilter?: number;
    minMillisBetweenUpdates?: number;
}
export interface Location {
    latitude: number;
    longitude: number;
    accuracy: number;
    altitude: number | null;
    altitudeAccuracy: number | null;
    heading: number | null;
    bearing?: number | null;
    speed: number | null;
    time: number;
}
export interface CallbackError extends Error {
    code?: string;
}
export interface CheckPermissionsResult {
    status: string;
    available: boolean;
    useSettingsPage: boolean;
    appWhitelisted?: boolean;
    ignoreBatteryOptimizations?: boolean;
}
export interface RequestPermissionsResult {
    status: string;
}
export interface BackgroundLocationPlugin {
    addWatcher(options: WatcherOptions, callback: (position?: Location, error?: CallbackError) => void): Promise<string>;
    removeWatcher(options: {
        id: string;
    }): Promise<void>;
    openSettings(): Promise<void>;
    doCheckPermissions(): Promise<CheckPermissionsResult>;
    doRequestPermissions(): Promise<RequestPermissionsResult>;
    stayAwake(): Promise<void>;
    doRequestWhenInUsePermission(): Promise<RequestPermissionsResult>;
    doRequestAlwaysPermission(): Promise<RequestPermissionsResult>;
    doRequestIgnoreDataSaver(): Promise<RequestPermissionsResult>;
    doRequestIgnoreBatteryOptimization(): Promise<RequestPermissionsResult>;
}
