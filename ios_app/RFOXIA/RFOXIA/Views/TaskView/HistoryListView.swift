//
//  SwiftUIView.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 15/04/2025.
//

import SwiftUI

struct HistoryListView: View {
    @Binding var items: [ButtonHistoryItem]
    
    var body: some View {
        VStack(alignment: .leading) {
            Text("History")
                .font(.headline)
                .padding(.horizontal)
            
            ForEach(items) { item in
                HStack {
                    Image(systemName: item.iconName)
                        .foregroundColor(item.color)
                        .frame(width: 30)
                    
                    VStack(alignment: .leading) {
                        Text(item.value.capitalized)
                        Text(item.timestamp, style: .time)
                            .font(.caption)
                            .foregroundColor(.gray)
                    }
                    
                    Spacer()
                    
                    Button {
                        if let index = items.firstIndex(where: { $0.id == item.id }) {
                            items.remove(at: index)
                        }
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(.red)
                    }
                }
                .padding()
                .background(Color.gray.opacity(0.1))
                .cornerRadius(10)
                .padding(.horizontal)
            }
        }
    }
}

#Preview {
    @Previewable @State var items: [ButtonHistoryItem] = []
    HistoryListView(items: $items)
}
