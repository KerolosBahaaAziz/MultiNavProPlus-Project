//
//  DeviceListView.swift
//  RFOXIA
//
//  Created by Kerlos on 24/04/2025.
//

import SwiftUI

struct DeviceListView: View {
    @ObservedObject var bluetoothManager = BluetoothManager()

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
                NavigationLink(destination: JoyStickView(), isActive: $bluetoothManager.isConnected) {
                    EmptyView()
                }
                .hidden()
            }
            .navigationTitle("Select a Device")
            .onAppear {
                bluetoothManager.scanForDevices()
            }
        }
    }
}


#Preview {
    DeviceListView()
}
