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
        VStack{
            SensorsReadingView()
            ModeButtonsView(selectedIndex: $selectedMode)
                .frame(height: 10)
            Text("\(value)")
                .padding(10)
            HStack {
                DirectionPadView(onDirectionPressed: { direction in
                    print("direction is \(direction)")
                })
                rotatingKnobView(selection: $value, range: -500...500){ isMoving in
                    print("\(isMoving)")
                }
                .frame(width: 150, height: 150)
                .padding([.leading ,.trailing] , 90)
                
                ActionButtonsView(onActionPressed: { action in
                    print("Action is \(action)")
                })
            }
            .frame(height: 140)
            .padding(.bottom, 50)
            ActivatorButtonView(){ Action , isActivated in
                print("\(Action) is \(isActivated)")
            }
        }
    }
}

#Preview {
    JoyStickView()
}
