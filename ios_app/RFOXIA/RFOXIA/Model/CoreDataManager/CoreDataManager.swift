//
//  CoreDataManager.swift
//  RFOXIA
//
//  Created by Kerlos on 26/04/2025.
//

import Foundation
import CoreData

struct CoreDataManager {
    static let shared = CoreDataManager()
    let container: NSPersistentContainer

    init() {
        container = NSPersistentContainer(name: "ShiftDataModel")
        container.loadPersistentStores { description, error in
            if let error = error {
                print("Core Data failed to load: \(error.localizedDescription)")
            }
        }
    }

    func saveHistory(taskName: String, items: [ButtonHistoryItem]) {
        let context = container.viewContext
        let history = History(context: context)
        history.id = UUID()
        history.taskName = taskName
        
        // Create string like "U D A W2 L P"
        //let commandString = items.map { $0.commandLetter }.joined(separator: " ")
        //history.letters = commandString  // Make sure "letters" exists in CoreData model as String
        
        do {
            try context.save()
           // print("Saved letters: \(commandString)")
        } catch {
            print("Failed to save history: \(error.localizedDescription)")
        }
    }

    func fetchHistories() -> [History] {
        let context = container.viewContext
        let request: NSFetchRequest<History> = History.fetchRequest()

        do {
            return try context.fetch(request)
        } catch {
            print("Failed to fetch histories: \(error.localizedDescription)")
            return []
        }
    }
}
