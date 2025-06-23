//
//  SaveMessgaePresenter.swift
//  RFOXIA
//
//  Created by Kerlos on 23/06/2025.
//

import Foundation

class SaveMessgaePresenter{
    static let shared = SaveMessgaePresenter()
    
    private init()    {}
    
    func saveMessageCoreData(forEmail email: String, text: String?, isMine: Bool, type: String?, record: Data?){
        CoreDataManager.shared.saveMessages(forEmail: email, text: text, isMine: isMine, type: type, record: record)
    }
}
