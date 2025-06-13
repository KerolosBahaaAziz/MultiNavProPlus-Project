//
//  PayPalButton.swift
//  RFOXIA
//
//  Created by Kerlos on 13/06/2025.
//

import Foundation
import SwiftUI
import PayPalCheckout

struct PayPalButtonView: View {
    var amount: String
    var currency: String

    var body: some View {
        PayPalButtonContainer(amount: amount, currency: currency)
            .frame(height: 50)
    }
}

struct PayPalButtonContainer: UIViewRepresentable {
    let amount: String
    let currency: String

    func makeUIView(context: Context) -> UIView {
        let view = UIView(frame: .zero)

        Checkout.setCreateOrderCallback { createOrderAction in
            PayPalOrderService.shared.createOrder(amount: amount, currency: currency) { orderId in
                if let orderId = orderId {
                    createOrderAction.set(orderId: orderId)
                } else {
                    print("❌ Failed to create PayPal order")
                }
            }
        }

        Checkout.setOnApproveCallback { approval in
            approval.actions.capture { response, error in
                if let data = response?.data {
                    print("✅ Payment approved and captured: \(data)")
                } else if let error = error {
                    print("❌ Error capturing payment: \(error.localizedDescription)")
                }
            }
        }

        Checkout.setOnCancelCallback {
            print("⚠️ User canceled the PayPal payment.")
        }

        Checkout.setOnErrorCallback { errorInfo in
            print("❌ PayPal SDK error: \(errorInfo.error.localizedDescription)")
        }

        let button = PayPalButton()
        button.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(button)

        NSLayoutConstraint.activate([
            button.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            button.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            button.topAnchor.constraint(equalTo: view.topAnchor),
            button.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])

        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {}
}
