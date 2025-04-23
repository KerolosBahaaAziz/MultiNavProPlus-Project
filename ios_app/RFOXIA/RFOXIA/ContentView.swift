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
//    let applePayHandler = ApplePayHandler()
    
    var body: some View {
        //                BluetoothChatView()
        // Pass the managed object context to BluetoothChatView
        //        ActionsAndDelaysView()
        //        TaskView()
//                JoyStickView()
        ApplePayView()
//        GoogleSignInView()
    }
}

#Preview {
    ContentView()
        .environment(\.managedObjectContext, PersistenceController.preview.container.viewContext)
}
