//
//  AudioRecorder.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 26/04/2025.
//

import Foundation
import AVFoundation
import SwiftUICore

class AudioRecorder: NSObject, ObservableObject {
    @Published var recordings: [Recording] = []
    @Published var isRecording = false
    @Published var recordingTime: TimeInterval = 0
    private var bluetoothManager = BluetoothManager()  // Bluetooth manager

    
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
    
    func prepareAudioData(from fileURL: URL, completion: @escaping (Data?) -> Void) {
        let asset = AVAsset(url: fileURL)
        
        guard let exportSession = AVAssetExportSession(asset: asset, presetName: AVAssetExportPresetAppleM4A) else {
            print("Failed to create export session.")
            completion(nil)
            return
        }

        let compressedURL = FileManager.default.temporaryDirectory.appendingPathComponent("compressed_\(UUID().uuidString).m4a")

        try? FileManager.default.removeItem(at: compressedURL)
        
        exportSession.outputURL = compressedURL
        exportSession.outputFileType = .m4a
        exportSession.exportAsynchronously {
            switch exportSession.status {
            case .completed:
                do {
                    let data = try Data(contentsOf: compressedURL)
                    completion(data)
                } catch {
                    print("Failed to load compressed data: \(error)")
                    completion(nil)
                }
            case .failed, .cancelled:
                print("Export failed: \(exportSession.error?.localizedDescription ?? "Unknown error")")
                completion(nil)
            default:
                break
            }
        }
    }

    func prepareLastRecordingForBLE(completion: @escaping (Data?) -> Void) {
        guard let lastRecording = recordings.last else {
            print("No recordings available.")
            completion(nil)
            return
        }
        prepareAudioData(from: lastRecording.url, completion: completion)
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

}

extension AudioRecorder: AVAudioRecorderDelegate {
    func audioRecorderDidFinishRecording(_ recorder: AVAudioRecorder, successfully flag: Bool) {
        if flag {
            let newRecording = Recording(url: recorder.url, createdAt: Date())
            recordings.append(newRecording)
            
            prepareLastRecordingForBLE { data in
                        if let voiceData = data {
                            self.bluetoothManager.sendVoiceData(voiceData)
                        } else {
                            print("Failed to prepare audio data for BLE")
                        }
                    }
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
