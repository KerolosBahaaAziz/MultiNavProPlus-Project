//
//  TaskView.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 14/04/2025.
//

import SwiftUI

struct TaskView: View {
    
    @State private var isOn: [Bool]
    @FetchRequest(entity: History.entity(), sortDescriptors: []) var savedTasks: FetchedResults<History>
    
    init() {
        self._isOn = State(initialValue: Array(repeating: false, count: 100))
    }
    
    var body: some View {
        NavigationStack {
            ZStack {
                BackgroundGradient.backgroundGradient
                    .ignoresSafeArea()
                
                VStack {
                    List {
                        ForEach(Array(savedTasks.enumerated()), id: \.1.id) { index, task in
                            
                            VStack(alignment: .leading) {
                                NavigationLink {
                                    decodedTaskView(task: task)
                                } label: {
                                    Text(task.taskName ?? "Unnamed Task")
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                        .foregroundStyle(Color.white)
                                }
                                
                                Toggle("Activate", isOn: bindingForIndex(index, task: task))
                                    .toggleStyle(SwitchToggleStyle(tint: .green))
                                    .padding(.top, 5)
                            }
                            .listRowBackground(Color.clear)
                        }
                    }
                    .listStyle(.plain)
                    .scrollContentBackground(.hidden)
                    
                    NavigationLink {
                        ActionsAndDelaysView()
                    } label: {
                        AddActionButton()
                    }
                }
                .padding()
            }
        }
    }
    
    // Extract the decoded view logic
    private func decodedTaskView(task: History) -> some View {
        if let itemsData = task.items,
           let decodedItems = try? JSONDecoder().decode([ButtonHistoryItem].self, from: itemsData) {
            return AnyView(HistoryOfCreatedTask(taskName: task.taskName ?? "Unnamed Task", items: decodedItems))
        } else {
            return AnyView(Text("No history available"))
        }
    }
    
    // Separate the binding logic for toggle
    private func bindingForIndex(_ index: Int, task: History) -> Binding<Bool> {
        Binding(
            get: {
                isOn[index] ?? false
            },
            set: { newValue in
                if isOn.indices.contains(index) {
                    isOn[index] = newValue
                    if newValue {
                        printTaskLetters(task: task)
                    }
                }
            }
        )
    }
    
    private func printTaskLetters(task: History) {
        guard let itemsData = task.items,
              let decodedItems = try? JSONDecoder().decode([ButtonHistoryItem].self, from: itemsData) else {
            print("No valid items for task")
            return
        }
        
        let lettersArray = decodedItems.map { $0.commandLetter }
        let lettersString = lettersArray.joined(separator: " ")
        
        print("Task Letters: \(lettersString)")
    }
}


#Preview {
    TaskView()
}
