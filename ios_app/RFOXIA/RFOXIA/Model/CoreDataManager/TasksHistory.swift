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
                fatalError("Core Data failed to load: \(error.localizedDescription)")
            }
        }
    }

    // MARK: - History Handling
    func saveHistory(taskName: String, items: [ButtonHistoryItem]) {
        let context = container.viewContext
        let history = History(context: context)
        history.id = UUID()
        history.taskName = taskName
        history.letters = items.map { $0.commandLetter }.joined(separator: " ")
        
        do {
            try context.save()
            print("Saved letters: \(history.letters ?? "")")
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
