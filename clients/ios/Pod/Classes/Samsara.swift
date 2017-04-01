//
//  Samsara.swift
//  SamsaraSDK
//
//  Created by Sathyavijayan Vittal on 04/04/2015.
//  Copyright © 2015-2017 Samsara's authors.
//

import Foundation
import UIKit
import Reachability

/** Global Definitions **/
public let kSSAPIVersion:String =  "v1"
public typealias SSEvent = [String:AnyObject]

public let kSSErrorDomain:String = "Samsara"
public let kSSEventValidationErrorCode:Int = 0
public let kSSConfigValidationErrorCode:Int = 1
public let kSSPublishErrorCode:Int = 2
public let kSSInvalidConfigErrorCode:Int = 3
public let kSSInitializationErrorCode:Int = 4

public typealias SSConfig = [String:AnyObject]
private let defaultConfiguration:SSConfig = [
    "maxBufferSize": 1000,
    "publishInterval": 30
]

private let kSSInvalidConfigError = NSError(domain: kSSErrorDomain, code: kSSInvalidConfigErrorCode, userInfo: ["message": "Invalid Config"])
private let kSSArchiveFileName = "\(NSTemporaryDirectory())/samsara_archive.dat"

public class Samsara : NSObject {

    var samsaraConfig: SSConfig?

    var buffer:SSRingBuffer<SSEvent>

    static var instance: Samsara?

    var scheduledTask:SSScheduledTask?

    var publishEventQueue: NSOperationQueue

    var isNetworkReachable:Bool = false

    var networkType:AnyObject = NSNull()

    //Internal flag to disable app lifecycle
    //notifications and persistence during
    //testing
    internal static var __SS_IS_TEST_ENV:Bool = false

    public class var sharedInstance: Samsara? {
        get {
            if (instance == nil) {
                NSLog("Samsara: Warning: sharedInstance called before initializateWithConfig.")
            }

            return instance
        }
    }

    init(config: SSConfig) {
        self.samsaraConfig = config
        self.buffer = SSRingBuffer(maxSize: config["maxBufferSize"] as! Int)
        self.publishEventQueue = NSOperationQueue()
        self.publishEventQueue.maxConcurrentOperationCount = 1

        super.init()

        self.scheduledTask = SSScheduledTask(Q: publishEventQueue,
            interval: config["publishInterval"] as! Int,
            task: {
            self.unsafeFlushBuffer()
        })

        if (!Samsara.__SS_IS_TEST_ENV) {
            //Load
            loadBufferFromArchive(kSSArchiveFileName, buffer: self.buffer)
            setupReachability()
            setupAppLifecycleNotifications()
            self.scheduledTask?.start()
        }
    }

    public class func initializeWithConfig(config: SSConfig) -> NSError? {

        if(instance != nil) {
            return NSError(domain: kSSErrorDomain, code: kSSInitializationErrorCode, userInfo: ["message":"Samsara already initialised"])
        }

        var mergedConf = defaultConfiguration
        mergedConf.concat(config)

        if let err = validateConfig(mergedConf) {
            return err
        } else {
            instance = Samsara(config: mergedConf)
        }

        return nil
    }

    //Samsara Configuration
    private static var samsaraConfigSchema:[String:Any] = [
        "url": [V.Present],
        "maxBufferSize": [V.Present, V.Integer],
        "publishInterval": [V.Present, V.Integer]
    ]

    class private func validateConfig(config:SSConfig) -> NSError? {
        if let error = SchemaValidator.validate(config, schema: samsaraConfigSchema) {
            return NSError(domain: kSSErrorDomain, code: kSSConfigValidationErrorCode, userInfo: error.userInfo)
        } else {
            return nil
        }
    }

    public func getConfiguration() -> SSConfig? {
        return samsaraConfig
    }


    /**
    Sets up Network reachability
    */
    func setupReachability() {
        //Setup Reachability
        var reachability:Reachability = Reachability.reachabilityForInternetConnection()
        //Reachability(hostName: config["url"] as! String)

        self.setReachabilityFlags(reachability)

        NSNotificationCenter.defaultCenter().addObserver(self,
            selector: Selector("networkReachabilityChanged:"),
            name: kReachabilityChangedNotification,
            object: nil)
        reachability.startNotifier()
    }

