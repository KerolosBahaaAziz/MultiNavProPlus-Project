//
//  RecordingButton.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 26/04/2025.
//

import SwiftUI

struct RecordingButton: View {
    @ObservedObject var recorder: AudioRecorder
    
    var body: some View {
        Circle()
            .fill(recorder.isRecording ? Color.red : Color.blue)
            .frame(width: 80, height: 80)
            .overlay(
                Image(systemName: recorder.isRecording ? "mic.fill" : "mic")
                    .foregroundColor(.white)
                    .font(.system(size: 30))
            )
            .simultaneousGesture(
                LongPressGesture(minimumDuration: 0.1)
                    .onEnded { _ in
                        if recorder.isRecording {
                            recorder.stopRecording()
                        } else {
                            recorder.startRecording()
                        }
                    }
            )
    }
}

#Preview {
    RecordingButton(recorder: AudioRecorder())
}
