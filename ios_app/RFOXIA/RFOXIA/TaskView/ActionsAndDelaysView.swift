//
//  ActionsAndDelaysView.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 15/04/2025.
//

import SwiftUI

struct ActionsAndDelaysView: View {
    
    @State private var taskName: String = ""
    @State private var showSecondPicker : Bool = false
    @State private var selectedDate = Date()
    @State private var selectedMode : Int = 0
    @State private var selectedButtons : [String] = []
    
    var body: some View {
        ScrollView{
            VStack {
                TextField("Task Name", text: $taskName)
                    .textFieldStyle(.roundedBorder)
                    .padding()
                
                DirectionPadView { direction in
                    selectedButtons.append("\(direction)")
                    print("Selected direction: \(direction)")
                }
                
                ActionButtonsView { action in
                    selectedButtons.append("\(action)")
                    print("Selected action: \(action)")
                }
                
                ActivatorButtonView { action, isActivated in
                    selectedButtons.append("\(action)")
                    print("\(action) is \(isActivated ? "activated" : "deactivated")")
                }
                
                ModeButtonsView(selectedIndex: $selectedMode)
                
                HStack {
                    Button("Add Action") {
                        print("Add Action tapped")
                    }
                    .padding()
                    .background(BackgroundGradient.backgroundGradient)
                    .foregroundStyle(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                    
                    Button("Add Delay") {
                        showSecondPicker = true
                        print("Add delay tapped")
                    }
                    .padding()
                    .background(BackgroundGradient.backgroundGradient)
                    .foregroundStyle(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                }
                .padding()
                Text("Button & Delay History:")
                    .font(.headline)
                    .padding(.top)
                
                ForEach(selectedButtons.indices, id: \.self) { index in
                    HStack{
                        if selectedButtons[index].contains(" seconds")
                        {
                            Text(selectedButtons[index])
                                .padding()
                        }else if selectedButtons[index].contains("mode")
                        {
                            Text(selectedButtons[index])
                                .padding()
                        }
                        else{
                            Image(systemName: selectedButtons[index])
                                .padding()
                        }
                        Spacer()
                        Button(action: {
                            selectedButtons.remove(at: index)
                        }) {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundColor(.red)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding()
                }
            }
        }
        .sheet(isPresented: $showSecondPicker){
            SecondPickerSheet(){ seconds in
                guard let selectedDelay = selectedButtons.last else {
                    selectedButtons.append("\(seconds) seconds")
                    return
                }
                if selectedDelay.contains(" seconds")
                {
                    return
                }else{
                    selectedButtons.append("\(seconds) seconds")
                }
            }
        }
        .onChange(of: selectedMode) { _ , newValue in                selectedButtons.append("mode \(newValue + 1)")
        }
    }
}
#Preview {
    ActionsAndDelaysView()
}
