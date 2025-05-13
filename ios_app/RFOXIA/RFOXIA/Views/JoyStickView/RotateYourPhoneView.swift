//
//  RotateYourPhoneView.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 13/05/2025.
//

import SwiftUI

struct RotateYourPhoneView: View {
    var body: some View {
        VStack {
            Spacer()
            Text("Please rotate your phone")
                .font(.largeTitle)
                .bold()
                .multilineTextAlignment(.center)
                .padding()
            Text("This screen is better used in landscape mode.")
                .font(.title2)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding()
            
            Image(systemName: "iphone.landscape")
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: 150, height: 150) // or any size you like
                .foregroundColor(.gray)
                .padding()
            Spacer()
        }
    }
}

#Preview {
    RotateYourPhoneView()
}
