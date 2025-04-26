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
        
        do {
            let encoder = JSONEncoder()
            let data = try encoder.encode(items)
            history.items = data
            try context.save()
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
