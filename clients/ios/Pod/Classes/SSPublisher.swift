//
//  SSPublisher.swift
//  samsara-ios-sdk
//
//  Created by Sathyavijayan Vittal on 13/04/2015.
//  Copyright © 2015-2017 Samsara's authors.
//

import Foundation

public class SSPublisher : NSObject {

    class func send(events: [(Int, SSEvent)], samsaraConfig: SSConfig, completionHandler: ([(Int, SSEvent)], Bool, String) -> Void) {
        var items:[SSEvent] = events.map({$1})
        send(items, samsaraConfig: samsaraConfig) {(e, err, err_msg) -> Void in
            completionHandler(events, err, err_msg)
        }
    }

    class func send(events: [SSEvent], samsaraConfig: SSConfig, completionHandler: ([SSEvent], Bool, String) -> Void) {
        var eventsURL = NSURL(string: SSURLForPath("events", config: samsaraConfig))

        if let url = eventsURL {
            var req = NSMutableURLRequest(URL: url)
            req.setValue("application/json", forHTTPHeaderField: "Content-Type")
            req.setValue("application/json", forHTTPHeaderField: "Accept")

            var timestamp = NSNumber(longLong: Int64(NSDate().timeIntervalSince1970 * 1000))
            req.setValue(timestamp.stringValue, forHTTPHeaderField:"X-Samsara-publishedTimestamp")

            req.HTTPMethod = "POST"

            var body:String = SSJSONStringify(events, prettyPrinted: false)

            req.HTTPBody = body.dataUsingEncoding(NSUTF8StringEncoding)

            let task = NSURLSession.sharedSession().dataTaskWithRequest(req) { (data,response,error) -> Void in

                var isError = false
                var errorMessage:String = ""

                if let resp = response {
                    var httpResponse = resp as! NSHTTPURLResponse
                    if (httpResponse.statusCode != 202) {
                        isError = true
                        errorMessage = "Samsara returned HTTP:\(httpResponse.statusCode)"
                        NSLog("SSPublisher: Error: \(errorMessage)")
                    }
                } else if let err = error {
                    isError = true
                    errorMessage = err.localizedDescription
                    NSLog("SSPublisher: Error: \(err.localizedDescription)")
                }

                completionHandler(events, isError, errorMessage)
            }

            task.resume()
        } else {
            NSLog("SSPublisher: Unable to create URL. This should never happen unless the URL passed is invalid.")
        }
    }
}
