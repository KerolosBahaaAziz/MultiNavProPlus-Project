//
//  ModeButtonsView.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 12/04/2025.
//

import SwiftUI

struct ModeButtonsView: View {
    @Binding var selectedIndex: Int
    var body: some View {
        HStack{
            ModeButtons(selection: $selectedIndex, index: 0, label:"1")
            ModeButtons(selection: $selectedIndex, index: 1, label: "2")
            ModeButtons(selection: $selectedIndex, index: 2, label: "3")
        }
    }
}

#Preview {
    @Previewable @State var selectedIndex: Int = 2
    ModeButtonsView(selectedIndex: $selectedIndex)
}
