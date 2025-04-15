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
    @Environment(\.scenePhase) var scenePhase
    @State var tasks : [Task] = [Task(action: "action1"),Task(action: "action2"),Task(action: "action3"),Task(action: "action4"),Task(action: "action5"),Task(action: "action6"),Task(action: "action7"),Task(action: "action8"),Task(action: "action9"),Task(action: "action10")]
    
    var body: some View {
        NavigationStack{
            GeometryReader { geometry in
                VStack(spacing: 16) {
                    SensorsReadingView()
                    
                    ModeButtonsView(selectedIndex: $selectedMode)
                        .padding(.horizontal)
                        .frame(height: geometry.size.height * 0.05)
                    
                    Text("\(value)")
                        .font(.title)
                        .padding(.vertical, 10)
                    
                    HStack {
                        DirectionPadView { direction in
                            print("direction is \(direction)")
                        }
                        
                        rotatingKnobView(selection: $value, range: -500...500) { isMoving in
                            print("isMoving: \(isMoving)")
                        }
                        .aspectRatio(1, contentMode: .fit)
                        .frame(maxWidth: geometry.size.width * 0.3)
                        .padding(.horizontal)
                        
                        ActionButtonsView { action in
                            print("Action is \(action)")
                        }
                    }
                    .frame(maxHeight: geometry.size.height * 0.25)
                    
                    Spacer()
                    ActivatorButtonView { action, isActivated in
                        print("\(action) is \(isActivated)")
                    }
                    .padding(.bottom)
                }
                .padding()
                .frame(width: geometry.size.width, height: geometry.size.height)
                .overlay(
                    NavigationLink(destination: TaskView()) {
                        Image(systemName: "plusminus.circle.fill")
                            .font(.system(size: 40))
                            .foregroundStyle((BackgroundGradient.backgroundGradient))
                    }
                    , alignment: .bottomTrailing
                )
            }
        }
        .onAppear(){
            OrientationHelper.forceLandscapeOnLaunch = true
            OrientationHelper.forceLandscape()
        }
        .onChange(of: scenePhase) { _, newPhase in
            if newPhase == .active {
                OrientationHelper.forceLandscapeOnLaunch = true
                OrientationHelper.forceLandscape()
            }
        }
        .onDisappear(){
            OrientationHelper.forceLandscapeOnLaunch = false
            OrientationHelper.forcePortrait()
        }
        .onReceive(NotificationCenter.default.publisher(for: UIApplication.willEnterForegroundNotification)) { _ in
            OrientationHelper.forceLandscape()
        }
        .onChange(of: selectedMode){ oldValue , newValue in
            if oldValue != newValue{
                print("selectedMode changed to \(newValue + 1)")
            }
        }
        
    }
}

#Preview {
    JoyStickView()
}
