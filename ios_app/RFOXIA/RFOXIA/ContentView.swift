//
//  ContentView.swift
//  RFOXIA
//
//  Created by Kerlos on 11/04/2025.
//

import SwiftUI
import CoreData

struct ContentView: View {
    @Environment(\.managedObjectContext) private var viewContext
    @EnvironmentObject var bluetoothManager: BluetoothManager
    
    //    let applePayHandler = ApplePayHandler()
    
    var body: some View {

        //        JoyStickView()
        //         .environment(\.managedObjectContext, viewContext)
        //                        BluetoothChatView()
        // Pass the managed object context to BluetoothChatView
        //        ActionsAndDelaysView()
        //        TaskView()
        //                 JoyStickView()
        //        GoogleSignInView()
        //        ApplePayView()
        //        CheckBluetoothView()
        //
        //        MainTabView()
        //            .environment(\.managedObjectContext, viewContext)
        //        BluetoothChatView()
        //        DevicesNamesView()
        //            .environment(\.managedObjectContext, viewContext)
        //            .environmentObject(bluetoothManager)
        MainTabView()
            .environment(\.managedObjectContext, viewContext)
            .environmentObject(bluetoothManager)
    }
}

#Preview {
    ContentView()
        .environment(\.managedObjectContext, PersistenceController.preview.container.viewContext)
        .environmentObject(BluetoothManager())
}
