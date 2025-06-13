//
//  PayPalView.swift
//  RFOXIA
//
//  Created by Kerlos on 13/06/2025.
//

import Foundation
import SwiftUI

struct PayPalView: View {
    var body: some View {
        VStack {
            Text("Pay with PayPal")
                .font(.title)

            PayPalButtonView(amount: "15.00", currency: "USD")
        }
        .padding()
    }
}
