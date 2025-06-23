//
//  ApplePayView.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 22/04/2025.
//

import SwiftUI

struct ApplePayView: View {
    var applePayHandler = ApplePayHandler()
    var amount : Double = 100
    var currencyCode : String = "USD"
    var privateCouponCode : String = ""
    let onPaymentSuccess: () -> Void // Added callback

    var body: some View {
        ApplePayButton {
            applePayHandler.startApplePay(amount: amount, currencyCode: currencyCode){ success in
                if success {
                    print("Payment Successful ✅")
                    DataBaseManager.shared.deletePrivateCoupon(couponCode: privateCouponCode)
                    DispatchQueue.main.async {
                        self.onPaymentSuccess()
                    }
                } else {
                    print("Payment Failed ❌")
                }
            }
        }

    }
}

#Preview {
    ApplePayView(onPaymentSuccess: {
        
    })
}
