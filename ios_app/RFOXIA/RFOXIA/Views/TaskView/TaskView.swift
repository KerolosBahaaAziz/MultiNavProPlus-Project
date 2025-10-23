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
    
    var addActionPressed: ((Bool) -> Void)?
    
    init() {
        self._isOn = State(initialValue: Array(repeating: false, count: 10))
    }
    
    var body: some View {
        NavigationStack {
            ZStack {
                BackgroundGradient.backgroundGradient
                    .ignoresSafeArea()
                
                VStack {
                    List {
                        // Saved Tasks from CoreData
                        ForEach(savedTasks) { task in
                            NavigationLink(destination: ActionsAndDelaysView(loadedTask: task)) {
                                Text(task.taskName ?? "Unnamed Task")
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .foregroundStyle(Color.white)
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
    
}



#Preview {
    
    TaskView()
}
