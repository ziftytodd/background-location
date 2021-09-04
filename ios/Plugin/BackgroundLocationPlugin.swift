import Foundation
import Capacitor
import UIKit
import CoreLocation

// Avoids a bewildering type warning.
let null = Optional<Double>.none as Any

func formatLocation(_ location: CLLocation) -> PluginCallResultData {
    return [
        "latitude": location.coordinate.latitude,
        "longitude": location.coordinate.longitude,
        "accuracy": location.horizontalAccuracy,
        "altitude": location.altitude,
        "altitudeAccuracy": location.verticalAccuracy,
        "speed": location.speed < 0 ? 0 : location.speed,
        "bearing": location.course < 0 ? -1 : location.course,
        "heading": location.course < 0 ? -1 : location.course,
        "time": NSNumber(
            value: Int(
                location.timestamp.timeIntervalSince1970 * 1000
            )
        ),
    ]
}

class Watcher {
    var callbackId: String
    let locationManager: CLLocationManager = CLLocationManager()
    private let created = Date()
    private let allowStale: Bool
    private let minMillisBetweenUpdates: Int
    private var lastUpdateTime: Int = 0
    private var isUpdatingLocation: Bool = false
    init(_ id: String, stale: Bool, minMillis: Int) {
        callbackId = id
        allowStale = stale
        minMillisBetweenUpdates = minMillis
    }
    func start(_ newId: String) {
        self.callbackId = newId

        // Avoid unnecessary calls to startUpdatingLocation, which can
        // result in extraneous invocations of didFailWithError.
        if !isUpdatingLocation {
            locationManager.startUpdatingLocation()
            isUpdatingLocation = true
        }
    }
    func stop() {
        if isUpdatingLocation {
            locationManager.stopUpdatingLocation()
            isUpdatingLocation = false
        }
    }
    func isLocationValid(_ location: CLLocation) -> Bool {
        let thisTime = Int(location.timestamp.timeIntervalSince1970 * 1000)
        if (
            ( allowStale || location.timestamp >= created ) &&
            ( (minMillisBetweenUpdates <= 0) || (lastUpdateTime == 0) || ((thisTime - lastUpdateTime) > minMillisBetweenUpdates) )
        ) {
            // print("Got new GPS fix", thisTime, lastUpdateTime, minMillisBetweenUpdates)
            lastUpdateTime = thisTime
            return true
        } else {
            // print("Skip too frequent", thisTime, lastUpdateTime, minMillisBetweenUpdates)
            return false
        }
    }
}

@objc(BackgroundLocationPlugin)
public class BackgroundLocationPlugin : CAPPlugin, CLLocationManagerDelegate {
    private var watcher: Watcher?
    private let locationManager = CLLocationManager()
    private var callPendingPermissions: CAPPluginCall?

    @objc public override func load() {
        UIDevice.current.isBatteryMonitoringEnabled = true
    }

    @objc func addWatcher(_ call: CAPPluginCall) {
        call.keepAlive = true

        // CLLocationManager requires main thread
        DispatchQueue.main.async {
            if let watch = self.watcher {
                return watch.start(call.callbackId)
            } else {
                let background = call.getString("backgroundMessage") != nil
                let watch = Watcher(
                    call.callbackId,
                    stale: call.getBool("stale") ?? false,
                    minMillis: Int(call.getDouble("minMillisBetweenUpdates") ?? 0)
                )
                let manager = watch.locationManager
                manager.delegate = self
                let externalPower = [
                    .full,
                    .charging
                ].contains(UIDevice.current.batteryState)
                manager.desiredAccuracy = (
                    externalPower
                    ? kCLLocationAccuracyBestForNavigation
                    : kCLLocationAccuracyBest
                )
                manager.distanceFilter = call.getDouble(
                    "distanceFilter"
                ) ?? kCLDistanceFilterNone;
                manager.allowsBackgroundLocationUpdates = background
//                 self.watchers.append(watcher)
//                 if call.getBool("requestPermissions") != false {
//                     let status = CLLocationManager.authorizationStatus()
//                     if [
//                         .notDetermined,
//                         .denied,
//                         .restricted,
//                     ].contains(status) {
//                         return (
//                             background
//                             ? manager.requestAlwaysAuthorization()
//                             : manager.requestWhenInUseAuthorization()
//                         )
//                     }
//                     if (
//                         background && status == .authorizedWhenInUse
//                     ) {
//                         // Attempt to escalate.
//                         manager.requestAlwaysAuthorization()
//                     }
//                 }
                self.watcher = watch
                return watch.start(call.callbackId)
            }
        }
    }

