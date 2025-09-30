//
//  MainTabView.swift
//  RFOXIA
//
//  Created by Kerlos on 12/04/2025.
//

import SwiftUI

import SwiftUI

struct MainTabView: View {
    init() {
        let appearance = UITabBarAppearance()
           appearance.configureWithOpaqueBackground()
           appearance.backgroundColor = UIColor(Color.white)
           UITabBar.appearance().standardAppearance = appearance
           UITabBar.appearance().scrollEdgeAppearance = appearance
    }

    var body: some View {
        ZStack {
            BackgroundGradient.backgroundGradient
                .ignoresSafeArea()

            TabView {
                
                BluetoothChatView()
                    .tabItem {
                        Label("Chat", systemImage: "bubble.left.and.bubble.right.fill")
                    }

                JoyStickView()
                    .tabItem {
                        Label("Control", systemImage: "gear")
                    }
                
            }
            .accentColor(Color(red: 26/255, green: 61/255, blue: 120/255)) 
        }
        .navigationBarBackButtonHidden(true)
    }
}

#Preview {
    MainTabView()
        .environmentObject(BluetoothManager())
}
