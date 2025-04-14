//
//  ShapeButton.swift
//  Testing JoyStick with SwiftUI
//
//  Created by Yasser Yasser on 11/04/2025.
//

import SwiftUI

struct ShapeButton: View {
    
    let shape : ActionButtonsView.ShapeType
    
    var body: some View {
        Image(systemName :iconName)
            .resizable()
            .aspectRatio(contentMode: .fit)
            .frame(width: 30, height: 30)
            .foregroundColor(.white)
            .padding()
            .background(BackgroundGradient.backgroundGradient)
            .clipShape(Circle())
    }
    
    
    private var iconName : String {
        switch(shape) {
        case .circle : return "circle"
        case .square : return "square"
        case .triangle : return "triangle"
        case .xshape : return "xmark"
        }
    }
}

#Preview {
    ShapeButton(shape: .circle)
}
