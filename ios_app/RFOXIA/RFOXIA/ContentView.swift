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
    @Environment(\.scenePhase) var scenePhase

    
    var body: some View {
        // Pass the managed object context to BluetoothChatView
        JoyStickView()
            .environment(\.managedObjectContext, viewContext)
            .onAppear(){
                OrientationHelper.forceLandscape()
            }
            .onChange(of: scenePhase){ oldPhase, newPhase in
                if newPhase == .active{
                    OrientationHelper.forceLandscape()
                }
            }
            .onDisappear(){
                OrientationHelper.forcePortrait()
            }
    }
}

#Preview {
    ContentView()
        .environment(\.managedObjectContext, PersistenceController.preview.container.viewContext)
}
