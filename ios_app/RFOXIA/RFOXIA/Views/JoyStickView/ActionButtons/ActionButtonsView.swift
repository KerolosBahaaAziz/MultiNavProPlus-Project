//
//  ActionButtonsView.swift
//  Testing JoyStick with SwiftUI
//
//  Created by Yasser Yasser on 11/04/2025.
//

import SwiftUI

struct ActionButtonsView: View {
    var onActionPressed: ((String) -> Void)?
    
    enum ShapeType {
        case circle
        case square
        case triangle
        case xshape
    }

    var body: some View {
        VStack(spacing: 10) {
            Button(action: {
                onActionPressed?("triangle")
            }) {
                ShapeButton(shape: .triangle)
            }

            HStack(spacing: 70) {
                Button(action: {
                    onActionPressed?("square")
                }) {
                    ShapeButton(shape: .square)
                }
                
                Button(action: {
                    onActionPressed?("circle")
                }) {
                    ShapeButton(shape: .circle)
                }

            }

            Button(action: {
                onActionPressed?("xmark")
            }) {
                ShapeButton(shape: .xshape)
            }
        }
    }
}

    
    #Preview {
        ActionButtonsView()
            .padding()
            .background(Color.black)
    }
