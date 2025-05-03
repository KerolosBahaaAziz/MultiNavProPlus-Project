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
        case rotating(Int)
    }
    
    var iconName: String {
        switch type {
        case .direction(let direction):
            switch direction {
            case "up": return "arrow.up"
            case "down": return "arrow.down"
            case "left": return "arrow.left"
            case "right": return "arrow.right"
            default: return ""
            }
        case .action(let action):
            return action.replacingOccurrences(of: "action", with: "") + ".circle"
        case .activator:
            return "power"
        case .delay:
            return "clock"
        case .mode:
            return ""
        case .rotating:
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
        case .rotating: return .mint
        }
    }
}

extension ButtonHistoryItem {
    var commandLetter: String {
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
            switch action {
            case "circle": return "C"
            case "square": return "S"
            case "triangle": return "T"
            case "xshape": return "X"
            default: return ""
            }
        case .activator (let active):
            let isOn = value.contains("ON")
            let state = isOn ? "ON" : "OFF"
            switch active {
            case "a.circle": return "A\(state)"
            case "b.circle": return "B\(state)"
            case "c.circle": return "C\(state)"
            case "d.circle": return "D\(state)"
            default: return ""
            }
        case .delay:
            //if let seconds = Int(value.components(separatedBy: " ").first ?? "0") {
            print("delay")
            return value.replacingOccurrences(of: "seconds", with: "")
                //return "W\(seconds)"
            //}
            return ""
        case .mode:
            if let modeNumber = value.components(separatedBy: " ").last {
               return "M\(modeNumber)"
            }
            return ""
        case .rotating:
            return value
        }
    }
}