    @objc func removeWatcher(_ call: CAPPluginCall) {
        // CLLocationManager requires main thread
        DispatchQueue.main.async {
            if let watch = self.watcher {
                watch.stop()
            }
            return call.resolve()
//             if let callbackId = call.getString("id") {
//                 if let index = self.watchers.firstIndex(
//                     where: { $0.callbackId == callbackId }
//                 ) {
//                     self.watchers[index].locationManager.stopUpdatingLocation()
//                     self.watchers.remove(at: index)
//                 }
//                 if let savedCall = self.bridge?.savedCall(withID: callbackId) {
//                     self.bridge?.releaseCall(savedCall)
//                 }
//                 return call.resolve()
//             }
//             return call.reject("No callback ID")
        }
    }

    @objc func openSettings(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            guard let settingsUrl = URL(
                string: UIApplication.openSettingsURLString
            ) else {
                return call.reject("No link to settings available")
            }

            if UIApplication.shared.canOpenURL(settingsUrl) {
                UIApplication.shared.open(settingsUrl, completionHandler: {
                    (success) in
                    if (success) {
                        return call.resolve()
                    } else {
                        return call.reject("Failed to open settings")
                    }
                })
            } else {
                return call.reject("Cannot open settings")
            }
        }
    }

    @objc func doCheckPermissions(_ call: CAPPluginCall) {
        DispatchQueue.main.async { [self] in
            NSLog("Checking permissions")
            if CLLocationManager.locationServicesEnabled() {
                if #available(iOS 14.0, *) {
                    switch locationManager.authorizationStatus {
                    case .notDetermined:
                        return call.resolve(["status": "notDetermined", "available": false, "useSettingsPage": false, "appWhitelisted": true, "ignoreBatteryOptimizations": true ]);
                    case .restricted:
                        return call.resolve(["status": "restricted", "available": false, "useSettingsPage": true, "appWhitelisted": true, "ignoreBatteryOptimizations": true ]);
                    case .denied:
                        return call.resolve(["status": "denied", "available": false, "useSettingsPage": true, "appWhitelisted": true, "ignoreBatteryOptimizations": true ]);
                    case .authorizedWhenInUse:
                        return call.resolve(["status": "whenInUse", "available": true, "useSettingsPage": true, "appWhitelisted": true, "ignoreBatteryOptimizations": true ]);
                    case .authorizedAlways:
                        return call.resolve(["status": "always", "available": true, "useSettingsPage": false, "appWhitelisted": true, "ignoreBatteryOptimizations": true ]);
                    @unknown default:
                        return call.resolve(["status": "unknown", "available": false, "useSettingsPage": false, "appWhitelisted": true, "ignoreBatteryOptimizations": true ]);
                    }
                } else {
                    // Fallback on earlier versions
                    switch CLLocationManager.authorizationStatus() {
                    case .notDetermined:
                        return call.resolve(["status": "notDetermined", "available": false, "useSettingsPage": false, "appWhitelisted": true, "ignoreBatteryOptimizations": true ]);
                    case .restricted:
                        return call.resolve(["status": "restricted", "available": false, "useSettingsPage": true, "appWhitelisted": true, "ignoreBatteryOptimizations": true ]);
                    case .denied:
                        return call.resolve(["status": "denied", "available": false, "useSettingsPage": true, "appWhitelisted": true, "ignoreBatteryOptimizations": true ]);
                    case .authorizedWhenInUse:
                        return call.resolve(["status": "whenInUse", "available": true, "useSettingsPage": true, "appWhitelisted": true, "ignoreBatteryOptimizations": true ]);
                    case .authorizedAlways:
                        return call.resolve(["status": "always", "available": true, "useSettingsPage": false, "appWhitelisted": true, "ignoreBatteryOptimizations": true ]);
                    @unknown default:
                        return call.resolve(["status": "unknown", "available": false, "useSettingsPage": false, "appWhitelisted": true, "ignoreBatteryOptimizations": true ]);
                    }
                }
            } else {
                return call.resolve(["status": "restricted", "available": false, "useSettingsPage": true, "appWhitelisted": true, "ignoreBatteryOptimizations": true]);
            }
        }
    }

    @objc func doRequestPermissions(_ call: CAPPluginCall) {
        DispatchQueue.main.async { [self] in
            NSLog("Checking perm before requesting")
            if CLLocationManager.locationServicesEnabled() {
                if #available(iOS 14.0, *) {
                    switch locationManager.authorizationStatus {
                    case .notDetermined, .restricted, .denied, .authorizedWhenInUse:
                        NSLog("Requesting permission")
                        self.callPendingPermissions = call;
                        locationManager.delegate = self;
                        locationManager.requestAlwaysAuthorization()
                        return
                    default:
                        return call.resolve(["status": "notRequested"])
                    }
                } else {
                    // Fallback on earlier versions
                    switch CLLocationManager.authorizationStatus() {
                    case .notDetermined, .restricted, .denied, .authorizedWhenInUse:
                        NSLog("Requesting permission")
                        self.callPendingPermissions = call;
                        locationManager.delegate = self;
                        locationManager.requestAlwaysAuthorization()
                        return
                    default:
                        return call.resolve(["status": "notRequested"])
                    }
                }
            } else {
                self.callPendingPermissions = call;
                locationManager.requestAlwaysAuthorization()
            }
        }
    }

    @objc func stayAwake(_ call: CAPPluginCall) {
        call.resolve()
    }

    @objc func doRequestWhenInUsePermission(_ call: CAPPluginCall) {
        DispatchQueue.main.async { [self] in
            NSLog("Checking perm before requesting")
            if CLLocationManager.locationServicesEnabled() {
                if #available(iOS 14.0, *) {
                    if (locationManager.authorizationStatus == .notDetermined) {
                        self.callPendingPermissions = call;
                        locationManager.delegate = self;
                        locationManager.requestWhenInUseAuthorization()
                    } else if ((locationManager.authorizationStatus == .authorizedWhenInUse) || (locationManager.authorizationStatus == .authorizedAlways)) {
                        call.resolve(["status": "granted"])
                    } else {
                        // Open the settings page
                        print("Opening settings page")
                        if let BUNDLE_IDENTIFIER = Bundle.main.bundleIdentifier,
                            let url = URL(string: "\(UIApplication.openSettingsURLString)&path=LOCATION/\(BUNDLE_IDENTIFIER)") {
                            UIApplication.shared.open(url, options: [:], completionHandler: nil)
                        }
                        print("Done opening setting page")
                        call.resolve(["status": "settingsPage"])
                    }
                } else {
                    // Fallback on earlier versions
                    if (CLLocationManager.authorizationStatus() == .notDetermined) {
                        self.callPendingPermissions = call;
                        locationManager.delegate = self;
                        locationManager.requestWhenInUseAuthorization()
                    } else if ((CLLocationManager.authorizationStatus() == .authorizedWhenInUse) || (CLLocationManager.authorizationStatus() == .authorizedAlways)) {
                        call.resolve(["status": "granted"])
                    } else {
                        // Open the settings page
                        print("Opening settings page")
                        if let BUNDLE_IDENTIFIER = Bundle.main.bundleIdentifier,
                            let url = URL(string: "\(UIApplication.openSettingsURLString)&path=LOCATION/\(BUNDLE_IDENTIFIER)") {
                            UIApplication.shared.open(url, options: [:], completionHandler: nil)
                        }
                        print("Done opening setting page")
                        call.resolve(["status": "settingsPage"])
                    }
                }
            } else {
                return call.resolve(["status": "restricted"])
            }
        }
    }

    @objc func doRequestAlwaysPermission(_ call: CAPPluginCall) {
        DispatchQueue.main.async { [self] in
            NSLog("Checking perm before requesting")
            if CLLocationManager.locationServicesEnabled() {
                if #available(iOS 14.0, *) {
                    if (locationManager.authorizationStatus != .authorizedAlways) {
                        print("Opening settings page")
                        if let BUNDLE_IDENTIFIER = Bundle.main.bundleIdentifier,
                            let url = URL(string: "\(UIApplication.openSettingsURLString)&path=LOCATION/\(BUNDLE_IDENTIFIER)") {
                            UIApplication.shared.open(url, options: [:], completionHandler: nil)
                        }
                        print("Done opening setting page")
                        return call.resolve(["status": "settingsPage"])
                    } else {
                        return call.resolve(["status": "granted"])
                    }
                } else {
                    // Fallback on earlier versions
                    if (CLLocationManager.authorizationStatus() != .authorizedAlways) {
                        print("Opening settings page")
                        if let BUNDLE_IDENTIFIER = Bundle.main.bundleIdentifier,
                            let url = URL(string: "\(UIApplication.openSettingsURLString)&path=LOCATION/\(BUNDLE_IDENTIFIER)") {
                            UIApplication.shared.open(url, options: [:], completionHandler: nil)
                        }
                        print("Done opening setting page")
                        return call.resolve(["status": "settingsPage"])
                    } else {
                        return call.resolve(["status": "granted"])
                    }
                }
            } else {
                return call.resolve(["status": "restricted"])
            }
        }
    }

    @objc func doRequestIgnoreDataSaver(_ call: CAPPluginCall) {
        call.resolve(["status": "granted"])
    }

    @objc func doRequestIgnoreBatteryOptimization(_ call: CAPPluginCall) {
        call.resolve(["status": "granted"])
    }

    public func locationManager(
        _ manager: CLLocationManager,
        didFailWithError error: Error
    ) {
        if let watch = self.watcher {
            if let call = self.bridge?.savedCall(withID: watch.callbackId) {
                if let clErr = error as? CLError {
                    if clErr.code == .locationUnknown {
                        // This error is sometimes sent by the manager if
                        // it cannot get a fix immediately.
                        return
                    } else if (clErr.code == .denied) {
                        watch.stop()
                        return call.reject(
                            "Permission denied.",
                            "NOT_AUTHORIZED"
                        )
                    }
                }
                return call.reject(error.localizedDescription, nil, error)
            }
        }
//         if let watcher = self.watchers.first(
//             where: { $0.locationManager == manager }
//         ) {
//             if let call = self.bridge?.savedCall(withID: watcher.callbackId) {
//                 if let clErr = error as? CLError {
//                     if clErr.code == .locationUnknown {
//                         // This error is sometimes sent by the manager if
//                         // it cannot get a fix immediately.
//                         return
//                     } else if (clErr.code == .denied) {
//                         watcher.stop()
//                         return call.reject(
//                             "Permission denied.",
//                             "NOT_AUTHORIZED"
//                         )
//                     }
//                 }
//                 return call.reject(error.localizedDescription, nil, error)
//             }
//         }
    }

    public func locationManagerDidPauseLocationUpdates(_ manager: CLLocationManager) {
        print("Unpausing location updates!")
        manager.startUpdatingLocation()
    }

    public func locationManager(
        _ manager: CLLocationManager,
        didUpdateLocations locations: [CLLocation]
    ) {
        if let location = locations.last {
            if let watch = self.watcher {
                if watch.isLocationValid(location) {
                    if let call = self.bridge?.savedCall(withID: watch.callbackId) {
                        return call.resolve(formatLocation(location))
                    }
                }
            }

//             if let watcher = self.watchers.first(
//                 where: { $0.locationManager == manager }
//             ) {
//                 if watcher.isLocationValid(location) {
//                     if let call = self.bridge?.savedCall(withID: watcher.callbackId) {
//                         return call.resolve(formatLocation(location))
//                     }
//                 }
//             }
        }
    }

    public func locationManager(
        _ manager: CLLocationManager,
        didChangeAuthorization status: CLAuthorizationStatus
    ) {
        // If this method is called before the user decides on a permission, as
        // it is on iOS 14 when the permissions dialog is presented, we ignore
        // it.
        print("Location authorization updated", status.rawValue)

        if status != .notDetermined {
            if let call = self.callPendingPermissions {
                if status == .authorizedAlways {
                    call.resolve(["status": "always"])
                } else if status == .authorizedWhenInUse {
                    call.resolve(["status": "whenInUse"])
                } else {
                    call.resolve(["status": "denied"])
                }
                self.callPendingPermissions = nil
            }

//             if let watcher = self.watchers.first(
//                 where: { $0.locationManager == manager }
//             ) {
//                 return watcher.start()
//             }
        }
    }
}

