//
//  ActivatorButtonView.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 12/04/2025.
//

import SwiftUI

struct ActivatorButtonView: View {
    var onActionPressed: ((String , Bool) -> Void)?
    @State var isActivated: [String: Bool] = [
        "a.circle": false,
        "b.circle": false,
        "c.circle": false,
        "d.circle": false
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
                toggleButtonState(for: "a.circle")
            }) {
                ActivatorButton(shape: .aButton,
                                isActivated: isActivated["a.circle"] ?? false)
            }
            Button(action: {
                toggleButtonState(for: "b.circle")
            }) {
                ActivatorButton(shape: .bButton,
                                isActivated: isActivated["b.circle"] ?? false)
            }
            
            Button(action: {
                toggleButtonState(for: "c.circle")
            }) {
                ActivatorButton(shape: .cButton,
                                isActivated: isActivated["c.circle"] ?? false)
            }
            
            Button(action: {
                toggleButtonState(for: "d.circle")
            }) {
                ActivatorButton(shape: .dButton,
                                isActivated: isActivated["d.circle"] ?? false)
            }
        }
    }
     func toggleButtonState(for button : String){
        isActivated[button]?.toggle()
        onActionPressed?(button , isActivated[button] ?? false)
    }
}
                   

#Preview {
    ActivatorButtonView()
}
