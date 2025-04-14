//
//  TaskView.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 14/04/2025.
//

import SwiftUI

struct TaskView: View {
    
    private var tasks : [Task] = []
    @State private var isOn : [Bool] = []
    
    init(tasks: [Task]) {
        self.tasks = tasks
    }
    
    var body: some View {
        List{
            ForEach(tasks, id: \.self) { task in
                HStack{
                    Text(task.action)
                    Spacer()
                }
            }
        }
    }
}

#Preview {
    TaskView(tasks: [Task(action: "action1"),Task(action: "action2"),Task(action: "action3")])
}
