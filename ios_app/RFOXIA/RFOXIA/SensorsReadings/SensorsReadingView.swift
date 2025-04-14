//
//  SensorsReadingView.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 11/04/2025.
//

import SwiftUI

struct SensorsReadingView: View {
    var body: some View {

        HStack{
            Text("26°C")
            Spacer()
            Text("48%")
            Spacer()
            Text("1013hPa")
            Spacer()
            Text("Good")
        }
        .font(.system(size: 33))
        .background(in: .rect, fillStyle: .init(eoFill: true))
    }
}

#Preview {
    SensorsReadingView()
}
