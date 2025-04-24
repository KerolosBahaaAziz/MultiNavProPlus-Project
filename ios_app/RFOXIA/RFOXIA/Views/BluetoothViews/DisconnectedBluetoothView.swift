//
//  DisconnectedBluetoothView.swift
//  RFOXIA
//
//  Created by Kerlos on 12/04/2025.
//

import SwiftUI

struct DisconnectedBluetoothView: View {
    var body: some View {
            Spacer()
            VStack(spacing: 20) {
                Image(systemName: "antenna.radiowaves.left.and.right.slash")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 100, height: 100)
                    .foregroundColor(.white.opacity(0.7))

                Text("Bluetooth Disconnected")
                    .font(.title2)
                    .fontWeight(.semibold)
                    .foregroundColor(.white.opacity(0.9))

                Text("Please enable Bluetooth to start chatting.")
                    .multilineTextAlignment(.center)
                    .foregroundColor(.white.opacity(0.7))
                    .padding(.horizontal)
            }
            Spacer()
        }
}

#Preview {
    DisconnectedBluetoothView()
}
