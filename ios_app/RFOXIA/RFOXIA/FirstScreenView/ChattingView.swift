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
    @State private var isConnected = true // Simulated Bluetooth connection
    @State private var peerName = "John's iPhone"
    @State private var isRecording = false // For voice recording state

    let messages: [ChatMessage] = [
        ChatMessage(text: "Hey! Ready to test Bluetooth chat?", isCurrentUser: false, senderName: "John"),
        ChatMessage(text: "Yep! Looks like we're connected.", isCurrentUser: true, senderName: "Me"),
        ChatMessage(text: "Awesome. No internet needed", isCurrentUser: false, senderName: "John"),
        ChatMessage(text: "Exactly — just Swift and airwaves", isCurrentUser: true, senderName: "Me")
    ]
    
    let customColor = Color(red: 26/255, green: 61/255, blue: 120/255) // RGB (26, 61, 120)

    var body: some View {
       
        return VStack(spacing: 0) {
            // Connection banner
            HStack {
                Image(systemName: "dot.radiowaves.left.and.right")
                    .foregroundColor(isConnected ? .green : .red)
                Text(isConnected ? "Connected to \(peerName)" : "Disconnected")
                    .font(.caption)
                    .foregroundColor(isConnected ? .green : .red)
                Spacer()
            }
            .padding()
            .background(Color.white) // Gradient background for the banner
            .edgesIgnoringSafeArea(.bottom)

            Divider()

            // Chat messages
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 12) {
                    ForEach(messages) { message in
                        HStack(alignment: .top) {
                            if message.isCurrentUser {
                                Spacer()
                                VStack(alignment: .trailing, spacing: 4) {
                                    Text(message.text)
                                        .padding()
                                        .background(customColor) // Solid background for current user messages
                                        .foregroundColor(.white)
                                        .cornerRadius(16)
                                }
                                .frame(maxWidth: 250, alignment: .trailing)
                            } else {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(message.senderName)
                                        .font(.caption)
                                        .foregroundColor(Color.white) // Sender name color
                                    Text(message.text)
                                        .padding()
                                        .background(Color.white.opacity(0.8)) // Light background for received messages
                                        .foregroundColor(.black)
                                        .cornerRadius(16)
                                }
                                .frame(maxWidth: 250, alignment: .leading)
                                Spacer()
                            }
                        }
                        .padding(.horizontal)
                    }
                }
                .padding(.top)
            }.background(BackgroundGradient.backgroundGradient)

            Divider()

            // Input bar
            HStack {
                TextField("Type a message...", text: $inputText)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .frame(minHeight: 36)
                    .foregroundColor(customColor) // Text color

                Button(action: {
                    // No sending — static view
                    inputText = ""
                }) {
                    Image(systemName: "paperplane.fill")
                        .foregroundColor(customColor) // Paper plane color
                        .padding(8)
                }
                
                // Voice recording button (UI only)
                Button(action: {
                    // Add voice recording functionality later
                }) {
                    Image(systemName: isRecording ? "mic.fill" : "mic")
                        .foregroundColor(isRecording ? .red : customColor) // Microphone button color
                        .padding(8)
                }

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
            //.edgesIgnoringSafeArea(.to)
            .padding(.bottom, 5)
            .padding(.leading,1)
            //.padding(.trailing,)
        }
        //.background(backgroundGradient) // Apply gradient to the entire background of the view
        .edgesIgnoringSafeArea(.leading) // Ensure the gradient extends across the entire screen
        .edgesIgnoringSafeArea(.bottom)
    }
}

// MARK: - Preview

#Preview {
    BluetoothChatView()
}
