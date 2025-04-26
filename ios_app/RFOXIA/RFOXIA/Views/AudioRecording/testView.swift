//
//  testView.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 26/04/2025.
//

import Foundation
import SwiftUI

struct testView: View {
    @StateObject private var recorder = AudioRecorder()

    var body: some View {
        VStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 12) {
                    ForEach(recorder.recordings) { recording in
                        VoiceMessageBubble(recording: recording)
                            .padding(.horizontal)
                    }
                }
            }
            
            Spacer()
            
            RecordingButton(recorder: recorder)
                .padding()
        }
    }
}

#Preview {
    testView()
}
