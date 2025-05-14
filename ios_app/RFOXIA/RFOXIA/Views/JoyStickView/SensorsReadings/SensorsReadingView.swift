//
//  SensorsReadingView.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 11/04/2025.
//

import SwiftUI

struct SensorsReadingView: View {
    @Binding var temp : Float
    @Binding var humidity : Float
    @Binding var pressure : Float
    @State var status : String
    var body: some View {
        HStack{
            Text("\(String(format: "%.1f", (temp / 16383.0) * 165.0 - 40.0)) °C")
            Spacer()
            Text("\(String(format: "%.1f", (humidity / 16383.0) * 100.0)) %")
            Spacer()
            Text("\(String(format: "%.1f", pressure / 4098.0)) hPa")
            Spacer()
            Text("\(status)")
        }
        .font(.system(size: 33))
        .background(in: .rect, fillStyle: .init(eoFill: true))
    }
}
//
//#Preview {
//    SensorsReadingView(temp: 26.3, humidity: 48.5, pressure: 1003.1, status: "Good")
//}
