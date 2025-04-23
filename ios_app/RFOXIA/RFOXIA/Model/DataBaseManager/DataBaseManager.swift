//
//  DataBaseManager.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 22/04/2025.
//

import Foundation
import FirebaseDatabase

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
        database.child(user.safeEmail).setValue([
            "firstName" : user.firstName,
            "lastName" : user.lastName,
            "paid" : user.paid
        ])
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
