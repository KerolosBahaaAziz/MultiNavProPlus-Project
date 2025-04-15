//
//  ActionsAndDelaysView.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 15/04/2025.
//

import SwiftUI

struct ActionsAndDelaysView: View {
    
    @State private var taskName: String = ""
    @State private var showSecondPicker : Bool = false
    @State private var selectedMode : Int = 0
    @State private var selectedButtons : [ButtonHistoryItem] = []
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        ScrollView{
            VStack {
                TextField("Task Name", text: $taskName)
                    .textFieldStyle(.roundedBorder)
                    .padding()
                
                DirectionPadView { direction in
                    let item = ButtonHistoryItem(
                        type: .direction(direction),
                        value: direction,
                        timestamp: Date()
                    )
                    selectedButtons.append(item)
                    print("\(direction)")
                }
                
                ActionButtonsView { action in
                    let item = ButtonHistoryItem(
                        type: .action(action),
                        value: action,
                        timestamp: Date()
                    )
                    selectedButtons.append(item)
                    print("\(action)")
                }
                
                ActivatorButtonView { action, isActivated in
                    let status = isActivated ? "ON" : "OFF"
                    let item = ButtonHistoryItem(
                        type: .activator(action),
                        value: "\(action) - \(status)",
                        timestamp: Date()
                    )
                    selectedButtons.append(item)
                }
                
                ModeButtonsView(selectedIndex: $selectedMode)
                
                HStack {
                    Button("Add Action") {
                        print("Add Action tapped")
                    }
                    .padding()
                    .background(BackgroundGradient.backgroundGradient)
                    .foregroundStyle(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                    
                    Button("Add Delay") {
                        showSecondPicker = true
                        print("Add delay tapped")
                    }
                    .padding()
                    .background(BackgroundGradient.backgroundGradient)
                    .foregroundStyle(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                    
                }
                .padding()
                HistoryListView(items: $selectedButtons)
            }
            .sheet(isPresented: $showSecondPicker) {
                SecondPickerSheet { seconds in
                    guard let lastValue = selectedButtons.last else {
                        let item = ButtonHistoryItem(
                            type: .delay,
                            value: "\(seconds) seconds",
                            timestamp: Date()
                        )
                        selectedButtons.append(item)
                        return
                    }
                    if lastValue.value.contains("seconds"){
                        return
                    }
                    let item = ButtonHistoryItem(
                        type: .delay,
                        value: "\(seconds) seconds",
                        timestamp: Date()
                    )
                    selectedButtons.append(item)
                }
            }
            .onChange(of: selectedMode) { _, newValue in
                let item = ButtonHistoryItem(
                    type: .mode,
                    value: "Mode \(newValue + 1)",
                    timestamp: Date()
                )
                selectedButtons.append(item)
            }
            .navigationTitle("Actions & Delays")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        dismiss()
                    }
                }
            }
        }
    }
}
#Preview {
    ActionsAndDelaysView()
}
