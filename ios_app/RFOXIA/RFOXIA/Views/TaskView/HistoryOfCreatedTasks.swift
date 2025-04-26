//
//  HistoryOfTaskedCreated.swift
//  RFOXIA
//
//  Created by Kerlos on 26/04/2025.
//

/*import SwiftUI

struct HistoryOfCreatedTasks: View {
    
    // The selected task that we pass from TaskView
    var selectedTask: History?
    
    // Fetch all saved history items from Core Data, filtering by the task name
    @FetchRequest(entity: History.entity(),
                  sortDescriptors: [NSSortDescriptor(keyPath: \History.timestamp, ascending: false)],
                  predicate: NSPredicate(format: "taskName == %@", selectedTask?.taskName ?? ""))
    var taskHistory: FetchedResults<History>
    
    var body: some View {
        NavigationStack {
            ZStack {
                BackgroundGradient.backgroundGradient
                    .ignoresSafeArea()
                
                VStack {
                    Text("History of \(selectedTask?.taskName ?? "Selected Task")")
                        .font(.title)
                        .fontWeight(.bold)
                        .padding()
                        .foregroundColor(.white)
                    
                    // List to display task history
                    List(taskHistory) { task in
                        HStack {
                            Text(task.taskName ?? "Unnamed Task")
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .foregroundStyle(Color.white)
                            
                            Spacer()
                            
                            Text(task.timestamp ?? Date(), style: .time)
                                .foregroundStyle(Color.white)
                        }
                        .listRowBackground(Color.clear)
                    }
                    .listStyle(.plain)
                    .scrollContentBackground(.hidden)
                    .padding()
                }
            }
        }
    }
}


#Preview {
    HistoryOfCreatedTasks()
}*/
