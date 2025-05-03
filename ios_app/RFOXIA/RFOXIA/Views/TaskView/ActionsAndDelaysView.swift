//
//  ActionsAndDelaysView.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 15/04/2025.
//

import SwiftUI

struct ActionsAndDelaysView: View {
    
    var loadedTask: History? = nil
    
    @State private var taskName: String = ""
    @State private var showSecondPicker: Bool = false
    @State private var selectedMode: Int = 0
    @State private var selectedButtons: [ButtonHistoryItem] = []
    @Environment(\.dismiss) private var dismiss
    @Environment(\.managedObjectContext) private var context
    
    var body: some View {
        ScrollView {
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
                }
                
                ActionButtonsView { action in
                    let item = ButtonHistoryItem(
                        type: .action(action),
                        value: action,
                        timestamp: Date()
                    )
                    selectedButtons.append(item)
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
                    /*Button("Add Action") {
                        // Optional: Add extra action handling
                    }
                    .padding()
                    .background(BackgroundGradient.backgroundGradient)
                    .foregroundStyle(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 10))*/
                    
                    Button("Add Delay") {
                        showSecondPicker = true
                    }
                    .padding()
                    .background(BackgroundGradient.backgroundGradient)
                    .foregroundStyle(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                    
                }
                .padding()
                
                HistoryListView(items: $selectedButtons)
                
                Button("Save Task") {
                    saveTask()
                }
                .padding()
                .background(BackgroundGradient.backgroundGradient)
                .foregroundColor(.white)
                .clipShape(RoundedRectangle(cornerRadius: 10))
                .padding()
            }
            .sheet(isPresented: $showSecondPicker) {
                SecondPickerSheet { seconds in
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
            .onAppear {
                loadTaskIfNeeded()
            }
        }
    }
    
    private func loadTaskIfNeeded() {
        guard let loadedTask = loadedTask else { return }
        
        taskName = loadedTask.taskName ?? ""
        
        if let data = loadedTask.items,
           let decodedItems = try? JSONDecoder().decode([ButtonHistoryItem].self, from: data) {
            selectedButtons = decodedItems
        }
    }
    
    private func saveTask() {
        let newTask = History(context: context)
        newTask.id = UUID()
        newTask.taskName = taskName
        
        if let encoded = try? JSONEncoder().encode(selectedButtons) {
            newTask.items = encoded
        }
        
        do {
            try context.save()
            dismiss()
        } catch {
            print("Failed to save task: \(error.localizedDescription)")
        }
    }
    
}


#Preview {
    ActionsAndDelaysView()
}
