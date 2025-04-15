//
//  ArrowButton.swift
//  Testing JoyStick with SwiftUI
//
//  Created by Yasser Yasser on 11/04/2025.
//

import SwiftUI

struct ArrowButton: View {
    
    let direction: DirectionPadView.Direction
    
    var body: some View {
        Image(systemName: iconName)
            .resizable()
            .aspectRatio(contentMode: .fit)
            .frame(width: 30, height: 30)
            .foregroundColor(.white)
            .padding()
            .background(BackgroundGradient.backgroundGradient)
            .clipShape(Circle())
            .shadow(radius: 3)
    }
    
    private var iconName : String {
        switch(direction) {
        case .up: return "chevron.up"
        case .down: return "chevron.down"
        case .left: return "chevron.left"
        case .right: return "chevron.right"
        }
    }
}

#Preview {
    ArrowButton(direction: .up)
}
