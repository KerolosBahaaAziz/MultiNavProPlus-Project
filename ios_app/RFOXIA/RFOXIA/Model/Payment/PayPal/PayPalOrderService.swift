//
//  PayPalOrderService.swift
//  RFOXIA
//
//  Created by Kerlos on 13/06/2025.
//

import Foundation

class PayPalOrderService {
    
    static let shared = PayPalOrderService()
    
    // Replace with your Sandbox credentials
    private let clientId = "ARtK7n9fg11xIBa4OzHymtUQb037NbHVkmodgQj3E8JvgN8miupe8oI2EgJeixqTv2qGVlDElLJDinRX"
    private let secret = "EFA2s9By7h2eNCf-w3rnHjbPPOdU8ylHSyA7iGQwkUz4ZMI7iUvZWnMhToZ5z_GUalkaRJn4jFM_wejS"
    private let baseUrl = "https://api-m.sandbox.paypal.com" // Use "https://api-m.paypal.com" for production

    // MARK: - Public Method to Create Order
    func createOrder(amount: String, currency: String, completion: @escaping (String?) -> Void) {
        getAccessToken { accessToken in
            guard let token = accessToken else {
                completion(nil)
                return
            }

            self.createOrderRequest(amount: amount, currency: currency, accessToken: token, completion: completion)
        }
    }

    // MARK: - Step 1: Get PayPal Access Token
    private func getAccessToken(completion: @escaping (String?) -> Void) {
        guard let url = URL(string: "\(baseUrl)/v1/oauth2/token") else {
            completion(nil)
            return
        }

        let credentials = "\(clientId):\(secret)"
        guard let credentialsData = credentials.data(using: .utf8)?.base64EncodedString() else {
            completion(nil)
            return
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("Basic \(credentialsData)", forHTTPHeaderField: "Authorization")
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        request.httpBody = "grant_type=client_credentials".data(using: .utf8)

        URLSession.shared.dataTask(with: request) { data, _, error in
            guard let data = data, error == nil,
                  let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let accessToken = json["access_token"] as? String else {
                completion(nil)
                return
            }

            completion(accessToken)
        }.resume()
    }

    // MARK: - Step 2: Create PayPal Order
    private func createOrderRequest(amount: String, currency: String, accessToken: String, completion: @escaping (String?) -> Void) {
        guard let url = URL(string: "\(baseUrl)/v2/checkout/orders") else {
            completion(nil)
            return
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let body: [String: Any] = [
            "intent": "CAPTURE",
            "purchase_units": [
                [
                    "amount": [
                        "currency_code": currency,
                        "value": amount
                    ]
                ]
            ]
        ]

        request.httpBody = try? JSONSerialization.data(withJSONObject: body, options: [])

        URLSession.shared.dataTask(with: request) { data, _, error in
            guard let data = data, error == nil,
                  let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let orderId = json["id"] as? String else {
                completion(nil)
                return
            }

            completion(orderId)
        }.resume()
    }
}
