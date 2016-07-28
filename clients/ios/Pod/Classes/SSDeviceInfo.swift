//
//  SSDeviceInfo.swift
//  samsara-ios-sdk
//
//  Created by Sathyavijayan Vittal on 14/04/2015.
//  Copyright (c) 2015 Sathyavijayan Vittal. All rights reserved.
//

import Foundation
import Locksmith
import SystemConfiguration

class SSDeviceInfo {

    private static let ssUDIDIdentifier = "samsara.io.udid"

    class func getDeviceInfo() -> [String:String] {
        var udid = getUDID()

        var deviceInfo = [
            "device_name": UIDevice.currentDevice().name,
            "system_name": UIDevice.currentDevice().systemName,
            "system_version": UIDevice.currentDevice().systemVersion,
            "model": UIDevice.currentDevice().model,
            "localizedModel": UIDevice.currentDevice().localizedModel,
        ]

        deviceInfo["UDID"] = udid

        return deviceInfo
    }

    class func getUDID () -> String {
        var dictionary = Locksmith.loadDataForUserAccount(ssUDIDIdentifier)

        var s_uid = NSUUID().UUIDString
        dictionary = dictionary ?? [:] //Yuck !!

        if let uid = dictionary!["UDID"] as? String {
            s_uid = uid
        } else {
            do {
                let error = try Locksmith.saveData (["UDID": s_uid], forUserAccount: ssUDIDIdentifier)
            } catch _ {
                //TODO Handle error
            }
        }
        return s_uid
    }
}
