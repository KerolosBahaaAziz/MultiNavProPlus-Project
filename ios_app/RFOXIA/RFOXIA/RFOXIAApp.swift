//
//  RFOXIAApp.swift
//  RFOXIA
//
//  Created by Kerlos on 11/04/2025.
//

import SwiftUI

@main
struct RFOXIAApp: App {
    let persistenceController = PersistenceController.shared
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    let bluetoothManager = BluetoothManager(storage: StorageService())
    //    init() {
//        OrientationHelper.forcePortrait()
//    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(\.managedObjectContext, persistenceController.container.viewContext)
                .environmentObject(bluetoothManager)
        }
    }
}
