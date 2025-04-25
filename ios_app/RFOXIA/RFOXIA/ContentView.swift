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
        MainTabView()
            .environment(\.managedObjectContext, viewContext)
    }
}

#Preview {
    ContentView()
        .environment(\.managedObjectContext, PersistenceController.preview.container.viewContext)
}
