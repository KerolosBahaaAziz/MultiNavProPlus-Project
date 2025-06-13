//
//  RFOXIAApp.swift
//  RFOXIA
//
//  Created by Kerlos on 11/04/2025.
//

import SwiftUI
import PayPalCheckout

@main
struct RFOXIAApp: App {
    let persistenceController = PersistenceController.shared
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    let bluetoothManager = BluetoothManager(storage: StorageService())
    //    init() {
//        OrientationHelper.forcePortrait()
//    }
    init() {
        let config = CheckoutConfig(
            clientID: "ARtK7n9fg11xIBa4OzHymtUQb037NbHVkmodgQj3E8JvgN8miupe8oI2EgJeixqTv2qGVlDElLJDinRX", // Replace with your real client ID
            returnUrl: "iti.RFOXIA://paypalpay", // 👈 Based on your bundle ID
            environment: .sandbox // Use `.live` in production
        )
        
        Checkout.set(config: config)
        Checkout.start()
    }
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(\.managedObjectContext, persistenceController.container.viewContext)
                .environmentObject(bluetoothManager)
        }
    }
}
