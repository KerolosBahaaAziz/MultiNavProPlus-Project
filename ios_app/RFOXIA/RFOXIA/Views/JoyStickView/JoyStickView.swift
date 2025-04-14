//
//  ContentView.swift
//  Testing JoyStick with SwiftUI
//
//  Created by Yasser Yasser on 11/04/2025.
//

import SwiftUI

struct JoyStickView: View {
    @State var value: Int = 0
    @State var selectedMode : Int = 0
    
    var body: some View {
        GeometryReader { geometry in
            VStack(spacing: 16) {
                
                SensorsReadingView()
                
                ModeButtonsView(selectedIndex: $selectedMode)
                    .padding(.horizontal)
                    .frame(height: geometry.size.height * 0.05)
                
                Text("\(value)")
                    .font(.title)
//                    .padding(.vertical, 5)
                
                HStack {
                    DirectionPadView { direction in
                        print("direction is \(direction)")
                    }
                    
                    rotatingKnobView(selection: $value, range: -500...500) { isMoving in
                        print("isMoving: \(isMoving)")
                    }
                    .aspectRatio(1, contentMode: .fit)
                    .frame(maxWidth: geometry.size.width * 0.38)
                    .padding(.horizontal)
                    
                    ActionButtonsView { action in
                        print("Action is \(action)")
                    }
                }
                .frame(maxHeight: geometry.size.height * 0.38)
                
                Spacer()
                
                ActivatorButtonView { action, isActivated in
                    print("\(action) is \(isActivated)")
                }
                .padding(.bottom)
            }
            .padding()
            .frame(width: geometry.size.width, height: geometry.size.height)
        }
    }
}

#Preview {
    JoyStickView()
}
