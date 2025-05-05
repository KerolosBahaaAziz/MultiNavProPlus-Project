//
//  SensorsReadingView.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 11/04/2025.
//

import SwiftUI

struct SensorsReadingView: View {
    @State var temp : String
    @State var humidity : String
    @State var pressure : String
    @State var status : String
    var body: some View {
        HStack{
            Text("\(temp)°C")
            Spacer()
            Text("\(humidity)%")
            Spacer()
            Text("\(pressure)hPa")
            Spacer()
            Text("\(status)")
        }
        .font(.system(size: 33))
        .background(in: .rect, fillStyle: .init(eoFill: true))
    }
}

#Preview {
    SensorsReadingView(temp: "26", humidity: "48", pressure: "1013", status: "Good")
}
