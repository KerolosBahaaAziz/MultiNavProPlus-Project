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

    @State private var hours = 0
    @State private var minutes = 0
    @State private var seconds = 0
    @State private var milliseconds = 0
    @State private var showAlert = false
    @State private var alertMessage = ""

    private let hoursRange = Array(0...23)
    private let minutesRange = Array(0...59)
    private let secondsRange = Array(0...59)
    private let millisecondsRange = Array(0...99)

    private var delayValue: String {
        let totalSeconds = Double(hours) * 3600 + Double(minutes) * 60 + Double(seconds) + Double(milliseconds) / 100
        return String(format: "%.2f", totalSeconds)
    }

    var body: some View {
        NavigationStack {
            VStack {
                Text("Set Delay")
                    .font(.title)
                    .padding()

                HStack(spacing: 20) {
                    // Hours Picker
                    VStack {
                        Text("Hours")
                            .font(.headline)
                        Picker("", selection: $hours) {
                            ForEach(hoursRange, id: \.self) { value in
                                Text("\(value)").tag(value)
                            }
                        }
                        .pickerStyle(.wheel)
                        .frame(width: 80)
                    }

                    // Minutes Picker
                    VStack {
                        Text("Minutes")
                            .font(.headline)
                        Picker("", selection: $minutes) {
                            ForEach(minutesRange, id: \.self) { value in
                                Text("\(value)").tag(value)
                            }
                        }
                        .pickerStyle(.wheel)
                        .frame(width: 80)
                    }

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
                        .frame(width: 80)
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
                        .frame(width: 80)
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
        let totalSeconds = Double(hours) * 3600 + Double(minutes) * 60 + Double(seconds) + Double(milliseconds) / 100

        guard totalSeconds >= 0.1 else {
            alertMessage = "Delay must be at least 0.1 seconds"
            showAlert = true
            return
        }

        // You can set an upper limit if you want (optional)
        guard totalSeconds <= 86400 else { // 24 hours = 86400 seconds
            alertMessage = "Delay cannot exceed 24 hours"
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
