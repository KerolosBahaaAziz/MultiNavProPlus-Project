//
//  TaskView.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 14/04/2025.
//

import SwiftUI

struct TaskView: View {
    
    @State private var tasks: [Task] = Array(1...10).map { Task(action: "action\($0)") }
    @State private var isOn: [Bool]
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
                    NavigationLink(destination: ActionsAndDelaysView()){
                        AddActionButton()
                    }
                }
                .padding()
            }
        }
    }
    private func deleteTask(at offsets: IndexSet) {
        tasks.remove(atOffsets: offsets)
        isOn.remove(at: offsets.first!)
    }
}


#Preview {

    TaskView()
}
