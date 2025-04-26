//
//  rotatingKnob.swift
//  Testing JoyStick with SwiftUI
//
//  Created by Yasser Yasser on 12/04/2025.
//

import SwiftUI

struct rotatingKnobView : View {
    @State private var rotation: Angle = .degrees(0)
    @State private var cumulativeRotation: Angle = .degrees(0)
    @State private var lastDragValue : CGPoint?
    
    @Binding var selection : Int
    private var range : ClosedRange<Int>
    private var step : Double = 1.0
    private var onEditingChanged : ((Int) -> Void)?
    
    init(selection: Binding<Int>, range: ClosedRange<Int>, onEditingChanged: ((Int) -> Void)? = nil){
        self._selection = selection
        self.range = range
        self.onEditingChanged = onEditingChanged
        self._rotation = State(initialValue: .degrees(Double(selection.wrappedValue)))
        self._cumulativeRotation = State(initialValue: .degrees(Double(selection.wrappedValue)))
    }
    var body: some View {
        GeometryReader{ geometry in
            let width = geometry.size.width
            let height = geometry.size.height
            let size = min(geometry.size.width , geometry.size.height)
            ZStack{
                Circle()
                    .strokeBorder( Color.accentColor.tertiary , lineWidth: width/2)
                    .highPriorityGesture(DragGesture(minimumDistance: 0.3)
                        .onChanged{ value in
                            updateValue(with: value, in: CGSize(width: width, height: height))
                        }
                        .onEnded { _ in
                            lastDragValue = nil
                            onEditingChanged?(selection)
                            rotation = .degrees(0)
                            cumulativeRotation = .degrees(0)
                        }
                    )
                TicksRadial(length : 15 , tickMax : 100 , candidates : Set(stride(from: 0, to: 360, by: 10)))
                    .stroke(Color.accentColor , lineWidth :2)
                    .rotationEffect(rotation)
                
                Rectangle()
                    .fill(Color.red)
                    .frame(width: 2, height: size/2)
                    .offset(y : -size/4)
                    .rotationEffect(rotation)
            }
            .onChange(of: selection){ _ , newValue in
                let newAngle = Angle.degrees(Double(newValue))
                if rotation != newAngle {
                    rotation = newAngle
                    cumulativeRotation = newAngle
                }
            }
            .onChange(of: rotation) {_, newValue in
                DispatchQueue.main.async{
                    selection = Int(newValue.degrees.rounded())
                }
            }
        }
    }
    
    private func updateValue(with value : DragGesture.Value , in size : CGSize){
        if let lastDragValue = lastDragValue {
            let location = value.location
            let newAngle = angleForPoints(lastDragValue , location,in : size)
            let angleDelta  = Angle.degrees(newAngle)
            
            cumulativeRotation += angleDelta
            cumulativeRotation = campledSnatToStep(cumulativeRotation,step : step)
            
            withAnimation(.spring()){
                rotation = cumulativeRotation
            }
            //onEditingChanged?(true)
        }
        lastDragValue = value.location
    }
    
    private func angleForPoints(_ start : CGPoint , _ end : CGPoint , in size : CGSize) -> Double{
        let startVector = CGVector(dx: start.x - size.width / 2, dy: start.y - size.height / 2)
        let endVector = CGVector(dx: end.x - size.width / 2, dy: end.y - size.height / 2)
        
        let startAngle = atan2(startVector.dy, startVector.dx)
        let endAngle = atan2(endVector.dy, endVector.dx)
        
        var deltaAngle = (endAngle - startAngle) * 180 / .pi
        
        if deltaAngle > 180 {
            deltaAngle -= 360
        }else if deltaAngle < -180 {
            deltaAngle += 360
        }
        return deltaAngle
    }
    
    private func campledSnatToStep(_ angle : Angle , step : Double) -> Angle{
        let degree = angle.degrees
        let snapped = round(degree / step) * step
        let clamped = min(max(snapped, Double(range.lowerBound)), Double(range.upperBound))
        return .degrees(clamped)
    }
}

#Preview {
    @Previewable @State var value : Int = 0
    Text("Numeric Knob In swiftUI")
    Text("\(value)")
    rotatingKnobView(selection: $value, range: -500...500){ editing in
        print(editing)
    }
}
