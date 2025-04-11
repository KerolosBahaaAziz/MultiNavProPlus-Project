//
//  ContentView.swift
//  Testing JoyStick with SwiftUI
//
//  Created by Yasser Yasser on 11/04/2025.
//

import SwiftUI

struct JoyStickView: View {
    var body: some View {
        VStack{
            SensorsReadingView()
            HStack {
                DirectionPadView(onDirectionPressed: { direction in
                    print("direction is \(direction)")
                })
                Spacer()
                ActionButtonsView(onActionPressed: { Action in
                    print("Action is \(Action)")
                })
            }
            .padding()
        }
    }
}

#Preview {
    JoyStickView()
}
