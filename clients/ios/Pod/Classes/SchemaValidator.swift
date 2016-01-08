//
//  SchemaValidator.swift
//  samsara-ios-sdk
//  
//  Schema Validation Utility inspired by Prismatic Schema for
//  clojure. This utility will be extracted as a separate Pod
//  soon.
//
//  Created by Sathyavijayan Vittal on 27/05/2015.
//  Copyright (c) 2015 Sathyavijayan Vittal. All rights reserved.
//

import Foundation

/**
Validations Supported:

|----+-------------+------------------+-------------------------------|
| No | Function    | Description      | Works On                      |
|----+-------------+------------------+-------------------------------|
|  1 | Present     | Is Not Nil       | AnyObject                     |
|  2 | Not Empty   | Is Not Empty.    | (String, Array, HashMap, Set) |
|  3 | Equals      | Is Equal to.     | AnyObject                     |
|  4 | Integer     | Is an Integer    | AnyObject                     |
|  5 | Numeric     | Is Numeric       | AnyObject                     |
|  6 | IsOfLength  | Is Of Length     | String                        |
|  7 | Between     | Is Between       | Number (Int, Float, Double)   |
|  8 | LessThan    | Is less than     | Numbers (Int, Float, Double)  |
|  9 | GreaterThan | Is greater than  | Numbers (Int, Float, Double)  |
| 10 | Email       | Is Email         | String                        |
| 11 | URL         | Is URL           | String                        |
| 12 | Regex       | Does match Regex | String                        |
|    | ...         |                  |                               |

*/

typealias _Validator = (AnyObject?) -> (Bool, String?)
typealias V = SchemaValidator

class SchemaValidator {
    
    private static let NSERROR_DOMAIN = "SchemaValidator"
    
    /* Regex Patterns */
    private static let email_pattern = "^([a-z0-9_\\.-]+)@([\\da-z\\.-]+)\\.([a-z\\.]{2,6})$"
    private static let url_pattern = "^(https?:\\/\\/)?([\\da-z\\.-]+)\\.([a-z\\.]{2,6})([\\/\\w \\.-]*)*\\/?$"
    
    /** Messages TODO: Move to a bundle like setup **/
    private static let messages:[String:String] = [
        "Present": "is required.",
        "Integer": "must be an Integer.",
        "Equals" : "is not equal to",
        "Numeric": "must be a number",
        "Regex":"must match pattern ",
        "URL" : "must be an URL.",
        "Email": "must be an Email."]
    
    /**
    Returns Error Message for Key.
    :returns: Error Message for a given key.
    */
    class func message(key: String) -> String {
        if let msg = SchemaValidator.messages[key] {
            return msg
        }
        return ""
    }
    
    /**
    Checks if the object passed is nil
    
    :returns: True if object is not nil, false otherwise.
    */
    class func Present(obj: AnyObject?) -> (Bool, String?) {
        if let o:AnyObject = obj {
            return  (true, nil)
        } else {
            return  (false, message("Present"))
        }
    }
    
    /**
    Checks if the object passed is an Int
    
    :returns: True if object is an Integer, false otherwise.
    */
    class func Integer(obj:AnyObject?) -> (Bool, String?) {
        if let n:Int = (obj as? Int) {
            return  (true, nil)
        } else {
            return  (false, message("Integer"))
        }
    }
    
    /**
    Checks if the object passed is numeric.
    
    :returns: True if the object passed is a number, false otherwise
    */
    class func Numeric(obj: AnyObject?) -> (Bool,String?) {
        if let n:NSNumber = (obj as? NSNumber) {
            return  (true, nil)
        } else {
            return (false, message("Numeric"))
        }
    }
    
    /**
    Checks if the object is equal to the value.
    
    :returns: True if the object is equal to value, false otherwise
    */
    class func Equals(value: NSObject) -> (AnyObject?) -> (Bool, String?) {
        
