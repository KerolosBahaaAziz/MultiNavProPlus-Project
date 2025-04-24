//
//  CustomAlert.swift
//  RFOXIA
//
//  Created by Kerlos on 24/04/2025.
//

import Foundation
import SwiftUICore
import SwiftUI

struct AlertInfo: Identifiable {
    let id = UUID()
    let title: String
    let message: String
    let confirmText: String
    let cancelText: String
    let confirmAction: () -> Void
}


extension View{
    func alert(info: Binding<AlertInfo?>) -> some View {
            self.alert(item: info) { alert in
                Alert(
                    title: Text(alert.title),
                    message: Text(alert.message),
                    primaryButton: .default(Text(alert.confirmText), action: alert.confirmAction),
                    secondaryButton: .cancel(Text(alert.cancelText))
                )
            }
        }
}
