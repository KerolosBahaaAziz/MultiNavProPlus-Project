//
//  JoyStickView.swift
//  Testing JoyStick with SwiftUI
//
//  Created by Yasser Yasser on 11/04/2025.
//

import SwiftUI

struct DirectionPadView: View {
    
    var onDirectionPressed: ((Direction) -> Void)?
    
    enum Direction {
        case up
        case down
        case left
        case right
    }
    
    var body: some View {
        VStack(spacing : 10){
            Button(action: {
                onDirectionPressed?(.up)
            }){
                ArrowButton(direction: .up)
            }
            HStack(spacing: 70){
                
                Button(action: { onDirectionPressed?(.left) }){
                    ArrowButton(direction: .left)
                }
                
                Button(action: { onDirectionPressed?(.right) }){
                    ArrowButton(direction: .right)
                }
            }
            Button(action: { onDirectionPressed?(.down) }){
                ArrowButton(direction: .down)
            }
        }
    }
}

#Preview {
    DirectionPadView()
        .padding()
        .background(Color.black)
}
