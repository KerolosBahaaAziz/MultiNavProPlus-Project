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
    var privateCouponCode: String = ""
    let onPaymentSuccess: () -> Void // Added callback
    
    
    var body: some View {
        PayPalButtonContainer(amount: amount, currency: currency , privateCouponCode: privateCouponCode, onPaymentSuccess: onPaymentSuccess)
            .frame(height: 50)
    }
}

struct PayPalButtonContainer: UIViewRepresentable {
    let amount: String
    let currency: String
    var privateCouponCode: String = ""
    let onPaymentSuccess: () -> Void // Added callback

    
    func makeUIView(context: Context) -> UIView {
        let view = UIView(frame: .zero)
        
        let button = UIButton(type: .system)
        button.setTitle("Subscribe with PayPal", for: .normal)
        button.setTitleColor(.white, for: .normal)
        button.titleLabel?.font = UIFont.boldSystemFont(ofSize: 16)
        button.backgroundColor = UIColor(red: 0.0, green: 0.47, blue: 0.73, alpha: 1.0)
        button.layer.cornerRadius = 8
        button.translatesAutoresizingMaskIntoConstraints = false
        
        button.addTarget(context.coordinator, action: #selector(Coordinator.payWithPayPal), for: .touchUpInside)
        
        view.addSubview(button)
        
        NSLayoutConstraint.activate([
            button.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            button.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            button.topAnchor.constraint(equalTo: view.topAnchor),
            button.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
        
        return view
    }
    func makeCoordinator() -> Coordinator {
        Coordinator(amount: amount, currency: currency, privateCouponCode: privateCouponCode, onpaymentSuccess: onPaymentSuccess)
    }
    
    class Coordinator {
        let amount: String
        let currency: String
        let privateCouponCode: String
        let onPaymentSuccess: () -> Void

        init(amount: String, currency: String,privateCouponCode: String ,onpaymentSuccess: @escaping () -> Void) {
            self.amount = amount
            self.currency = currency
            self.privateCouponCode = privateCouponCode
            self.onPaymentSuccess = onpaymentSuccess
        }
        
        @objc func payWithPayPal() {
            Checkout.setCreateOrderCallback { createOrderAction in
                PayPalOrderService.shared.createOrder(amount: self.amount, currency: self.currency) { orderId in
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
                        DataBaseManager.shared.deletePrivateCoupon(couponCode: self.privateCouponCode)
                        DispatchQueue.main.async {
                            self.onPaymentSuccess()
                        }
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
            
            Checkout.start()
        }
    }
    func updateUIView(_ uiView: UIView, context: Context) {}
}

#Preview {
    PayPalButtonView(amount: "100.00", currency: "USD", onPaymentSuccess: {
        
    })
}
