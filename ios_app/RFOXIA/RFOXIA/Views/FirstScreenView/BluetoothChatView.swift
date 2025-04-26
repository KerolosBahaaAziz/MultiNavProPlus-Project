//
//  ChattingView.swift
//  RFOXIA
//
//  Created by Kerlos on 11/04/2025.
//

import SwiftUI

// MARK: - Message Model

enum MessageType {
    case text(String)
    case voice(URL)
}

struct ChatMessage: Identifiable {
    let id = UUID()
    let type: MessageType
    let isCurrentUser: Bool
    let senderName: String
    let createdAt: Date = Date()
}


// MARK: - Chat View

struct BluetoothChatView: View {
    
    @State var inputText = ""
    @State var messages: [ChatMessage] = []
    @State var isRecording = false
    @StateObject var recorder = AudioRecorder()

    
    @State var navigateToSubscribe = false
    @State var alertItem: AlertInfo?
    
    
    @StateObject private var bluetoothManager = BluetoothManager()  // Bluetooth manager
    
    
    let customColor = Color(red: 26/255, green: 61/255, blue: 120/255)
    
    var body: some View {
        NavigationStack{
            VStack(spacing: 0) {
                ChatMessagesView(messages: messages, customColor: customColor)
                    .background(BackgroundGradient.backgroundGradient)
                
                // Input bar
                HStack {
                    TextField("Type a message...", text: $inputText)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                        .frame(minHeight: 36)
                        .foregroundColor(customColor) // Text color
                    
                    Button(action: {
                        if !inputText.isEmpty {
                            bluetoothManager.sendMessage(inputText)
                            let newMessage = ChatMessage(type: .text(inputText), isCurrentUser: true, senderName: "Me")
                            messages.append(newMessage)
                            inputText = ""
                        }
                    }) {
                        Image(systemName: "paperplane.fill")
                            .foregroundColor(customColor)
                            .padding(8)
                    }
                    
                    // Add voice recording functionality later
                    Button(action: handleMicTapped) {
                        Image(systemName: isRecording ? "stop.fill" : "mic.fill")
                            .foregroundColor(isRecording ? .red : customColor)
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
                    for message in newMessages {
                        let newMessage = ChatMessage(type: .text(message), isCurrentUser: false, senderName: bluetoothManager.connectedDeviceName)
                        messages.append(newMessage)
                    }
                }.onDisappear{
                    bluetoothManager.disableNotify(for: [bluetoothManager.chatCharacteristicUUID])
                }
                .onChange(of: recorder.recordings) { newRecordings in
                    if let last = newRecordings.last {
                        let newMessage = ChatMessage(type: .voice(last.url), isCurrentUser: true, senderName: "Me")
                        messages.append(newMessage)
                    }
                    
                }
        }
    }
}


// MARK: - Preview

#Preview {
    BluetoothChatView()
}
