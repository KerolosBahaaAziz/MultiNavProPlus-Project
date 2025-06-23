//
//  ChattingView.swift
//  RFOXIA
//
//  Created by Kerlos on 11/04/2025.
//

import SwiftUI

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
    
    let otherUser = UserDefaults.standard.string(forKey: "otherEmail") ?? ""
    
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
                            let newMessage = ChatMessage(type: .text(inputText), isCurrentUser: true, senderName: "Me", senderId: UserDefaults.standard.string(forKey: "userEmail") ?? "", createdAt: Date(), text: inputText)
                            messages.append(newMessage)
                            saveTextMessage(forEmail: otherUser, text: inputText, isMine: true, type: "text")
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
                
                NavigationLink(destination: ChoosePaymentMethodView(), isActive: $navigateToSubscribe) {
                    EmptyView()
                }
            }.background(BackgroundGradient.backgroundGradient)
                .onAppear {
                    // Start scanning for Bluetooth devices when the view appears
                    bluetoothManager.scanForDevices()
                    bluetoothManager.enableNotify(for: [bluetoothManager.chatCharacteristicUUID])
                }
                .onChange(of: bluetoothManager.receivedMessages, perform: handleReceivedMessages).onDisappear{
                    bluetoothManager.disableNotify(for: [bluetoothManager.chatCharacteristicUUID])
                }
                .onChange(of: bluetoothManager.receivedAudioData, perform: handleReceivedAudioData)
            
        }.navigationBarBackButtonHidden(true)
            .onReceive(recorder.$recordings) { newRecordings in
                handleNewRecordings(newRecordings)
            }
    }
    
    func saveReceivedAudio(data: Data) -> URL? {
        let filename = "ReceivedVoice_\(UUID().uuidString).m4a"
        let fileURL = FileManager.default.temporaryDirectory.appendingPathComponent(filename)
        
        do {
            try data.write(to: fileURL)
            return fileURL
        } catch {
            print("Error saving received audio: \(error)")
            return nil
        }
    }
    
    private func handleReceivedMessages(_ newMessages: [String]) {
        for message in newMessages {
            let newMessage = ChatMessage(type: .text(message), isCurrentUser: false, senderName: bluetoothManager.connectedDeviceName, senderId: UserDefaults.standard.string(forKey: "userEmail") ?? "", createdAt: Date(), text: message)
            messages.append(newMessage)
            saveTextMessage(forEmail: otherUser, text: message, isMine: false, type: "text")
        }
    }
    
    private func handleReceivedAudioData(_ audioDataArray: [Data]) {
        for audioData in audioDataArray {
            if let savedURL = saveReceivedAudio(data: audioData) {
                let newMessage = ChatMessage(type: .voice(savedURL), isCurrentUser: false, senderName: bluetoothManager.connectedDeviceName, senderId: UserDefaults.standard.string(forKey: "userEmail") ?? "", createdAt: Date(), text: "")
                messages.append(newMessage)
            }
        }
    }
    
    private func handleNewRecordings(_ newRecordings: [Recordingg]) {
        if let last = newRecordings.last {
            let newMessage = ChatMessage(type: .voice(last.url), isCurrentUser: true, senderName: "Me", senderId: UserDefaults.standard.string(forKey: "userEmail") ?? "", createdAt: Date(), text: "")
            messages.append(newMessage)
        }
    }
    
    func saveTextMessage(forEmail email: String, text: String, isMine: Bool, type: String){
        SaveMessgaePresenter.shared.saveMessageCoreData(forEmail: email, text: text, isMine: isMine, type: type, record: nil)
    }
    
}


// MARK: - Preview

#Preview {
    BluetoothChatView()
}
