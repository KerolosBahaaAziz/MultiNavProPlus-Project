//
//  ChatMessagesView.swift
//  RFOXIA
//
//  Created by Kerlos on 23/04/2025.
//

import SwiftUI

struct ChatMessagesView: View {
    let messages: [ChatMessage]
    let customColor: Color

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 8) {
                ForEach(messages) { message in
                    HStack(alignment: .top) {
                        if message.isCurrentUser { Spacer() }
                        
                        VStack(alignment: message.isCurrentUser ? .trailing : .leading, spacing: 4) {
                            if case .text(let text) = message.type {
                                Text(text)
                                    .padding()
                                    .background(message.isCurrentUser ? customColor : Color.white.opacity(0.8))
                                    .foregroundColor(message.isCurrentUser ? .white : .black)
                                    .cornerRadius(16)
                            } else if case .voice(let url) = message.type {
                                VoiceMessageBubble(recording: Recordingg(url: url, createdAt: message.createdAt))
                            }
                        }
                        .frame(maxWidth: 250, alignment: message.isCurrentUser ? .trailing : .leading)
                        
                        if !message.isCurrentUser { Spacer() }
                    }
                    .padding(.horizontal)
                }
            }
        }
    }
}


#Preview {
    ChatMessagesView(messages: [], customColor: Color.accentColor)
}
