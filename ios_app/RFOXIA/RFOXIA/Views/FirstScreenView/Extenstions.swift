//
//  Extenstions.swift
//  RFOXIA
//
//  Created by Kerlos on 24/04/2025.
//

import Foundation

extension BluetoothChatView{
    
    func handleMicTapped() {
        let isSubscribed = UserDefaults.standard.bool(forKey: "isSubscribed")
        let timeStamp = UserDefaults.standard.double(forKey: "subscriptionExpireDate")
        let expiryDate = Date(timeIntervalSince1970: timeStamp)
        let isExpired = expiryDate < Date()
        
        if isSubscribed && !isExpired {
            if isRecording {
                recorder.stopRecording()
            } else {
                recorder.startRecording()
            }
            isRecording.toggle()
        } else {
            alertItem = AlertInfo(
                title: "Notice",
                message: isSubscribed ? "Your subscription expired. Subscribe now for $1/month." : "To use voice, subscribe for $1/month.",
                confirmText: "Subscribe",
                cancelText: "Cancel",
                confirmAction: {
                    navigateToSubscribe = true
                }
            )
            print("is navigate: \(navigateToSubscribe)")
        }
    }
    
    func startRecording() {
        // Your actual mic recording logic
        print("Start recording")
        isRecording = true
    }
}
