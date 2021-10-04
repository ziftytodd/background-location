import type { PluginListenerHandle } from '@capacitor/core';
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
export interface LocationErrorEvent {
    reason: string;
    code: boolean;
}
export interface LocationUpdateEvent {
    location: Location;
}
export interface GpsLog {
    message: string;
    code?: string;
}
export declare type ErrorListener = (event: LocationErrorEvent) => void;
export declare type LocationListener = (event: LocationUpdateEvent) => void;
export declare type LogListener = (event: GpsLog) => void;
export interface BackgroundLocationPlugin {
    addWatcher(options: WatcherOptions): Promise<void>;
    removeWatcher(options: {
        id: string;
    }): Promise<void>;
    openSettings(): Promise<void>;
    doCheckPermissions(): Promise<CheckPermissionsResult>;
    doRequestPermissions(): Promise<RequestPermissionsResult>;
    stayAwake(): Promise<void>;
    startMonitoring(options: WatcherOptions): Promise<void>;
    stopMonitoring(): Promise<void>;
    doRequestWhenInUsePermission(): Promise<RequestPermissionsResult>;
    doRequestAlwaysPermission(): Promise<RequestPermissionsResult>;
    doRequestIgnoreDataSaver(): Promise<RequestPermissionsResult>;
    doRequestIgnoreBatteryOptimization(): Promise<RequestPermissionsResult>;
    addListener(eventName: 'error', listenerFunc: ErrorListener): Promise<PluginListenerHandle> & PluginListenerHandle;
    addListener(eventName: 'locationUpdate', listenerFunc: LocationListener): Promise<PluginListenerHandle> & PluginListenerHandle;
    addListener(eventName: 'gpsLog', listenerFunc: LogListener): Promise<PluginListenerHandle> & PluginListenerHandle;
}
