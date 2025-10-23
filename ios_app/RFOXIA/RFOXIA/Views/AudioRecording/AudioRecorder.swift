//
//  AudioRecorder.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 26/04/2025.
//

import Foundation
import AVFoundation

class AudioRecorder: NSObject, ObservableObject {
    @Published var recordings: [Recording] = []
    @Published var isRecording = false
    @Published var recordingTime: TimeInterval = 0
    
    var audioRecorder: AVAudioRecorder?
    var timer: Timer?
    
    let recordingSession = AVAudioSession.sharedInstance()
    
    struct Recording: Identifiable ,Equatable{
        let id = UUID()
        let url: URL
        let createdAt: Date
        var name: String {
            url.lastPathComponent
        }
    }
    
    func startRecording() {
        do {
            try recordingSession.setCategory(.playAndRecord, mode: .default)
            try recordingSession.setActive(true)

            let url = getNewRecordingURL()
            let settings = [
                AVFormatIDKey: Int(kAudioFormatMPEG4AAC),
                AVSampleRateKey: 12000,
                AVNumberOfChannelsKey: 1,
                AVEncoderAudioQualityKey: AVAudioQuality.high.rawValue
            ]

            audioRecorder = try AVAudioRecorder(url: url, settings: settings)
            audioRecorder?.delegate = self
            audioRecorder?.record()
            
            isRecording = true
            recordingTime = 0
            startTimer()
        } catch {
            print("Failed to start recording: \(error.localizedDescription)")
            isRecording = false
        }
    }
    
    func stopRecording() {
        audioRecorder?.stop()
        stopTimer()
        isRecording = false
    }
    
    func deleteRecording(at indexSet: IndexSet) {
        indexSet.forEach { index in
            let recording = recordings[index]
            do {
                try FileManager.default.removeItem(at: recording.url)
                recordings.remove(at: index)
            } catch {
                print("Error deleting recording: \(error.localizedDescription)")
            }
        }
    }
    
    private func getNewRecordingURL() -> URL {
        let filename = "Recording_\(Date().formatted(.dateTime.day().month().year().hour().minute())).m4a"
        let paths = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)
        return paths[0].appendingPathComponent(filename)
    }
    
    private func startTimer() {
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { _ in
            self.recordingTime += 1
        }
    }
    
    private func stopTimer() {
        timer?.invalidate()
        timer = nil
    }
}

extension AudioRecorder: AVAudioRecorderDelegate {
    func audioRecorderDidFinishRecording(_ recorder: AVAudioRecorder, successfully flag: Bool) {
        if flag {
            let newRecording = Recording(url: recorder.url, createdAt: Date())
            recordings.append(newRecording)
        } else {
            print("Recording failed")
        }
    }
}

class AudioPlayerDelegate: NSObject, AVAudioPlayerDelegate {
    var onFinish: () -> Void
    
    init(onFinish: @escaping () -> Void) {
        self.onFinish = onFinish
    }
    
    func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        onFinish()
    }
}
