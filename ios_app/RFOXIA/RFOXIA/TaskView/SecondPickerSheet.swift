//
//  SecondPickerSheet.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 15/04/2025.
//

import SwiftUI

struct SecondPickerSheet: View {
    var onDelaySelected: ((String) -> Void)?
    @Environment(\.dismiss) private var dismiss
    
    @State private var seconds = 0
    @State private var milliseconds = 0
    @State private var showAlert = false
    @State private var alertMessage = ""
    
    private let secondsRange = Array(0...60)
    private let millisecondsRange = Array(0...99)
    
    private var delayValue: String {
        String(format: "%.2f", Double(seconds) + Double(milliseconds)/100)
    }
    
    var body: some View {
        NavigationStack {
            VStack {
                Text("Set Delay")
                    .font(.title)
                    .padding()
                
                HStack(spacing: 20) {
                    // Seconds Picker
                    VStack {
                        Text("Seconds")
                            .font(.headline)
                        Picker("", selection: $seconds) {
                            ForEach(secondsRange, id: \.self) { value in
                                Text("\(value)").tag(value)
                            }
                        }
                        .pickerStyle(.wheel)
                        .frame(width: 120)
                    }
                    
                    // Milliseconds Picker
                    VStack {
                        Text("Milliseconds")
                            .font(.headline)
                        Picker("", selection: $milliseconds) {
                            ForEach(millisecondsRange, id: \.self) { value in
                                Text(String(format: "%02d", value)).tag(value)
                            }
                        }
                        .pickerStyle(.wheel)
                        .frame(width: 120)
                    }
                }
                .frame(height: 200)
                
                Text("Selected delay: \(delayValue) seconds")
                    .font(.headline)
                    .padding()
                
                Spacer()
            }
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") {
                        validateAndAddDelay()
                    }
                }
            }
            .alert("Invalid Delay", isPresented: $showAlert) {
                Button("OK", role: .cancel) {}
            } message: {
                Text(alertMessage)
            }
            .onAppear {
                OrientationHelper.forcePortrait()
            }
        }
    }
    
    private func validateAndAddDelay() {
        let totalSeconds = Double(seconds) + Double(milliseconds)/100
        
        guard totalSeconds >= 0.1 else {
            alertMessage = "Delay must be at least 0.1 seconds"
            showAlert = true
            return
        }
        
        guard totalSeconds <= 60.99 else {
            alertMessage = "Delay cannot exceed 60.99 seconds"
            showAlert = true
            return
        }
        
        onDelaySelected?(delayValue)
        dismiss()
    }
}

#Preview {
    SecondPickerSheet()
}
