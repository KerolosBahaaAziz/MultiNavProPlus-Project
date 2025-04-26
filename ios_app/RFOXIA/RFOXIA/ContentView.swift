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
        //                BluetoothChatView()
        // Pass the managed object context to BluetoothChatView
        //        ActionsAndDelaysView()
        //        TaskView()
//                 JoyStickView()
//        GoogleSignInView()
//        ApplePayView()
//        BluetoothChatView()
//            .environment(\.managedObjectContext, viewContext)
//        CheckBluetoothView()
    
//        MainTabView()
//            .environment(\.managedObjectContext, viewContext)
        JoyStickView()
            .environment(\.managedObjectContext, viewContext)
    }
}

#Preview {
    ContentView()
        .environment(\.managedObjectContext, PersistenceController.preview.container.viewContext)
        .environmentObject(BluetoothManager())
}
