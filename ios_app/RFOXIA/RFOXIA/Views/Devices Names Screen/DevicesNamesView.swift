////
////  DevicesNamesView.swift
////  RFOXIA
////
////  Created by Yasser Yasser on 10/05/2025.
////
//
//import SwiftUI
//
//struct DevicesNamesView: View {
//    
//    @EnvironmentObject var bluetoothManager: BluetoothManager
//    
//    var body: some View {
//            VStack(spacing: 20) {
//                Button("🔍 Request Devices (Send 'a')") {
//                    bluetoothManager.sendMessage("a")
//                }
//                .padding()
//                .background(Color.blue)
//                .foregroundColor(.white)
//                .cornerRadius(10)
//                
//                if bluetoothManager.availableDeviceNames.isEmpty {
//                    Text("No devices found yet.")
//                } else {
//                    List(bluetoothManager.availableDeviceNames.indices, id: \.self) { index in
//                        Button(action: {
//                            bluetoothManager.sendMessage("\(index)")
//                        }) {
//                            Text(bluetoothManager.availableDeviceNames[index])
//                        }
//                    }
//                }
//            }
//            .padding()
//        }
//}
//
//#Preview {
//    DevicesNamesView()
//}
