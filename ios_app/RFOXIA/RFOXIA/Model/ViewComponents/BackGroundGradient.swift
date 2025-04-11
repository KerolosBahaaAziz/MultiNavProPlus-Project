//
//  BackGroundGradient.swift
//  RFOXIA
//
//  Created by Kerlos on 11/04/2025.
//

import Foundation
import SwiftUICore

struct BackgroundGradient {
    static let backgroundGradient = LinearGradient(
        gradient: Gradient(colors: [
            Color(red: 35/255, green: 57/255, blue: 146/255),
            Color(red: 160/255, green: 48/255, blue: 199/255),
            Color(red: 255/255, green: 0/255, blue: 144/255)
        ]),
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )
}
