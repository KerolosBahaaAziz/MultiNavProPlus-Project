//
//  DeviceListView.swift
//  RFOXIA
//
//  Created by Kerlos on 24/04/2025.
//

import SwiftUI

struct DeviceListView: View {
    @EnvironmentObject var bluetoothManager: BluetoothManager  // Use the environment object here

    var body: some View {
        NavigationStack {
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

                // Hidden navigation trigger
                NavigationLink(destination: DevicesNamesView(), isActive: $bluetoothManager.isConnected) {
                    EmptyView()
                }
                .hidden()
            }
            .navigationTitle("Select a Device")
            .onAppear {
                bluetoothManager.scanForDevices()
            }.alert("Connection Failed", isPresented: $bluetoothManager.showConnectionError) {
                Button("OK", role: .cancel) { }
            } message: {
                Text(bluetoothManager.connectionErrorMessage)
            }
        }.navigationBarBackButtonHidden(true)
    }
}

#Preview {
    DeviceListView()
}
