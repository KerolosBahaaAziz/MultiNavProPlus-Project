//
//  VoiceMessageBubble.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 26/04/2025.
//

import SwiftUI
import AVFoundation

struct VoiceMessageBubble: View {
    let recording: Recordingg
    
    @State private var audioPlayer: AVAudioPlayer?
    @State private var isPlaying = false
    @State private var progress: Double = 0
    @State private var timer: Timer?
    
    var body: some View {
        HStack {
            Button(action: togglePlayback) {
                Image(systemName: isPlaying ? "pause.fill" : "play.fill")
                    .foregroundColor(.white)
                    .padding()
                    .background(Color.blue)
                    .clipShape(Circle())
            }
            
            VStack(alignment: .leading) {
                Text(recording.name)
                    .font(.caption)
                    .foregroundColor(.black)
                
                Text(recording.createdAt.formatted())
                    .font(.caption2)
                    .foregroundColor(.gray)
                
                if isPlaying {
                    ProgressView(value: progress, total: 1.0)
                        .progressViewStyle(LinearProgressViewStyle())
                }
            }
            .padding(8)
            .background(Color.gray.opacity(0.2))
            .cornerRadius(10)
        }
        .onDisappear {
            stopPlayback()
        }
    }
    
    private func togglePlayback() {
        if isPlaying {
            pausePlayback()
        } else {
            startPlayback()
        }
    }
    
    private func startPlayback() {
        if audioPlayer == nil {
            do {
                audioPlayer = try AVAudioPlayer(contentsOf: recording.url)
                audioPlayer?.delegate = AudioPlayerDelegate {
                    stopPlayback()
                }
            } catch {
                print("Failed to initialize player: \(error)")
                return
            }
        }
        
        audioPlayer?.play()
        isPlaying = true
        startProgressTimer()
    }
    
    private func pausePlayback() {
        audioPlayer?.pause()
        isPlaying = false
        stopProgressTimer()
    }
    
    private func stopPlayback() {
        audioPlayer?.stop()
        audioPlayer = nil
        isPlaying = false
        progress = 0
        stopProgressTimer()
    }
    
    private func startProgressTimer() {
        stopProgressTimer()
        timer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { _ in
            if let duration = audioPlayer?.duration, duration > 0 {
                progress = (audioPlayer?.currentTime ?? 0) / duration
            }
        }
    }
    
    private func stopProgressTimer() {
        timer?.invalidate()
        timer = nil
    }
}


#Preview {
    let dummyURL = URL(fileURLWithPath: "dummy.m4a")
    let dummyRecording = Recordingg(url: dummyURL, createdAt: Date())
    VoiceMessageBubble(recording: dummyRecording)
}

