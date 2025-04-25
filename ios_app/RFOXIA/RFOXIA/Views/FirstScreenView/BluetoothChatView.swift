//
//  ChattingView.swift
//  RFOXIA
//
//  Created by Kerlos on 11/04/2025.
//

import SwiftUI

// MARK: - Message Model

struct ChatMessage: Identifiable {
    let id = UUID()
    let text: String
    let isCurrentUser: Bool
    let senderName: String
}

// MARK: - Chat View

struct BluetoothChatView: View {
    @State private var inputText = ""
    @State var isRecording = false // For voice recording state
    @State var navigateToSubscribe = false
    @State var alertItem: AlertInfo?
    
    
    @StateObject private var bluetoothManager = BluetoothManager()  // Bluetooth manager
    
    @State private var messages: [ChatMessage] = []
    
    let customColor = Color(red: 26/255, green: 61/255, blue: 120/255)
    
    var body: some View {
         NavigationStack{
        return VStack(spacing: 0) {
                ChatMessagesView(messages: messages, customColor: customColor)
                    .background(BackgroundGradient.backgroundGradient)
                
                // Input bar
                HStack {
                    TextField("Type a message...", text: $inputText)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                        .frame(minHeight: 36)
                        .foregroundColor(customColor) // Text color
                    
                    Button(action: {
                        // Send the message via Bluetooth
                        bluetoothManager.sendMessage(inputText)
                        
                        // Update local UI with the sent message
                        let newMessage = ChatMessage(text: inputText, isCurrentUser: true, senderName: "Me")
                        messages.append(newMessage)
                        inputText = "" // Clear input after sending
                    }) {
                        Image(systemName: "paperplane.fill")
                            .foregroundColor(customColor) // Paper plane color
                            .padding(8)
                    }
                    // Add voice recording functionality later
                    Button(action: handleMicTapped) {
                        Image(systemName: isRecording ? "mic.fill" : "mic")
                            .foregroundColor(isRecording ? .red : customColor) // Microphone button color
                            .padding(8)
                    }.alert(info: $alertItem)
                    
                    // Phone call button (UI only)
                    Button(action: {
                        // Add phone call functionality later
                    }) {
                        Image(systemName: "phone.fill")
                            .foregroundColor(customColor) // Phone icon color
                            .padding(8)
                    }
                }
                .padding()
                .background(Color.white) // Background for the input bar
                .cornerRadius(8)
                .padding(.bottom, 0)
                .padding(.leading, 0)
                
                NavigationLink(destination: ApplePayView(), isActive: $navigateToSubscribe) {
                        EmptyView()
                }
            }.background(BackgroundGradient.backgroundGradient)
                .onAppear {
                    // Start scanning for Bluetooth devices when the view appears
                    bluetoothManager.scanForDevices()
                    bluetoothManager.enableNotify(for: [bluetoothManager.chatCharacteristicUUID])
                }
                .onChange(of: bluetoothManager.receivedMessages) { newMessages in
                    // When a new message is received via Bluetooth, add it to the UI
                    for message in newMessages {
                        let newMessage = ChatMessage(text: message, isCurrentUser: false, senderName: bluetoothManager.connectedDeviceName)
                        messages.append(newMessage)
                    }
                }.onDisappear{
                    bluetoothManager.disableNotify(for: [bluetoothManager.chatCharacteristicUUID])
                }
        }
    }
}

// MARK: - Preview

#Preview {
    BluetoothChatView()
}
