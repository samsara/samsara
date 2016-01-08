//
//  SSScheduledTask.swift
//  samsara-ios-sdk
//  
//  A simple ScheduledTask Implementation that calls a closure
//  periodically.
//
//  Created by Sathyavijayan Vittal on 29/05/2015.
//  Copyright (c) 2015 Sathyavijayan Vittal. All rights reserved.
//

import Foundation

class SSScheduledTask:NSObject {
    
    var interval:Int
    
    var timer:NSTimer?
    
    var sendEventQ: NSOperationQueue
    
    var task: () -> ()
    
    init(Q: NSOperationQueue, interval: Int, task: () -> ()) {
        self.interval = interval
        self.task = task
        self.sendEventQ = Q
        super.init()
        
        //The check for -1 is a hack to allow me to test the sdk properly
        //Not proud of this hack !
        self.setupTimer()
    }
    
    func setupTimer() {
        if interval != -1 {
            timer = NSTimer(timeInterval: Double(interval), target: self, selector: Selector("onTimer"), userInfo: nil, repeats: true)
            NSRunLoop.currentRunLoop().addTimer(timer!, forMode: NSRunLoopCommonModes)
            NSLog("SSScheduledTask: Initialised Timer")
        }
    }
    
    func onTimer() {
        sendEventQ.addOperationWithBlock { () -> Void in
            self.task()
        }
    }
    
    func start() {
        if(timer != nil && timer!.valid) {
            setupTimer()
        }
        
        timer?.fire()
    }
    
    
    func end() {
        timer?.invalidate()
    }
    
}