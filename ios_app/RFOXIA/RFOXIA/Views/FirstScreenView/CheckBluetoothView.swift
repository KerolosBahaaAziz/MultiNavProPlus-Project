//
//  CheckBluetoothView.swift
//  RFOXIA
//
//  Created by Kerlos on 12/04/2025.
//

import SwiftUI

struct CheckBluetoothView: View {
    @State private var inputText = ""
    @State private var isRecording = false
    @StateObject private var bluetoothManager = BluetoothManager()
    
    @State private var messages: [ChatMessage] = [
        ChatMessage(type: .text("Hey! Ready to test Bluetooth chat?"), isCurrentUser: false, senderName: "John"),
        ChatMessage(type: .text("Yep! Looks like we're connected."), isCurrentUser: true, senderName: "Me"),
        ChatMessage(type: .text("Awesome. No internet needed"), isCurrentUser: false, senderName: "John"),
        ChatMessage(type: .text("Exactly — just Swift and airwaves"), isCurrentUser: true, senderName: "Me")
    ]

    let customColor = Color(red: 26/255, green: 61/255, blue: 120/255)
    
    var body: some View {
        VStack(spacing: 0) {
            // Banner
            HStack {
                Image(systemName: "dot.radiowaves.left.and.right")
                    .foregroundColor(bluetoothManager.isBluetoothOn ? .green : .red)
                Text(bluetoothManager.isBluetoothOn ? "Bluetooth ON" : "Bluetooth OFF")
                    .font(.caption)
                    .foregroundColor(bluetoothManager.isBluetoothOn ? .green : .red)
                Spacer()
            }
            .padding()
            .background(Color.white)
            
            Divider()
            
            // Conditional View
            if bluetoothManager.isBluetoothOn {
                BluetoothChatView()
            } else {
                DisconnectedBluetoothView()
            }
        }
        .background(BackgroundGradient.backgroundGradient)
        .ignoresSafeArea()
        .onAppear {
            bluetoothManager.scanForDevices()
        }
        .onChange(of: bluetoothManager.receivedMessages) { newMessages in
            for message in newMessages {
                let newMessage = ChatMessage(type: .text(message), isCurrentUser: false, senderName: bluetoothManager.connectedDeviceName)
                messages.append(newMessage)
            }
        }
    }
}

#Preview {
    CheckBluetoothView()
}
