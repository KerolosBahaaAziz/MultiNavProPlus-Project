//
//  SendCommands.swift
//  RFOXIA
//
//  Created by Kerlos on 23/04/2025.
//

import Foundation

//extension BluetoothManager {
//    func sendCommand(_ command: Character) {
//        sendCommandToMicrocontroller(command)
//    }
//}

class StorageService {
    private let defaults = UserDefaults.standard
    
    func save<T: Codable>(_ object: T, forKey key: String) {
        if let data = try? JSONEncoder().encode(object) {
            defaults.set(data, forKey: key)
        }
    }
    
    func load<T: Codable>(_ type: T.Type, forKey key: String) -> T? {
        guard let data = defaults.data(forKey: key),
              let object = try? JSONDecoder().decode(type, from: data) else { return nil }
        return object
    }
    
    func saveString(_ value: String, forKey key: String) {
        defaults.set(value, forKey: key)
    }

    func loadString(forKey key: String) -> String? {
        defaults.string(forKey: key)
    }

    func saveInt(_ value: Int, forKey key: String) {
        defaults.set(value, forKey: key)
    }

    func loadInt(forKey key: String) -> Int {
        defaults.integer(forKey: key)
    }
}

