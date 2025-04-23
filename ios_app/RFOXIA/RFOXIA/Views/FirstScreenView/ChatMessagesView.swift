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
            LazyVStack(alignment: .leading, spacing: 12) {
                ForEach(messages) { message in
                    HStack(alignment: .top) {
                        if message.isCurrentUser {
                            Spacer()
                            VStack(alignment: .trailing, spacing: 4) {
                                Text(message.text)
                                    .padding()
                                    .background(customColor)
                                    .foregroundColor(.white)
                                    .cornerRadius(16)
                            }
                            .frame(maxWidth: 250, alignment: .trailing)
                        } else {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(message.senderName)
                                    .font(.caption)
                                    .foregroundColor(.white)
                                Text(message.text)
                                    .padding()
                                    .background(Color.white.opacity(0.8))
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
        }
    }
}


#Preview {
    ChatMessagesView(messages: [], customColor: Color.accentColor)
}
