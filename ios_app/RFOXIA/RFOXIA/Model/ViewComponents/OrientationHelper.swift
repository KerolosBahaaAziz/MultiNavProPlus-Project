//
//  OrientationManager.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 14/04/2025.
//

import UIKit
import SwiftUI

final class OrientationHelper {
    static func forceLandscape() {
        guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene else { return }

        scene.requestGeometryUpdate(.iOS(interfaceOrientations: .landscapeLeft)) { _ in
//            if let error = error {
//                print("Orientation update failed: \(error.localizedDescription)")
//            }
        }
    }

    static func forcePortrait() {
        guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene else { return }

        scene.requestGeometryUpdate(.iOS(interfaceOrientations: .portrait)) { _ in
//            if let error = error {
//                print("Orientation update failed: \(error.localizedDescription)")
//            }
        }
    }
}
