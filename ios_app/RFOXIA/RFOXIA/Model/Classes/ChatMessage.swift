//
//  ChatMessage.swift
//  RFOXIA
//
//  Created by Kerlos on 17/05/2025.
//

import Foundation

struct ChatMessage: Identifiable {
    let id = UUID()
    let type: MessageType
    let isCurrentUser: Bool    //( = is mine?)
    let senderName: String
    let senderId: String
    let createdAt: Date = Date()
    let text: String
}

enum MessageType {
    case text(String)
    case voice(URL)
}