        return {(obj:AnyObject?) -> (Bool,String?) in
            
            if let o:NSObject = (obj as? NSObject) {
                if o == value {
                    return (true, nil)
                }
            }
            var msg = self.message("Equals")
            return  (false, "\(msg) \(value).")
        }
    }
    
    /**
    Checks if the object passed matches the Regular Expression.
    Note that this will work only for Strings. If the object is of
    a different type, the validation will just fail.
    
    :returns: True if the object matches regex, false otherwise.
    */
    class func Regex(regex: String, msg:String = " must match pattern") -> (AnyObject?) -> (Bool,String?) {
        return {(obj: AnyObject?) -> (Bool, String?) in
            if let o:NSString = (obj as? NSString) {
                return ((o.rangeOfString(regex, options: .RegularExpressionSearch).location != NSNotFound), nil)
            } else {
                return  (false, "\(msg) \(regex).")
            }
        }
    }
    
    /**
    Checks if the object passed is an URL.
    Note that this will only work for Strings. If the object is of
    a different type, the validation will just fail.
    
    :returns: True if the object passed is an URL, false otherwise.
    */
    class func URL(obj: AnyObject?) -> (Bool, String?) {
        var result = SchemaValidator.Regex(SchemaValidator.url_pattern)(obj)
        if result.0 {
            return (result.0, nil)
        } else {
            return (result.0, (message("URL")))
        }
    }
    
    /**
    Checks if the object passed is an email.
    Note that this will work only for Strings. If the object is of
    a different type, the validation will just fail.
    
    :returns: True if the object passed is an Email, false otherwise.
    */
    class func Email(obj: AnyObject?) -> (Bool, String?) {
        var result = SchemaValidator.Regex(SchemaValidator.email_pattern)(obj)
        if result.0 {
            return (result.0, nil)
        } else {
            return (result.0, message("Email"))
        }
    }
    
    /**
    
    Object Types:
    "(implicit)root": Array[Objects] <or> Object
    
    
    | Type           | Approach                                   |
    |----------------+--------------------------------------------|
    | Array[Objects] | Apply Schema to all the items in the Array |
    | Object         | Apply Schema to the Object                 |
    
    Schema:
    "key": Value <or> Array[Value] <or> Object <or> Array[Object]
    
    Iterate through the keys in Schema:
    * Get value for key from the object to validate
    
    | Value Type | Schema Type       | Approach                                                |
    |------------+-------------------+---------------------------------------------------------|
    | Value      | Array[Validators] | Apply Each Validator to the Value and collect errors.   |
    | [Value]    | Array[Validators] | Apply Each Validator to the [Value] and collect errors. |
    |            |                   | Support only validators that apply to arrays.           |
    | Object     | SubSchema         | Validate the object against the  SubSchema.             |
    | [Object]   | SubSchema         | Validate each object against the SubSchema.             |
    
    */
    
    class func applyValidators(obj: AnyObject?, validators: [_Validator]) -> [String]? {
        
        var errors:[String] = []
        
        for validator in validators {
            var result = validator(obj)
            
            if !result.0 {
                if let err = result.1 {
                    errors.append(err)
                } else {
                    errors.append("Unknown Error!")
                }
            }
        }
        
        if errors.count > 0 {
            return errors
        } else {
            return nil
        }
    }
    
    
    private class func validateObject(object: [NSObject: AnyObject], schema: [String:Any] ) -> [String: [String]]? {
        var errors: [String: [String]] = [:]
        
        for (k,v) in schema {
            
            if let validators = v as? [_Validator] {
                var v_errors:[String]? = SchemaValidator.applyValidators(object[k], validators: validators)
                if let errs = v_errors {
                    errors[k] = errs
                }
            } else {
                errors["_"] = ["SubSchema Validation NOT supported yet!"]
            }
        }
        
        return (errors.isEmpty) ? nil : errors
    }
    
    class func validate(object: [NSObject:AnyObject], schema: [String:Any]) -> NSError? {
        if let error = validateObject(object, schema: schema) {
            return NSError(domain: "SchemaValidator", code: 0, userInfo: error)
        }
        
        return nil
    }
}

