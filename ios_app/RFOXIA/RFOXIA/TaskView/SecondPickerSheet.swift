//
//  SecondPickerSheet.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 15/04/2025.
//

import SwiftUI

struct SecondPickerSheet: View {
    
    var addSecondPressed: ((String) -> Void)?
    @Environment(\.dismiss) var dismiss
    
    @State private var text: String = ""
    @State private var second : Int = 1
    @State private var milliSecond : Int = 0
    @State private var showAlert : Bool = false
    @State private var alertMessage : String = ""
    
    let numbers = Array(0...60)
    let milliNumbers = Array(1...99)
    var body: some View {
        
        VStack{
            Text("Delay")
                .font(.title)
            TextField("Add Delay in Seconds", text: $text)
                .textFieldStyle(.roundedBorder)
                .font(.title)
                .keyboardType(.decimalPad)

            HStack{
                Picker("select A number",selection: $second){
                    ForEach(numbers,id: \.self){number in
                        Text("\(number)")
                            .tag(number)
                    }
                }.pickerStyle(.wheel)
                
                Picker("select A number",selection: $milliSecond){
                    ForEach(milliNumbers,id: \.self){number in
                        Text("\(number)")
                            .tag(number)
                    }
                }.pickerStyle(.wheel)
            }
            .onChange(of: [second , milliSecond]){ _ ,newValue in
                let seconds = newValue.first ?? 0
                let milliSeconds = newValue.last ?? 0
                text = "\(seconds).\(milliSeconds)"
            }

            Button("Add Delay"){
                checkValue()
            }
            .frame(maxWidth: .infinity)
            .frame(maxHeight: 50)
            .background(BackgroundGradient.backgroundGradient)
            .foregroundStyle(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 10))
            .padding()
            Spacer()
        }
        .alert(isPresented: $showAlert){
            Alert(title: Text("ERROR") ,message : Text(alertMessage))
        }
    }
    
    private func checkValue() -> Void {
        guard !text.isEmpty else {
            alertMessage = "Please enter a value in the text field"
            showAlert = true
            return
        }
        if isValidTwoDecimalNumber(text) ==  false{
            alertMessage = "value must be in range of 0.1 to 60.99"
            showAlert = true
            return
        }
        addSecondPressed?(text)
        dismiss()
    }
    
    private func isValidTwoDecimalNumber(_ input: String) -> Bool {
        let pattern = #"^(?:[1-5]?\d(?:\.\d{1,2})?|60(?:\.0{1,2})?)$|^0\.[1-9]\d?$|^0\.\d[1-9]$"#
        return input.range(of: pattern, options: .regularExpression) != nil
    }
}

#Preview {
    SecondPickerSheet()
}
