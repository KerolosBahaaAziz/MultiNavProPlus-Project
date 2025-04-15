//
//  AppDelegate.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 15/04/2025.
//

import UIKit

class AppDelegate: NSObject, UIApplicationDelegate {

    func application(_ application: UIApplication, supportedInterfaceOrientationsFor window: UIWindow?) -> UIInterfaceOrientationMask {
        if OrientationHelper.forceLandscapeOnLaunch {
            return [.landscapeLeft, .landscapeRight]
        }
        else {
            return  [.landscapeLeft , .landscapeRight, .portrait]
        }
    }

}
