//
//  TaskView.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 14/04/2025.
//

import SwiftUI
import CoreData

struct TaskView: View {
    
    @FetchRequest(entity: History.entity(), sortDescriptors: []) var savedTasks: FetchedResults<History>
    @Environment(\.managedObjectContext) private var viewContext
    
    var body: some View {
        NavigationStack {
            ZStack {
                BackgroundGradient.backgroundGradient
                    .ignoresSafeArea()
                
                VStack {
                    List {
                        ForEach(Array(savedTasks.enumerated()), id: \.1.id) { index, task in
                            HStack {
                                // Task name with navigation
                                NavigationLink(destination: decodedTaskView(task: task)) {
                                    Text(task.taskName ?? "Unnamed Task")
                                        .foregroundStyle(.white)
                                }

                                Spacer()

                                // Send button
                                Button(action: {
                                    printTaskLetters(task: task)
                                }) {
                                    Image(systemName: "paperplane.fill")
                                        .foregroundColor(.blue)
                                        .padding(.horizontal, 8)
                                }
                                .buttonStyle(BorderlessButtonStyle()) // prevent NavigationLink interference

                                // Delete button
                                Button(action: {
                                    deleteTask(task: task)
                                }) {
                                    Image(systemName: "trash.fill")
                                        .foregroundColor(.red)
                                        .padding(.horizontal, 8)
                                }
                                .buttonStyle(BorderlessButtonStyle()) // prevent NavigationLink interference
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
    
    private func deleteTask(task: History) {
        viewContext.delete(task)
        do {
            try viewContext.save()
        } catch {
            print("Error deleting task: \(error)")
        }
    }
}

#Preview {
    TaskView()
}
