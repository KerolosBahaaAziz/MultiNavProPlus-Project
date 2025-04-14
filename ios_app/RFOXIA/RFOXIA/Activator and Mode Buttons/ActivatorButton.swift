//
//  ActivatorButton.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 12/04/2025.
//

import SwiftUI

struct ActivatorButton: View {
    let shape : ActivatorButtonView.ActivatorType
    let isActivated : Bool
    
    var body: some View {
        Image(systemName :isActivated ? iconNameActivated : iconNameUnActivated)
            .resizable()
            .aspectRatio(contentMode: .fit)
            .frame(width: 30, height: 30)
            .foregroundColor(.white)
            .padding(5)
            .background(BackgroundGradient.backgroundGradient)
            .clipShape(Circle())
    }
    
    
    private var iconNameUnActivated : String {
        switch(shape) {
        case .aButton : return "a.circle"
        case .bButton : return "b.circle"
        case .cButton : return "c.circle"
        case .dButton : return "d.circle"
        }
    }
    
    private var iconNameActivated : String {
        switch(shape) {
        case .aButton : return "a.circle.fill"
        case .bButton : return "b.circle.fill"
        case .cButton : return "c.circle.fill"
        case .dButton : return "d.circle.fill"
        }
    }
}

#Preview {
    ActivatorButton(shape: .aButton, isActivated: true)
}
