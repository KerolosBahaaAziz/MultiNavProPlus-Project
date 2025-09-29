//
//  DevicesNamesView.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 10/05/2025.
//

import SwiftUI

struct DevicesNamesView: View {
    @EnvironmentObject var bluetoothManager: BluetoothManager
    
    @State private var isDiscovering = false
    @State private var discoveryTimer: Timer?
    
    var body: some View {
        VStack(spacing: 20) {
            if !bluetoothManager.isConnected {
                Text("Not connected to any device")
                    .foregroundColor(.red)
                    .padding()
            } else if bluetoothManager.nearByDevicesName.isEmpty {
                if isDiscovering {
                    ProgressView("Discovering devices...")
                        .padding()
                } else {
                    Text("No devices found yet.")
                        .padding()
                }
            } else {
                List(bluetoothManager.nearByDevicesName.indices, id: \.self) { index in
                    NavigationLink(
                        destination: BluetoothChatView()
                    ) {
                        Text(bluetoothManager.nearByDevicesName[index])
                            .padding()
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(Color.blue.opacity(0.1))
                            .cornerRadius(8)
                    }
                    .simultaneousGesture(TapGesture().onEnded {
                        bluetoothManager.discoverOrSelectNearbyDevices(index)
                    })
                }
            }
        }
        .navigationTitle("Nearby Devices")
        .onAppear {
            // Enable notifications for device name updates
            bluetoothManager.enableNotify(for: [
                bluetoothManager.deviceNameCharacteristicUUID,
                bluetoothManager.sendToDiscoverDevicesCharacteristicUUID
            ])
            // Trigger initial device discovery after 1 second
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                self.startDiscovery()
            }
            // Set up timer to refresh every 10 seconds
            discoveryTimer = Timer.scheduledTimer(withTimeInterval: 40.0, repeats: true) { _ in
                self.startDiscovery()
            }
        }
        .onDisappear {
            // Disable notifications when leaving the view
            bluetoothManager.disableNotify(for: [
                bluetoothManager.deviceNameCharacteristicUUID,
                bluetoothManager.sendToDiscoverDevicesCharacteristicUUID
            ])
            // Invalidate the timer to prevent memory leaks
            discoveryTimer?.invalidate()
            discoveryTimer = nil
        }
    }
    
    private func startDiscovery() {
        guard bluetoothManager.isConnected else {
            print("Cannot start discovery: No device connected")
            return
        }
        DispatchQueue.main.async {
            self.isDiscovering = true
            // Trigger device discovery
            bluetoothManager.discoverOrSelectNearbyDevices()
            // Reset isDiscovering after a short delay (adjust as needed)
            DispatchQueue.main.asyncAfter(deadline: .now() + 10.0) {
                self.isDiscovering = false
            }
        }
    }
}

#Preview {
    DevicesNamesView()
}
