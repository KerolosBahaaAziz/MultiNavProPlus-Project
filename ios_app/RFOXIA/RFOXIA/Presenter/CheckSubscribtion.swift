//
//  CheckSubscribtion.swift
//  RFOXIA
//
//  Created by Kerlos on 23/06/2025.
//

import Foundation


class CheckSubscribtionPresenter{
    static let shared = CheckSubscribtionPresenter()
    
    private init()    {}
    
    func checkSubscription() {
        DataBaseManager.shared.isUserSubscribe { isSubscribed in
            if isSubscribed {
                DataBaseManager.shared.getSubscriptionExpiryDate { expireDate in
                    if let expireDate = expireDate {
                        let isExpired = expireDate < Date()
                        if !isExpired {
                            UserDefaults.standard.set(expireDate.timeIntervalSince1970, forKey: "subscriptionExpireDate")
                            UserDefaults.standard.set(true, forKey: "isSubscribed")
                        } else {
                            UserDefaults.standard.set(false, forKey: "isSubscribed")
                            UserDefaults.standard.removeObject(forKey: "subscriptionExpireDate")
                        }
                    } else {
                        // No expiry date found → treat as not subscribed
                        UserDefaults.standard.set(false, forKey: "isSubscribed")
                        UserDefaults.standard.removeObject(forKey: "subscriptionExpireDate")
                    }
                }
            } else {
                // User is not subscribed at all
                UserDefaults.standard.set(false, forKey: "isSubscribed")
                UserDefaults.standard.removeObject(forKey: "subscriptionExpireDate")
            }
        }
    }
    
}
