//
//  SwiftUIView.swift
//  RFOXIA
//
//  Created by Kerlos on 02/05/2025.
//

import SwiftUI

struct HistoryOfCreatedTask: View {
    let taskName: String
    let items: [ButtonHistoryItem]
      
      var body: some View {
          ScrollView {
              Text(taskName)
                  .font(.largeTitle)
                  .padding()
              
              HistoryListView(items: .constant(items)) // ✅ read-only binding
          }
          .navigationTitle("Task History")
          .navigationBarTitleDisplayMode(.inline)
      }
}

#Preview {
    HistoryOfCreatedTask(taskName: "", items: [])
}
