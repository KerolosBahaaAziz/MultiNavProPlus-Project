//
//  DeviceListView.swift
//  RFOXIA
//
//  Created by Kerlos on 24/04/2025.
//

import SwiftUI

import SwiftUI

struct DeviceListView: View {
    @EnvironmentObject var bluetoothManager: BluetoothManager
    @State private var navigateToMain = false   // State to trigger navigation
    
    var body: some View {
        VStack {
            List {
                ForEach(bluetoothManager.discoveredPeripherals, id: \.0.identifier) { (peripheral, name) in
                    Button(action: {
                        bluetoothManager.connectToPeripheral(peripheral)
                    }) {
                        VStack(alignment: .leading) {
                            Text(name)
                            Text(peripheral.identifier.uuidString)
                                .font(.caption)
                                .foregroundColor(.gray)
                        }
                    }
                }
            }
            
            // Skip button
            Button("Skip") {
                navigateToMain = true
            }
            .padding()
            
            // Navigation links
            NavigationLink(destination: MainTabView(), isActive: $bluetoothManager.isConnected) {
                EmptyView()
            }
            .hidden()
            
            NavigationLink(destination: MainTabView(), isActive: $navigateToMain) {
                EmptyView()
            }
            .hidden()
        }
        .navigationTitle("Select a Device")
        .onAppear {
            bluetoothManager.scanForDevices()
        }
        .alert("Connection Failed", isPresented: $bluetoothManager.showConnectionError) {
            Button("OK", role: .cancel) { }
        } message: {
            Text(bluetoothManager.connectionErrorMessage)
        }
        .navigationBarBackButtonHidden(true)
    }
}


#Preview {
    DeviceListView()
}
