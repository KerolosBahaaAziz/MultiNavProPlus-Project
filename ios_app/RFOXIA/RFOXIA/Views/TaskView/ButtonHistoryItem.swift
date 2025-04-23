//
//  ButtonHistoryItem.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 15/04/2025.
//

import Foundation
import SwiftUICore

struct ButtonHistoryItem: Identifiable {
    let id = UUID()
    let type: ButtonType
    let value: String
    let timestamp: Date
    
    enum ButtonType {
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
            case "up": return "arrow.up"
            case "down": return "arrow.down"
            case "left": return "arrow.left"
            case "right": return "arrow.right"
            default: return "questionmark"
            }
        case .action(let action):
            return action.replacingOccurrences(of: "action", with: "") + ".circle"
        case .activator:
            return "power"
        case .delay:
            return "clock"
        case .mode:
            return "gear"
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