    /**
    Listen to app lifecycle events
    */
    func setupAppLifecycleNotifications() {
        //Register for app lifecycle notifications
        /**
        UIApplicationWillTerminateNotification
        UIApplicationWillResignActiveNotification
        UIApplicationDidBecomeActiveNotification
        UIApplicationWillEnterForegroundNotification
        UIApplicationDidEnterBackgroundNotification
        UIApplicationDidReceiveMemoryWarningNotification
        UIApplicationSignificantTimeChangeNotification
        UIApplicationDidFinishLaunchingNotification
        */
        NSNotificationCenter.defaultCenter().addObserver(self,
            selector: Selector("applicationWillTerminate:"),
            name: UIApplicationWillTerminateNotification,
            object: nil)
        NSNotificationCenter.defaultCenter().addObserver(self,
            selector: Selector("applicationWillResignActive:"),
            name: UIApplicationWillResignActiveNotification,
            object: nil)
        NSNotificationCenter.defaultCenter().addObserver(self,
            selector: Selector("applicationDidBecomeActive:"),
            name: UIApplicationDidBecomeActiveNotification,
            object: nil)
        NSNotificationCenter.defaultCenter().addObserver(self,
            selector: Selector("applicationWillEnterForeground:"),
            name:UIApplicationWillEnterForegroundNotification,
            object: nil)
        NSNotificationCenter.defaultCenter().addObserver(self,
            selector: Selector("applicationWillEnterBackground:"),
            name:UIApplicationDidEnterBackgroundNotification,
            object: nil)
        NSNotificationCenter.defaultCenter().addObserver(self,
            selector: Selector("didReceiveMemoryWarning:"),
            name:UIApplicationDidReceiveMemoryWarningNotification,
            object: nil)
        NSNotificationCenter.defaultCenter().addObserver(self,
            selector: Selector("significantTimeChange:"),
            name:UIApplicationSignificantTimeChangeNotification,
            object: nil)
        NSNotificationCenter.defaultCenter().addObserver(self,
            selector: Selector("didFinishLaunching:"),
            name:UIApplicationDidFinishLaunchingNotification,
            object: nil)
    }


    /**
    Publish Samsara Event.

    :returns: Samsara Event passed if successful, NSError on validation errors.
    */
    public func publishEvents(events: [SSEvent], completionHandler: ((NSError?) -> Void)? = nil) -> NSError? {
        var enrichedEvents: [SSEvent] = events.map({self.enrichEvent($0)})

        if let err = validateEvents(enrichedEvents) {
            return err
        }

        SSPublisher.send(enrichedEvents, samsaraConfig: samsaraConfig! ,
            completionHandler: { (events: [SSEvent], isError: Bool, errorMessage: String) -> Void in
                var error:NSError? = (!isError) ? nil: NSError(domain: kSSErrorDomain, code: kSSPublishErrorCode, userInfo: ["message": errorMessage])
                if let ch = completionHandler {
                    ch(error)
                }
        })

        return nil
    }

    /**
    Publish Samsara Event.

    :returns: Samsara Event passed if successful, NSError otherwise.
    */
    public func recordEvent(event: SSEvent, completionHandler: (() -> Void)? = nil) -> NSError? {
        var enrichedEvent: SSEvent = enrichEvent(event)

        if let err = validateEvent(enrichedEvent) {
            return err
        }

        buffer.push(enrichedEvent, withCompletionHandler: completionHandler)

        return nil
    }

    private var samsaraEventSchema:[String:Any] = [
        "timestamp": [V.Present, V.Integer],
        "sourceId": [V.Present],
        "eventName": [V.Present]
    ]

    private func validateEvent(event:SSEvent) -> NSError? {
        if let error = SchemaValidator.validate(event, schema: samsaraEventSchema) {
            return NSError(domain: kSSErrorDomain, code: kSSEventValidationErrorCode, userInfo: error.userInfo)
        } else {
            return nil
        }
    }

    private func validateEvents(events: [SSEvent]) -> NSError? {
        var errors: [NSError] = []

        for event in events {
            if let err = validateEvent(event) {
                errors.append(err)
            }
        }

        return (errors.isEmpty) ? nil : NSError(domain: kSSErrorDomain, code: kSSEventValidationErrorCode, userInfo: ["errors": errors])
    }

    /**
    Enrich the event with the following properties:

    a. timestamp    - if not already supplied by the user
    b. sourceId     - default to UDID if not supplied by the user
    c. deviceInfo   - See SSDeviceInfo.swift, if not supplied by the user.
    */
    private func enrichEvent(event:SSEvent) -> SSEvent {
        var additionalInfo:SSEvent = SSDeviceInfo.getDeviceInfo()

        //Note: The convoluted casting in the timestamp value is to
        //workaround a bug where Swift Int on iPhone 5 is only is 32 bit.
        additionalInfo["timestamp"] = NSNumber(longLong: Int64(NSDate().timeIntervalSince1970 * 1000))
        additionalInfo["sourceId"] = additionalInfo["UDID"]
        additionalInfo["isNetworkReachable"] = self.isNetworkReachable
        additionalInfo["networkType"] = self.networkType

        var result:SSEvent = event
        result.merge(additionalInfo)

        return result
    }

