//
//  UserDefaults.swift
//  RFOXIA
//
//  Created by Kerlos on 24/04/2025.
//

import Foundation

class UserDefaultsUsed{
    func userDefaults(){
        UserDefaults.standard.set(false, forKey: "isSubscribed")
        UserDefaults.standard.set(false, forKey: "isLogin")
    }
}
