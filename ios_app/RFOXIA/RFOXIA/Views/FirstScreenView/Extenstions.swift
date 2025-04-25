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

        if isSubscribed {
            startRecording()
        } else {
            alertItem = AlertInfo(
                title: "Notice",
                message: "To use voice, subscribe for $1/month.",
                confirmText: "Subscribe",
                cancelText: "Cancel",
                confirmAction: {
                navigateToSubscribe = true
                }
            )
            print("is anvigate : \(navigateToSubscribe)")
        }
    }
    
    func startRecording() {
        // Your actual mic recording logic
        print("Start recording")
        isRecording = true
    }
}
