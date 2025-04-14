//
//  ModeButtonsView.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 12/04/2025.
//

import SwiftUI

struct ModeButtons: View {
    @Binding var selection: Int
    var index: Int
    var label: String
    
    var body: some View {
        Button(action: {
            selection = index
        }) {
            HStack {
                Circle()
                    .strokeBorder(Color.blue, lineWidth: 2)
                    .frame(width: 30, height: 30)
                    .overlay(
                        Circle()
                            .fill(selection == index ? Color.blue : Color.clear)
                            .padding(4)
                    )
                
                Text(label)
                    .font(.body)
            }
            .padding()
        }
    }
}

#Preview {
    @Previewable @State var selectedIndex: Int = 0
    
    ModeButtons(selection: $selectedIndex, index: 0, label: "1")
}
