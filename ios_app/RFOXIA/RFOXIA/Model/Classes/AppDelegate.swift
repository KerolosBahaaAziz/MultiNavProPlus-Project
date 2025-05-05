//
//  AppDelegate.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 15/04/2025.
//

import UIKit
import FirebaseCore
import GoogleSignIn

class AppDelegate: NSObject, UIApplicationDelegate {

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        FirebaseApp.configure()
        return true
    }
    
    func application(_ app: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey : Any] = [:]) -> Bool {
        return GIDSignIn.sharedInstance.handle(url)
    }
    
    func application(_ application: UIApplication, supportedInterfaceOrientationsFor window: UIWindow?) -> UIInterfaceOrientationMask {
        if OrientationHelper.forceLandscapeOnLaunch {
            return [.landscapeLeft, .landscapeRight]
        }
        else {
            return  [.landscapeLeft , .landscapeRight, .portrait]
        }
    }

}
