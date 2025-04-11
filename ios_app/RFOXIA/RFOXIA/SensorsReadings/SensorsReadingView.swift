//
//  SensorsReadingView.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 11/04/2025.
//

import SwiftUI

struct SensorsReadingView: View {
    var body: some View {
        VStack(alignment: .leading){
            Text("Temp: 26°C")
            Text("Humidity: 48%")
            Text("Pressure: 1013hPa")
            Text("Air quality: Good")
        }
        .font(.title3)
        .background(in: .rect, fillStyle: .init(eoFill: true))
    }
}

#Preview {
    SensorsReadingView()
}
