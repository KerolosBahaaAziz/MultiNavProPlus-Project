//
//  ChoosePaymentMethodView.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 21/06/2025.
//

import SwiftUI

struct ChoosePaymentMethodView: View {
    @State private var subscriptionPrice: Double = 100.0
    @State private var discount: Double = 0.0
    @State private var isPublicCoupon: Bool = true
    @State private var couponCode : String = ""
    @State private var couponValidation : String = "INVALID COUPON"
    @State private var couponValidationColor : Color = .red
    
    var subtotal : Double {
        subscriptionPrice
    }
    
    var total : Double {
        max(0, subscriptionPrice * (1 - discount / 100.0))
    }
    
    var body: some View {
        ZStack{
            BackgroundGradient
                .backgroundGradient
                .ignoresSafeArea()
            VStack(alignment: .leading,spacing: 20){
                Text("Subscription: $\(subscriptionPrice, specifier: "%.2f")")
                    .foregroundStyle(.white)
                Text("Subtotal: $\(subtotal, specifier: "%.2f")")
                    .foregroundStyle(.white)
                Text("Discount: \(discount, specifier: "%.0f")%")
                    .foregroundStyle(.white)
                Text("Total: $\(total, specifier: "%.2f")")
                    .foregroundStyle(.white)
                    .fontWeight(.bold)
                HStack{
                    TextField("Enter Coupon Code", text: $couponCode)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                    Text(couponValidation)
                        .foregroundStyle(couponValidationColor)
                        .font(.subheadline)
                }
                
                Picker("Coupon Type", selection: $isPublicCoupon) {
                    Text("Public Coupon").tag(true)
                    Text("Private Coupon").tag(false)
                }
                .pickerStyle(SegmentedPickerStyle())
                
                ApplePayView(amount: total,currencyCode: "USD",privateCouponCode: couponCode, onPaymentSuccess: {
                    DataBaseManager.shared.savePaymentHistory(amount: total, method: "ApplePay")
                    resetView()
                    updateUserDefaults()
                })
                PayPalButtonView(amount: String(total),currency: "USD",privateCouponCode: couponCode, onPaymentSuccess: {
                    DataBaseManager.shared.savePaymentHistory(amount: total, method: "PayPal")
                    resetView()
                    updateUserDefaults()
                })
            }
            .padding()
        }
        .navigationTitle("Choose Payment Method")
        .onChange(of: couponCode, {
            applyCoupon()
        })
        .onChange(of: isPublicCoupon, {
            applyCoupon()
        })
        .onAppear(){
            resetView()
        }
    }
    func applyCoupon() {
        guard !couponCode.isEmpty else {
            print("Please enter a coupon code.")
            return
        }
        
        if isPublicCoupon {
            DataBaseManager.shared.isPublicCouponExists(couponCode: couponCode) { exists in
                if exists {
                    DataBaseManager.shared.getPublicCouponData(couponCode: couponCode) { discountAmount in
                        DispatchQueue.main.async {
                            discount = Double(discountAmount)
                            print("✅ Coupon applied!")
                            couponValidation = "VALID COUPON"
                            couponValidationColor = .green
                        }
                    }
                } else {
                    DispatchQueue.main.async {
                        print("❌ Invalid public coupon.")
                        couponValidation = "INVALID COUPON"
                        couponValidationColor = .red
                        discount = 0
                    }
                }
            }
        } else {
            DataBaseManager.shared.isPrivateCouponExists(couponCode: couponCode) { exists in
                if exists {
                    DataBaseManager.shared.getPrivateCouponData(couponCode: couponCode) { discountAmount in
                        DispatchQueue.main.async {
                            discount = Double(discountAmount)
                            print("✅ Private coupon applied!")
                            couponValidation = "VALID COUPON"
                            couponValidationColor = .green
                        }
                    }
                } else {
                    DispatchQueue.main.async {
                        print("❌ Invalid private coupon.")
                        couponValidation = "INVALID COUPON"
                        couponValidationColor = .red
                        discount = 0
                    }
                }
            }
        }
    }
    
    func updateUserDefaults(){
        let today = Date ()
        UserDefaults.standard.set(true, forKey: "isSubscribed")
        UserDefaults.standard.set(today.timeIntervalSince1970, forKey: "subscribtionExpireDate")
    }
    
    func resetView(){
        subscriptionPrice = 100.0
        discount = 0.0
        isPublicCoupon = true
        couponCode = ""
        couponValidation = "INVALID COUPON"
        couponValidationColor = .red
        
    }
}

#Preview {
    ChoosePaymentMethodView()
}
