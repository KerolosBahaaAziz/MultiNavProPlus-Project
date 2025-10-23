//
//  SubscriptionHandler.swift
//  RFOXIA
//
//  Created by Kerlos on 26/04/2025.
//

import Foundation
import FirebaseDatabase

class SubscriptionHandler{
    static let shared = SubscriptionHandler()
    
    func checkSubscriptionStatus(safeEmail: String, completion: @escaping (Bool) -> Void) {
        let ref = Database.database().reference()
        let safeEmailKey = safeEmail.replacingOccurrences(of: ".", with: "-")
        
        print("Checking subscription for safeEmail: \(safeEmailKey)")
        
        ref.child(safeEmailKey).child("paid").observeSingleEvent(of: .value) { snapshot in
            if let isSubscribed = snapshot.value as? Bool {
                completion(isSubscribed)
            } else {
                print("❌ No subscription info found")
                completion(false)
            }
        } withCancel: { error in
            print("❌ Error fetching subscription status: \(error.localizedDescription)")
            completion(false)
        }
    }

    func updateSubscriptionStatus(isSubscribed: Bool, email: String) {
        let ref = Database.database().reference()
        let safeEmail = email.replacingOccurrences(of: ".", with: "-")

        ref.child(safeEmail).child("paid").setValue(isSubscribed) { error, _ in
            if let error = error {
                print("❌ Failed to update subscription status: \(error.localizedDescription)")
            } else {
                print("✅ Subscription status updated.")
                UserDefaults.standard.set(isSubscribed, forKey: "isSubscribed")
            }
        }
    }

}

