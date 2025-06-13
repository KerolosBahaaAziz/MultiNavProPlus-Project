//
//  MessagesHistory.swift
//  RFOXIA
//
//  Created by Kerlos on 17/05/2025.
//

import Foundation
import CoreData

extension CoreDataManager{
    // MARK: - Chat Handling
    func saveMessage(text: String, senderEmail: String, isMine: Bool, type: String, record: Data) {
           let context = container.viewContext
           let message = Messages(context: context)
           message.id = UUID()
           message.text = text
           message.senderId = senderEmail
           message.createdAt = Date()
           message.isCurrentUser = isMine
           message.type = type
        message.record = record

           do {
               try context.save()
           } catch {
               print("Error saving message: \(error)")
           }
       }

    /*func fetchMessages(for peerEmail: String) -> [ChatMessage] {
        let context = container.viewContext
        let request: NSFetchRequest<Messages> = Messages.fetchRequest()
        request.predicate = NSPredicate(format: "senderId == %@", peerEmail)
        request.sortDescriptors = [NSSortDescriptor(keyPath: \Messages.createdAt, ascending: true)]

        do {
            let results = try context.fetch(request)
            let chatMessages = results.map { message in
                ChatMessage(
                    id: message.id ?? UUID(),
                    type: message.type == "text" ? .text(message.text ?? "") : .voice(message.record), // or decode voice if you implement that
                    isCurrentUser: message.isCurrentUser,
                    senderName: "", // You might need to fetch or pass this separately
                    senderId: message.senderId ?? "",
                    createdAt: message.createdAt ?? Date(),
                    text: message.text ?? ""
                )
            }
            return chatMessages
        } catch {
            print("Error fetching messages: \(error)")
            return []
        }
    }*/

    func deleteAllMessages() {
        let context = container.viewContext
        let request: NSFetchRequest<NSFetchRequestResult> = Messages.fetchRequest()
        let deleteRequest = NSBatchDeleteRequest(fetchRequest: request)
        
        do {
            try context.execute(deleteRequest)
            try context.save()
        } catch {
            print("Error deleting messages: \(error)")
        }
    }
    
}
