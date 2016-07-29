//
//  SSRingBuffer.swift
//  samsara-ios-sdk
//
//  A Simple Ring Buffer implementation using Serial Queue.
//
//  Created by Sathyavijayan Vittal on 13/04/2015.
//  Copyright (c) 2015 Sathyavijayan Vittal. All rights reserved.
//

import Foundation


class SSRingBuffer<T> {

    private var buffer: Array<(Int,T)>

    private var Q:dispatch_queue_t

    private var maxSize:Int

    private var counter:Int

    init(maxSize size: Int) {
        Q = dispatch_queue_create(NSUUID().UUIDString,DISPATCH_QUEUE_SERIAL)
        maxSize = size
        counter = 0

        buffer = Array<(Int, T)>()
    }

    func push(objects: [T], withCompletionHandler completionHandler: (() -> Void)? = nil) {
        dispatch_async(Q, {() -> Void in
            for object in objects {
                self.unsafePush(object)
            }

            //call completion handler.
            if let ch = completionHandler {
                ch()
            }
        })
    }

    func push(object: T, withCompletionHandler completionHandler: (() -> Void)? = nil) {
        dispatch_async(Q, {() -> Void in
            self.unsafePush(object)
            //call completion handler.
            if let ch = completionHandler {
                ch()
            }
        })
    }

    private func unsafePush(object: T) {
        if self.buffer.count + 1 > self.maxSize {
            //Remove the first element.
            self.buffer.removeAtIndex(0)
        }
        //append new item as a tuple with the generated id
        self.buffer.append((counter++, object))
    }

    func snapshot() -> Array<(Int, T)>? {
        if buffer.count == 0 {
            return nil
        } else {
            return buffer
        }
    }

    func remove(items: Array<(Int, T)>, withCompletionHandler completionHandler: (() -> Void)? = nil) {
        dispatch_async(Q, {() -> Void in
            var ids_to_remove:[Int] = items.map{ $0.0 }

            self.buffer = self.buffer.filter { (item) -> Bool in
                return !ids_to_remove.contains(item.0)
            }

            //call completion handler.
            if let ch = completionHandler {
                ch()
            }
        })
    }

    func count() -> Int {
        return buffer.count
    }
}
