//
//  DataBaseManager.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 22/04/2025.
//

import Foundation
import FirebaseDatabase
import FirebaseAuth

final class DataBaseManager {
    static let shared = DataBaseManager()
    
    private let database = Database.database(url: "https://rfoxia-20970-default-rtdb.firebaseio.com").reference()
    
}

extension DataBaseManager {
    func userExists(with email : String , completion : @escaping (Bool)-> Void){
        
        let safeEmail = email.replacingOccurrences(of: ".", with: "-")
        database.child(safeEmail).observeSingleEvent(of: .value, with: {snapShot in
            guard snapShot.exists() else {
                completion(false)
                return
            }
            completion(true)
        })
    }
    
    func addUser(user : User){
        print("user safe email : \(user.safeEmail)")
        database.child(user.safeEmail).setValue([
            "firstName" : user.firstName,
            "lastName" : user.lastName,
            "paid" : user.paid
        ])
    }
    
    func signOut(completion: @escaping (Bool, String) -> Void) {
        do {
            try Auth.auth().signOut()  // Firebase sign-out method
            UserDefaults.standard.set(false, forKey: "isLogin")
            print("✅ Successfully signed out.")
            completion(true, "Successfully signed out.")  // Success callback
        } catch let error {
            print("❌ Sign out failed: \(error.localizedDescription)")  // Print the error if sign-out fails
            completion(false, "Sign-out failed: \(error.localizedDescription)")  // Failure callback
        }
    }
    
    func isUserSubscribe(completion : @escaping (Bool)-> Void){
        guard let email = Auth.auth().currentUser?.email else {
            print("❌ No user signed in.")
            completion(false)
            return
        }
        let safeEmail = email.replacingOccurrences(of: ".", with: "-")
        
        database.child(safeEmail).observeSingleEvent(of: .value, with: { snapShot in
            guard snapShot.exists() else {
                print("❌ User not found.")
                completion(false)
                return
            }
            completion(true)
        })
    }
    
    func isPublicCouponExists(couponCode: String, completion: @escaping (Bool) -> Void) {
        database.child("PublicCoupons").child(couponCode).observeSingleEvent(of: .value) { snapshot in
            if snapshot.exists() {
                completion(true)
            } else {
                completion(false)
            }
        }
    }
    
    func getPublicCouponData(couponCode: String, completion: @escaping (Int) -> Void) {
        database.child("PublicCoupons").child(couponCode).child("discount").observeSingleEvent(of: .value) { snapshot in
            if let value = snapshot.value as? Int {
                completion(value)
            } else {
                completion(0)
            }
        }
    }
    
    func isPrivateCouponExists(couponCode: String, completion: @escaping (Bool) -> Void) {
        guard let email = Auth.auth().currentUser?.email else {
            print("❌ No user signed in.")
            completion(false)
            return
        }
        
        let safeEmail = email.replacingOccurrences(of: ".", with: "-")
        database.child(safeEmail).child("PrivateCoupons").child(couponCode).observeSingleEvent(of: .value) { snapshot in
            if snapshot.exists() {
                completion(true)
            } else {
                completion(false)
            }
        }
    }
    
    func getPrivateCouponData(couponCode: String, completion: @escaping (Int) -> Void) {
        guard let email = Auth.auth().currentUser?.email else {
            print("❌ No user signed in.")
            completion(0)
            return
        }
        
        let safeEmail = email.replacingOccurrences(of: ".", with: "-")
        database.child(safeEmail).child("PrivateCoupons").child(couponCode).child("discount").observeSingleEvent(of: .value) { snapshot in
            if let value = snapshot.value as? Int {
                completion(value)
            } else {
                completion(0)
            }
        }
    }
    
    func deletePrivateCoupon(couponCode: String) {
        guard let email = Auth.auth().currentUser?.email else {
            print("❌ No user signed in.")
            return
        }
        
        let safeEmail = email.replacingOccurrences(of: ".", with: "-")
        database.child(safeEmail).child("PrivateCoupons").child(couponCode).removeValue()
    }
    
    func savePaymentHistory(amount : Double, method : String) {
        guard let email = Auth.auth().currentUser?.email else {
            print("❌ No user signed in.")
            return
        }
        
        let safeEmail = email.replacingOccurrences(of: ".", with: "-")
        
        let currentDate = Date()
        let expiryDate = currentDate.addingTimeInterval(30 * 24 * 60 * 60) //30 days in seconds
        
        let paymentData: [String: Any] = [
            "amount": amount,
            "beginDate": currentDate.timeIntervalSince1970,
            "method": method,
            "expiryDate": expiryDate.timeIntervalSince1970
        ]
        
        database.child(safeEmail)
            .child("PaymentHistory")
            .setValue(paymentData) { error, _ in
                if let error = error {
                    print("❌ Failed to save payment: \(error.localizedDescription)")
                } else {
                    print("✅ Payment saved successfully.")
                }
            }
        database.child(safeEmail)
            .child("paid")
            .setValue(true){error , _ in
                if let error = error {
                    print("❌ Failed to change user to paid after payment: \(error.localizedDescription)")
                } else {
                    print("✅ saved user as paid successfully.")
                }
            }
    }
    
    func isSubscribtionExpired(completion : @escaping (Bool) -> Void){
        guard let email = Auth.auth().currentUser?.email else {
            print("❌ No user signed in.")
            return
        }
        
        let safeEmail = email.replacingOccurrences(of: ".", with: "-")
        
        database.child(safeEmail)
            .child("PaymentHistory")
            .child("expiryDate")
            .observeSingleEvent(of: .value){ snapShot in
                guard let timeStamp = snapShot.value as? TimeInterval else {
                    print("❌ No expiry date found.")
                    completion(true)
                    return
                }
                let expieryDate = Date(timeIntervalSince1970: timeStamp)
                let isExpired = expieryDate < Date()
                completion(isExpired)
            }
    }
}


struct User {
    let firstName : String
    let lastName : String
    let email : String
    let paid : Bool
    var safeEmail : String {
        self.email.replacingOccurrences(of: ".", with: "-")
    }
}
