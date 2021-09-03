#import <Foundation/Foundation.h>
#import <Capacitor/Capacitor.h>

// Define the plugin using the CAP_PLUGIN Macro, and
// each method the plugin supports using the CAP_PLUGIN_METHOD macro.
CAP_PLUGIN(BackgroundLocationPlugin, "BackgroundLocation",
    CAP_PLUGIN_METHOD(addWatcher, CAPPluginReturnCallback);
    CAP_PLUGIN_METHOD(removeWatcher, CAPPluginReturnPromise);
    CAP_PLUGIN_METHOD(openSettings, CAPPluginReturnPromise);
    CAP_PLUGIN_METHOD(doCheckPermissions, CAPPluginReturnPromise);
    CAP_PLUGIN_METHOD(doRequestPermissions, CAPPluginReturnPromise);
    CAP_PLUGIN_METHOD(stayAwake, CAPPluginReturnPromise);

    CAP_PLUGIN_METHOD(doRequestWhenInUsePermission, CAPPluginReturnPromise);
    CAP_PLUGIN_METHOD(doRequestAlwaysPermission, CAPPluginReturnPromise);
    CAP_PLUGIN_METHOD(doRequestIgnoreDataSaver, CAPPluginReturnPromise);
    CAP_PLUGIN_METHOD(doRequestIgnoreBatteryOptimization, CAPPluginReturnPromise);
)