    /**
    Flushes the SSRingBuffer to Samsara API and removes items that were successfully pushed
    */
    func unsafeFlushBuffer(completionHandler: ((buffer: SSRingBuffer<SSEvent>) -> Void)? = nil) {
        var snapshot:Array<(Int, SSEvent)>? = self.buffer.snapshot()

        if let snap = snapshot {
            SSPublisher.send(snap, samsaraConfig: samsaraConfig!) {(events: [(Int, SSEvent)], isError: Bool, errorMessage: String) -> Void in
                if(!isError) {
                    self.buffer.remove(events) {
                        if let cb = completionHandler {
                            cb(buffer: self.buffer)
                        }
                    }
                } else {
                    if let cb = completionHandler {
                        cb(buffer: self.buffer)
                    }
                }
            }
        }
    }

    func setReachabilityFlags(reachability:Reachability) {
        self.isNetworkReachable = reachability.isReachable()

        if(reachability.isReachable()) {
            if(reachability.isReachableViaWiFi()) {
                self.networkType = "WiFi"
            } else if(reachability.isReachableViaWWAN()) {
                self.networkType = "WWAN"
            } else {
                self.networkType = NSNull() //should never happen
            }
        } else {
            self.networkType = NSNull()
        }

        NSLog("Samsara: Network status: reachable=\(self.isNetworkReachable) type=\(self.networkType)")
    }

    /**
    Flushes the buffer as a background task.
    */
    func flushInBackground() {
        let operation = NSOperation()
        operation.completionBlock = {
            self.unsafeFlushBuffer({ (buffer) -> Void in
                NSLog("Samsara: Flushed buffer successfully in the background.")
            })
        }

        let backgroundTaskID = UIApplication.sharedApplication().beginBackgroundTaskWithExpirationHandler { () -> Void in
            operation.cancel()
        }

        self.publishEventQueue.addOperations([operation], waitUntilFinished: true)

        UIApplication.sharedApplication().endBackgroundTask(backgroundTaskID)
    }

    func archiveBuffer(pathToFile: String) -> NSError? {
        if let snapshot = buffer.snapshot() {
            var items:[SSEvent] = snapshot.map({$0.1})
            var result:Bool = NSKeyedArchiver.archiveRootObject(items, toFile: pathToFile)
            if(!result) {
                return NSError(domain: "Samsara", code: 0, userInfo: nil)
            } else {
                NSLog("Samsara: Archived \(items.count) events.")
            }
        }

        return nil
    }

    func loadBufferFromArchive(pathToFile: String, buffer: SSRingBuffer<SSEvent>) -> Bool {
        if let events = NSKeyedUnarchiver.unarchiveObjectWithFile(pathToFile) as? [SSEvent] {
            buffer.push(events)
            NSLog("Samsara: Loaded \(events.count) items")
            return true
        }

        //This is called when the app restarts. Even if unarchive failed, the best
        //course of action is to delete the file to avoid consistency issues
        //and sending stale events to samsara.
        do {
          try NSFileManager.defaultManager().removeItemAtPath(kSSArchiveFileName)
        } catch {
            NSLog("Samsara: Failed to cleanup the archive file")
        }

        return false
    }


    //App Lifecycle notifications
    func applicationWillTerminate(notification: NSNotification) {
        // Stop the ScheduledTask
        self.scheduledTask?.end()
        recordEvent(["eventName": "app.willterminate"])
        if let err = archiveBuffer(kSSArchiveFileName) {
            NSLog("Samsara: Failed to archive buffer")
        } else {
            NSLog("Samsara: Succesfully archived buffer")
        }
    }

    func applicationWillResignActive(notification: NSNotification) {
        recordEvent(["eventName": "app.willresignactive"])
    }

    func applicationDidBecomeActive(notification: NSNotification) {
        recordEvent(["eventName": "app.didbecomeactive"])

    }

    func applicationWillEnterForeground(notification: NSNotification) {
        recordEvent(["eventName": "app.willenterforeground"])
        //Start the scheduler if it is stopped
        scheduledTask?.start()
    }

    func applicationWillEnterBackground(notification: NSNotification) {
        // Stop the ScheduledTask
        self.scheduledTask?.end()
        recordEvent(["eventName": "app.willenterbackground"])
        flushInBackground()
    }

    func didReceiveMemoryWarning(notification: NSNotification) {
        recordEvent(["eventName": "app.didreceivememorywarning"])
    }

    func didFinishLaunching(notification: NSNotification) {
        recordEvent(["eventName": "app.didfinishlaunching"])
    }


    func networkReachabilityChanged(notification: NSNotification) {
        var reachability:Reachability = notification.object as! Reachability
        setReachabilityFlags(reachability)
    }
}
