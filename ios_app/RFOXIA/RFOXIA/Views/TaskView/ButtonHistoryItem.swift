//
//  ButtonHistoryItem.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 15/04/2025.
//

import Foundation
import SwiftUICore

struct ButtonHistoryItem: Identifiable, Codable{
    let id = UUID()
    let type: ButtonType
    let value: String
    let timestamp: Date
    
    enum ButtonType: Codable {
        case direction(String)
        case action(String)
        case activator(String)
        case delay
        case mode
    }
    
    var iconName: String {
        switch type {
        case .direction(let direction):
            switch direction {
            case "up": return "U"
            case "down": return "D"
            case "left": return "L"
            case "right": return "R"
            default: return ""
            }
        case .action(let action):
            return action.replacingOccurrences(of: "action", with: "") + ".circle"
        case .activator:
            return "P"
        case .delay:
            return "clock"
        case .mode:
            return ""
        }
    }
    
    var color: Color {
        switch type {
        case .direction: return .blue
        case .action: return .green
        case .activator: return .orange
        case .delay: return .purple
        case .mode: return .gray
        }
    }
}
