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
    func saveMessages(forEmail email: String, text: String?, isMine: Bool, type: String?, record: Data?, senderName: String) {
        let context = container.viewContext
        let message = Messages(context: context)
        message.id = UUID()
        message.text = text
        message.email = email
        message.createdAt = Date()
        message.isCurrentUser = isMine
        message.type = type
        message.record = record
        message.senderName = senderName
        
        do {
            try context.save()
        } catch {
            print("Error saving message: \(error)")
        }
    }
    
    func fetchMessages(for peerEmail: String) -> [ChatMessage] {
        let context = container.viewContext
        let request: NSFetchRequest<Messages> = Messages.fetchRequest()
        request.predicate = NSPredicate(format: "email == %@", peerEmail)
        request.sortDescriptors = [NSSortDescriptor(keyPath: \Messages.createdAt, ascending: true)]
        
        do {
            let results = try context.fetch(request)
            let chatMessages = results.compactMap { message -> ChatMessage? in
                let messageType: MessageType
                if message.type == "text" {
                    messageType = .text(message.text ?? "")
                } else if let audioData = message.record {
                    // Write Data to temporary file and get URL
                    let tempURL = writeAudioDataToTempFile(audioData: audioData)
                    messageType = .voice(tempURL)
                } else {
                    return nil // Invalid message type or missing audio data
                }
                
                return ChatMessage(
                    type: messageType,
                    isCurrentUser: message.isCurrentUser,
                    senderName: message.senderName ?? "",
                    senderId: message.email ?? "",
                    createdAt: message.createdAt ?? Date(),
                    text: message.text ?? ""
                )
            }
            
            return chatMessages
        } catch {
            print("Error fetching messages: \(error)")
            return []
        }
    }
    
    func writeAudioDataToTempFile(audioData: Data) -> URL {
        let tempDirectory = FileManager.default.temporaryDirectory
        let fileName = UUID().uuidString + ".m4a" // or appropriate file extension
        let fileURL = tempDirectory.appendingPathComponent(fileName)
        
        do {
            try audioData.write(to: fileURL)
        } catch {
            print("❌ Failed to write audio data to temp file: \(error)")
        }
        
        return fileURL
    }
    
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
