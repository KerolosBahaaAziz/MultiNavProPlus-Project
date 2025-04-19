//
//  AddActionButton.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 15/04/2025.
//

import SwiftUI

struct AddActionButton: View {
    var body: some View {
        Image(systemName: iconName)
            .resizable()
            .aspectRatio(contentMode: .fit)
            .frame(width: 30, height: 30)
            .foregroundColor(.white)
            .padding()
            .background(Color.black)
            .clipShape(Circle())
            .shadow(radius: 3)
    }
    
    private var iconName : String = "plus"
}

#Preview {
    AddActionButton()
}
