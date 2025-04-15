//
//  TaskView.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 14/04/2025.
//

import SwiftUI

struct TaskView: View {
    
    @Binding private var tasks: [Task]
    @State private var isOn: [Bool]
    var addActionPressed: ((Bool) -> Void)?
    
    // ✅ Updated init to accept Binding
    init(tasks: Binding<[Task]>) {
        self._tasks = tasks
        self._isOn = State(initialValue: Array(repeating: false, count: tasks.wrappedValue.count))
    }
    
    var body: some View {
        NavigationStack {
            ZStack {
                BackgroundGradient.backgroundGradient
                    .ignoresSafeArea()
                VStack {
                    List {
                        ForEach(tasks.indices, id: \.self) { index in
                            HStack {
                                NavigationLink(destination: EmptyView()){
                                    Text(tasks[index].action)
                                    Spacer()
                                    Toggle("", isOn: $isOn[index])
                                }
                            }
                        }
                        .onDelete(perform: deleteTask)
                    }
                    .listStyle(.plain)
                    
                    NavigationLink(destination: EmptyView()){
                        Button(action: {
                            addActionPressed?(true)
                        }) {
                            VStack {
                                AddActionButton()
                                Text("Add Action")
                                    .font(.subheadline)
                            }
                            .padding(.top)
                        }
                    }
                    
                }
            }
        }
    }
    private func deleteTask(at offsets: IndexSet) {
        tasks.remove(atOffsets: offsets)
        isOn.remove(at: offsets.first!)
    }
}


#Preview {
    @Previewable @State var tasks : [Task] = [Task(action: "action1"),Task(action: "action2"),Task(action: "action3"),Task(action: "action4"),Task(action: "action5"),Task(action: "action6"),Task(action: "action7"),Task(action: "action8"),Task(action: "action9"),Task(action: "action10")]
    TaskView(tasks: $tasks)
}
