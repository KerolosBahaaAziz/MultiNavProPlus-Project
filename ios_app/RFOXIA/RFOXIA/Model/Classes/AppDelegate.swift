//
//  AppDelegate.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 15/04/2025.
//

import UIKit

class AppDelegate: NSObject, UIApplicationDelegate {
    var orientationLock = UIInterfaceOrientationMask.all

    func application(_ application: UIApplication,
                     supportedInterfaceOrientationsFor window: UIWindow?) -> UIInterfaceOrientationMask {
        return orientationLock
    }
}
