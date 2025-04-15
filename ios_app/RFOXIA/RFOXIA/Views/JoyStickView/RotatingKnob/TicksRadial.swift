//
//  TicksRadial.swift
//  Testing JoyStick with SwiftUI
//
//  Created by Yasser Yasser on 12/04/2025.
//

import SwiftUI

struct TicksRadial: Shape {
    var length: CGFloat
    var tickMax: Int
    var candidates: Set<Int>

    func path(in rect: CGRect) -> Path {
        var path = Path()
        let center = CGPoint(x: rect.midX, y: rect.midY)
        let radius = min(rect.width, rect.height) / 2

        for angle in candidates {
            let radians = Double(angle) * .pi / 180
            let cosAngle = CGFloat(cos(radians))
            let sinAngle = CGFloat(sin(radians))

            let outer = CGPoint(
                x: center.x + cosAngle * radius,
                y: center.y + sinAngle * radius
            )
            let inner = CGPoint(
                x: center.x + cosAngle * (radius - length),
                y: center.y + sinAngle * (radius - length)
            )

            path.move(to: inner)
            path.addLine(to: outer)
        }

        return path
    }
}
