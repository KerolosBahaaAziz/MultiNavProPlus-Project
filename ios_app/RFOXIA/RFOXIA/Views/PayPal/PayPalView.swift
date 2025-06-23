//
//  PayPalView.swift
//  RFOXIA
//
//  Created by Kerlos on 13/06/2025.
//

import Foundation
import SwiftUI


struct PayPalView: View {
    var amount : String
    var currency : String
    var body: some View {
        VStack {
            PayPalButtonView(amount: amount, currency: currency, onPaymentSuccess: {
                
            })
        }
    }
}

#Preview {
    PayPalView(amount: "100", currency: "USD")
}
