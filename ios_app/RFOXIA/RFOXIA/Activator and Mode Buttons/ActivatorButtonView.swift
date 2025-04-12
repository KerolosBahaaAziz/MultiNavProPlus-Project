//
//  ActivatorButtonView.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 12/04/2025.
//

import SwiftUI

struct ActivatorButtonView: View {
    var onActionPressed: ((ActivatorType , Bool) -> Void)?
    @State var isActivated: [ActivatorType: Bool] = [
        .aButton: false,
        .bButton: false,
        .cButton: false,
        .dButton: false
    ]
    enum ActivatorType {
        case aButton
        case bButton
        case cButton
        case dButton
    }
    
    var body: some View {
        HStack(spacing: 10) {
            Button(action: {
                toggleButtonState(for: .aButton)
            }) {
                ActivatorButton(shape: .aButton,
                                isActivated: isActivated[.aButton] ?? false)
            }
            Button(action: {
                toggleButtonState(for: .bButton)
            }) {
                ActivatorButton(shape: .bButton,
                                isActivated: isActivated[.bButton] ?? false)
            }
            
            Button(action: {
                toggleButtonState(for: .cButton)
            }) {
                ActivatorButton(shape: .cButton,
                                isActivated: isActivated[.cButton] ?? false)
            }
            
            Button(action: {
                toggleButtonState(for: .dButton)
            }) {
                ActivatorButton(shape: .dButton,
                                isActivated: isActivated[.dButton] ?? false)
            }
        }
    }
    private func toggleButtonState(for button : ActivatorType){
        isActivated[button]?.toggle()
        onActionPressed?(button , isActivated[button] ?? false)
    }
}
                   

#Preview {
    ActivatorButtonView()
}
