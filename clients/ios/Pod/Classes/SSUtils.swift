//
//  Utils.swift
//  SamsaraSDK
//
//  Created by Sathyavijayan Vittal on 08/04/2015.
//  Copyright (c) 2015 Sathyavijayan Vittal. All rights reserved.
//

import Foundation

func SSJSONStringify(value: AnyObject, prettyPrinted: Bool = false) -> String {

    //FIXME:
    var options = prettyPrinted ? NSJSONWritingOptions.PrettyPrinted : NSJSONWritingOptions(rawValue: 0)

    if NSJSONSerialization.isValidJSONObject(value) {
        do {
            var data = try NSJSONSerialization.dataWithJSONObject(value, options: options)

            if let string = NSString(data: data, encoding: NSUTF8StringEncoding) {
               return string as String
            }
        } catch _ {
           //TODO: Error handling
        }
    }
    return ""
}

func SSURLForPath(path:String, config: SSConfig) -> String {
    var url:String = config["url"] as! String
    return "\(url)/\(kSSAPIVersion)/\(path)"
}

extension Dictionary {
    /**
    Adds contents of the dictionary supplied
    overwriting existing keys.
    */
    mutating func concat(other:Dictionary) {
        for (key,value) in other {
            self.updateValue(value, forKey:key)
        }
    }

    /**
    Adds contents of the dictionary supplied
    without overwriting existing keys.
    */
    mutating func merge(other:Dictionary) {
        for (key,value) in other {
            if let v = self[key] {
                //Do nothing
            } else {
                self.updateValue(value, forKey:key)
            }
        }
    }

}
